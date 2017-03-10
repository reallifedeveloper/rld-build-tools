package com.reallifedeveloper.tools.test.database.dbunit;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.reallifedeveloper.tools.test.TestUtil;
import com.reallifedeveloper.tools.test.database.dbunit.DbUnitTestEntity.TestEnum;
import com.reallifedeveloper.tools.test.database.inmemory.InMemoryJpaRepository;

public class DbUnitFlatXmlReaderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void readFileForSimpleEntity() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        xmlReader.read("/dbunit/testentity.xml", repository, TestEntity.class, Long.class);

        Assert.assertEquals("Wrong number of entities in repository: ", 3, repository.count());

        TestEntity expected = new TestEntity(42L, "foo");
        TestEntity actual = repository.findOne(42L);
        verifyEntity(expected, actual);

        expected = new TestEntity(4711L, "bar");
        actual = repository.findOne(4711L);
        verifyEntity(expected, actual);

        expected = new TestEntity(9999L, "");
        actual = repository.findOne(9999L);
        verifyEntity(expected, actual);
    }

    @Test
    public void readFileForEntityWithAssociations() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> testEntityRepository = new InMemoryJpaRepository<>();
        InMemoryJpaRepository<DbUnitTestEntity, Integer> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        xmlReader.read("/dbunit/testentity.xml", testEntityRepository, TestEntity.class, Long.class);
        xmlReader.read("/dbunit/dbunittestentity.xml", repository, DbUnitTestEntity.class, Integer.class);

        Assert.assertEquals("Wrong number of entities in repository: ", 2, repository.count());

        TestEntity testEntity42 = new TestEntity(42L, "foo");
        TestEntity testEntity4711 = new TestEntity(4711L, "bar");
        DbUnitTestEntity expected = new DbUnitTestEntity((byte) 1, (short) 2, 3, 4L, 5.0f, 6.0, false, 'a', "foo",
                TestUtil.parseDate("2014-01-01"), TestEnum.FOO, new BigDecimal("1234.56"),
                new BigInteger("9999999999"), new TestEntity(42L, "foo"),
                Arrays.asList(new TestEntity[] { testEntity42, testEntity4711 }));
        DbUnitTestEntity actual = repository.findOne(3);
        verifyEntity(expected, actual);

        expected = new DbUnitTestEntity((byte) 10, (short) 11, 12, 13L, 14.0f, 15.0, true, 'b', "bar",
                TestUtil.parseDate("2015-01-01"), TestEnum.BAR, new BigDecimal("-1000.01"),
                new BigInteger("8888888888"), new TestEntity(4711L, "bar"), Collections.emptySet());
        actual = repository.findOne(12);
        verifyEntity(expected, actual);
    }

    public void readFileForEntityWithMissingAssociations() throws Exception {
        InMemoryJpaRepository<DbUnitTestEntity, Integer> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        expectedException.expect(IllegalArgumentException.class);
        expectedException
                .expectMessage("Entity of class " + TestEntity.class.getName() + " with primary key 42 not found");
        xmlReader.read("/dbunit/dbunittestentity.xml", repository, DbUnitTestEntity.class, Integer.class);

    }

    @Test
    public void readWrongTypeOfFile() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        xmlReader.read("/dbunit/dbunittestentity.xml", repository, TestEntity.class, Long.class);
        Assert.assertEquals("Wrong number of entities in repository: ", 0, repository.count());

    }

    @Test
    public void readIncorrectFile() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
        expectedException.expect(IllegalArgumentException.class);
        xmlReader.read("/dbunit/broken.xml", repository, TestEntity.class, Long.class);
    }

    @Test
    public void readNonExistingFile() throws Exception {
        DbUnitFlatXmlReader xmlProcessor = new DbUnitFlatXmlReader();
        expectedException.expect(FileNotFoundException.class);
        expectedException.expectMessage("/dbunit/nosuchfile.xml");
        xmlProcessor.read("/dbunit/nosuchfile.xml", null, TestEntity.class, Long.class);
    }

    private void verifyEntity(TestEntity expected, TestEntity actual) {
        Assert.assertNotNull("Entity should not be null", actual);
        Assert.assertEquals("Wrong id field: ", expected.id(), actual.id());
        Assert.assertEquals("Wrong name field: ", expected.name(), actual.name());
    }

    private void verifyEntity(DbUnitTestEntity expected, DbUnitTestEntity actual) {
        if (expected == null) {
            Assert.assertNull("Entity should be null");
        } else {
            Assert.assertNotNull("Entity should not be null", actual);
            Assert.assertEquals("Wrong byte field: ", expected.b().byteValue(), actual.b().byteValue());
            Assert.assertEquals("Wrong short field: ", expected.s().shortValue(), actual.s().shortValue());
            Assert.assertEquals("Wrong integer field: ", expected.id().intValue(), actual.id().intValue());
            Assert.assertEquals("Wrong long field: ", expected.l().longValue(), actual.l().longValue());
            Assert.assertEquals("Wrong float field: ", expected.f().floatValue(), actual.f().floatValue(), 0.0000001);
            Assert.assertEquals("Wrong double field: ", expected.d().doubleValue(), actual.d().doubleValue(),
                    0.0000001);
            Assert.assertEquals("Wrong boolean field: ", expected.bool().booleanValue(),
                    actual.bool().booleanValue());
            Assert.assertEquals("Wrong char field: ", expected.c().charValue(), actual.c().charValue());
            Assert.assertEquals("Wrong String field: ", expected.string(), actual.string());
            Assert.assertEquals("Wrong date field: ", expected.date(), actual.date());
            Assert.assertEquals("Wrong enum field: ", expected.testEnum(), actual.testEnum());
            if (expected.testEntity() == null) {
                Assert.assertNull("Test entity should be null", actual.testEntity());
            } else {
                verifyTestEntity(expected.testEntity(), actual.testEntity());
            }
            Assert.assertEquals("Wrong number of test entities: ", expected.testEntities().size(),
                    actual.testEntities().size());
            for (int i = 0; i < expected.testEntities().size(); i++) {
                verifyTestEntity(expected.testEntities().get(i), actual.testEntities().get(i));
            }
        }
    }

    private void verifyTestEntity(TestEntity expected, TestEntity actual) {
        Assert.assertNotNull("Test entity should not be null", actual);
        Assert.assertEquals("Wrong testEntity.id field: ", expected.id(), actual.id());
        Assert.assertEquals("Wrong testEntity.name field: ", expected.name(), actual.name());
    }
}
