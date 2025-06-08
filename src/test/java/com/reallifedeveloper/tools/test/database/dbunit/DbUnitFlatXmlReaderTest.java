package com.reallifedeveloper.tools.test.database.dbunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.reallifedeveloper.tools.test.TestUtil;
import com.reallifedeveloper.tools.test.database.dbunit.DbUnitTestEntity.TestEnum;
import com.reallifedeveloper.tools.test.database.inmemory.InMemoryJpaRepository;

public class DbUnitFlatXmlReaderTest {

    private static final double DELTA = 0.0000001;

    @Test
    public void readFileForSimpleEntity() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        xmlReader.read("/dbunit/testentity.xml", repository, TestEntity.class, Long.class);

        assertEquals(3, repository.count(), "Wrong number of entities in repository: ");

        TestEntity expected = new TestEntity(42L, "foo");
        TestEntity actual = repository.findById(42L).get();
        verifyEntity(expected, actual);

        expected = new TestEntity(4711L, "bar");
        actual = repository.findById(4711L).get();
        verifyEntity(expected, actual);

        expected = new TestEntity(9999L, "");
        actual = repository.findById(9999L).get();
        verifyEntity(expected, actual);
    }

    @Test
    public void readFileForEntityWithAssociations() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> testEntityRepository = new InMemoryJpaRepository<>();
        InMemoryJpaRepository<DbUnitTestEntity, Integer> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        xmlReader.read("/dbunit/testentity.xml", testEntityRepository, TestEntity.class, Long.class);
        xmlReader.read("/dbunit/dbunittestentity.xml", repository, DbUnitTestEntity.class, Integer.class);

        assertEquals(2, repository.count(), "Wrong number of entities in repository: ");

        TestEntity testEntity42 = new TestEntity(42L, "foo");
        TestEntity testEntity4711 = new TestEntity(4711L, "bar");
        DbUnitTestEntity expected = new DbUnitTestEntity((byte) 1, (short) 2, 3, 4L, 5.0f, 6.0, false, 'a', "foo",
                TestUtil.parseDate("2014-01-01"), TestEnum.FOO, new BigDecimal("1234.56"), new BigInteger("9999999999"),
                new TestEntity(42L, "foo"), Arrays.asList(new TestEntity[] { testEntity42, testEntity4711 }));
        DbUnitTestEntity actual = repository.findById(3).get();
        verifyEntity(expected, actual);

        expected = new DbUnitTestEntity((byte) 10, (short) 11, 12, 13L, 14.0f, 15.0, true, 'b', "bar", TestUtil.parseDate("2015-01-01"),
                TestEnum.BAR, new BigDecimal("-1000.01"), new BigInteger("8888888888"), new TestEntity(4711L, "bar"),
                Collections.emptySet());
        actual = repository.findById(12).get();
        verifyEntity(expected, actual);
    }

    @Test
    public void readFileForEntityWithMissingAssociations() throws Exception {
        InMemoryJpaRepository<DbUnitTestEntity, Integer> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> xmlReader.read("/dbunit/dbunittestentity.xml", repository, DbUnitTestEntity.class, Integer.class));
        assertEquals("Entity of class " + TestEntity.class.getName() + " with primary key 42 not found", e.getMessage());

    }

    @Test
    public void readWrongTypeOfFile() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        xmlReader.read("/dbunit/dbunittestentity.xml", repository, TestEntity.class, Long.class);
        assertEquals(0, repository.count(), "Wrong number of entities in repository: ");

    }

    @Test
    public void readFileWithIncorrectAttribute() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        assertThrows(IllegalArgumentException.class,
                () -> xmlReader.read("/dbunit/broken_wrong_attribute.xml", repository, TestEntity.class, Long.class));
    }

    @Test
    @Disabled
    public void readFileWithoutAttributes() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        assertThrows(IllegalArgumentException.class,
                () -> xmlReader.read("/dbunit/broken_no_attributes.xml", repository, TestEntity.class, Long.class));
    }

    @Test
    public void readNonExistingFile() throws Exception {
        DbUnitFlatXmlReader xmlProcessor = new DbUnitFlatXmlReader();
        Exception e = assertThrows(FileNotFoundException.class,
                () -> xmlProcessor.read("/dbunit/nosuchfile.xml", null, TestEntity.class, Long.class));
        assertEquals("/dbunit/nosuchfile.xml", e.getMessage());
    }

    private void verifyEntity(TestEntity expected, TestEntity actual) {
        assertNotNull(actual, "Entity should not be null");
        assertEquals(expected.id(), actual.id(), "Wrong id field: ");
        assertEquals(expected.name(), actual.name(), "Wrong name field: ");
    }

    private void verifyEntity(DbUnitTestEntity expected, DbUnitTestEntity actual) {
        if (expected == null) {
            assertNull("Entity should be null");
        } else {
            assertNotNull(actual, "Entity should not be null");
            assertEquals(expected.b().byteValue(), actual.b().byteValue(), "Wrong byte field: ");
            assertEquals(expected.s().shortValue(), actual.s().shortValue(), "Wrong short field: ");
            assertEquals(expected.id().intValue(), actual.id().intValue(), "Wrong integer field: ");
            assertEquals(expected.l().longValue(), actual.l().longValue(), "Wrong long field: ");
            assertEquals(expected.f().floatValue(), actual.f().floatValue(), DELTA, "Wrong float field: ");
            assertEquals(expected.d().doubleValue(), actual.d().doubleValue(), DELTA, "Wrong double field: ");
            assertEquals(expected.bool().booleanValue(), actual.bool().booleanValue(), "Wrong boolean field: ");
            assertEquals(expected.c().charValue(), actual.c().charValue(), "Wrong char field: ");
            assertEquals(expected.string(), actual.string(), "Wrong String field: ");
            assertEquals(expected.date(), actual.date(), "Wrong date field: ");
            assertEquals(expected.testEnum(), actual.testEnum(), "Wrong enum field: ");
            if (expected.testEntity() == null) {
                assertNull(actual.testEntity(), "Test entity should be null");
            } else {
                verifyTestEntity(expected.testEntity(), actual.testEntity());
            }
            assertEquals(expected.testEntities().size(), actual.testEntities().size(), "Wrong number of test entities: ");
            for (int i = 0; i < expected.testEntities().size(); i++) {
                verifyTestEntity(expected.testEntities().get(i), actual.testEntities().get(i));
            }
        }
    }

    private void verifyTestEntity(TestEntity expected, TestEntity actual) {
        assertNotNull(actual, "Test entity should not be null");
        assertEquals(expected.id(), actual.id(), "Wrong testEntity.id field: ");
        assertEquals(expected.name(), actual.name(), "Wrong testEntity.name field: ");
    }
}
