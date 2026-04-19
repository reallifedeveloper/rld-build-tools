package com.reallifedeveloper.tools.test.database.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.reallifedeveloper.tools.test.TestUtil;
import com.reallifedeveloper.tools.test.database.dbunit.DbUnitFlatXmlReaderTest;
import com.reallifedeveloper.tools.test.database.dbunit.DbUnitTestEntity;
import com.reallifedeveloper.tools.test.database.dbunit.DbUnitTestEntity.TestEnum;
import com.reallifedeveloper.tools.test.database.dbunit.TestEntity;
import com.reallifedeveloper.tools.test.database.inmemory.InMemoryJpaRepository;

public class CsvDatabaseReaderTest {

    private CsvDatabaseReader csvReader = new CsvDatabaseReader(';', 0);

    @Test
    public void readFileForSimpleEntity() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        csvReader.read("/csv/testentity.csv", repository, TestEntity.class, Long.class, "TEST_ENTITY");

        assertEquals(3, repository.count(), "Wrong number of entities in repository: ");

        TestEntity expected = new TestEntity(42L, "foo");
        TestEntity actual = repository.findById(42L).get();
        DbUnitFlatXmlReaderTest.verifyEntity(expected, actual);

        expected = new TestEntity(4711L, "bar");
        actual = repository.findById(4711L).get();
        DbUnitFlatXmlReaderTest.verifyEntity(expected, actual);

        expected = new TestEntity(9999L, "");
        actual = repository.findById(9999L).get();
        DbUnitFlatXmlReaderTest.verifyEntity(expected, actual);
    }

    @Test
    public void readFileForEntityWithAssociations() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> testEntityRepository = new InMemoryJpaRepository<>();
        InMemoryJpaRepository<DbUnitTestEntity, Integer> repository = new InMemoryJpaRepository<>();
        csvReader.read("/csv/testentity.csv", testEntityRepository, TestEntity.class, Long.class, "TEST_ENTITY");
        csvReader.read("/csv/dbunittestentity.csv", repository, DbUnitTestEntity.class, Integer.class, "DBUNITTESTENTITY");
        csvReader.read("/csv/dbunittestentity_testentity.csv", repository, DbUnitTestEntity.class, Integer.class,
                "DBUNITTESTENTITY_TESTENTITY");

        assertEquals(3, testEntityRepository.count(), "Wrong number of entities in testEntityRepository: ");
        assertEquals(2, repository.count(), "Wrong number of entities in repository: ");

        TestEntity testEntity42 = new TestEntity(42L, "foo");
        TestEntity testEntity4711 = new TestEntity(4711L, "bar");
        DbUnitTestEntity expected = new DbUnitTestEntity((byte) 1, (short) 2, 3, 4L, 5.0f, 6.0, false, 'a', "foo",
                TestUtil.parseDate("2014-01-01"), TestEnum.FOO, new BigDecimal("1234.56"), new BigInteger("9999999999"),
                new TestEntity(42L, "foo"), Arrays.asList(new TestEntity[] { testEntity42, testEntity4711 }));
        DbUnitTestEntity actual = repository.findById(3).get();
        DbUnitFlatXmlReaderTest.verifyEntity(expected, actual);

        expected = new DbUnitTestEntity((byte) 10, (short) 11, 12, 13L, 14.0f, 15.0, true, 'b', "bar", TestUtil.parseDate("2015-01-01"),
                TestEnum.BAR, new BigDecimal("-1000.01"), new BigInteger("8888888888"), new TestEntity(4711L, "bar"),
                Collections.emptySet());
        actual = repository.findById(12).get();
        DbUnitFlatXmlReaderTest.verifyEntity(expected, actual);
    }

    @Test
    public void readFileForEntityWithMissingAssociations() throws Exception {
        InMemoryJpaRepository<DbUnitTestEntity, Integer> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> csvReader.read("/csv/dbunittestentity.csv", repository, DbUnitTestEntity.class, Integer.class,
                        "DBUNITTESTENTITY"));
        assertEquals("Entity of class " + TestEntity.class.getName() + " with primary key 42 not found", e.getMessage());

    }

    @Test
    public void readWrongTypeOfFile() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> csvReader.read("/csv/dbunittestentity.csv", repository, TestEntity.class, Long.class, "TEST_ENTITY"));
        assertEquals("Cannot find any field matching attribute 'b' for " + TestEntity.class, e.getMessage());
    }

    @Test
    public void readFileWithIncorrectAttribute() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> csvReader.read("/csv/broken_wrong_attribute.csv", repository, TestEntity.class, Long.class, "TEST_ENTITY"));
        assertEquals("Cannot find any field matching attribute 'namex' for " + TestEntity.class, e.getMessage());
    }

    @Test
    public void readFileWithoutIdAttributeAndNoPrimaryKeyGenerator() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(IllegalStateException.class,
                () -> csvReader.read("/csv/broken_no_id_attribute.csv", repository, TestEntity.class, Long.class, "TEST_ENTITY"));
        assertTrue(TestUtil.castToNonNull(e.getMessage()).matches(
                "Primary key is null and no primary key generator available: entity=" + TestEntity.class.getName() + "@[0-9a-f]+"));
    }

    @Test
    public void readNonExistingFile() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(FileNotFoundException.class,
                () -> csvReader.read("/csv/nosuchfile.csv", repository, TestEntity.class, Long.class, "TEST_ENTITY"));
        assertEquals("/csv/nosuchfile.csv", e.getMessage());
    }
}
