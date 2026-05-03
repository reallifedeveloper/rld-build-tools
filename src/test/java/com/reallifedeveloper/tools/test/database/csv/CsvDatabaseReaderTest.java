package com.reallifedeveloper.tools.test.database.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;

import com.reallifedeveloper.tools.test.database.dbunit.DatabaseReaderTestCases;
import com.reallifedeveloper.tools.test.database.dbunit.DbUnitTestEntity;
import com.reallifedeveloper.tools.test.database.dbunit.TestEntity;
import com.reallifedeveloper.tools.test.database.dbunit.TestEntityWithoutRepository;
import com.reallifedeveloper.tools.test.database.inmemory.InMemoryJpaRepository;

public class CsvDatabaseReaderTest implements DatabaseReaderTestCases {

    private CsvDatabaseReader csvReader = new CsvDatabaseReader(';', 0);

    @Override
    public void readTestEntityFile(CrudRepository<TestEntity, Long> repository) throws Exception {
        csvReader.read("/csv/testentity.csv", repository, TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    public void readDbUnitTestEntityFiles(CrudRepository<DbUnitTestEntity, Integer> repository) throws Exception {
        csvReader.read("/csv/dbunittestentity.csv", repository, DbUnitTestEntity.class, DbUnitTestEntity.class, "DBUNITTESTENTITY");
        csvReader.read("/csv/dbunittestentity_testentity.csv", repository, DbUnitTestEntity.class, null, "DBUNITTESTENTITY_TESTENTITY");
        csvReader.read("/csv/testentitywithoutrepository.csv", repository, DbUnitTestEntity.class, TestEntityWithoutRepository.class,
                "TEST_ENTITY_WITHOUT_REPOSITORY");
    }

    @Override
    public void readTestEntityFromWrongTypeOfFile(CrudRepository<TestEntity, Long> repository) throws Exception {
        csvReader.read("/csv/dbunittestentity.csv", repository, TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    public void readTestEntityFileWithIncorrectAttribute(CrudRepository<TestEntity, Long> repository) throws Exception {
        csvReader.read("/csv/broken_wrong_attribute.csv", repository, TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    public void readTestEntityFileWithNoIdAttribute(CrudRepository<TestEntity, Long> repository) throws Exception {
        csvReader.read("/csv/broken_no_id_attribute.csv", repository, TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    public void readTestEntityFromNonExistingFile(CrudRepository<TestEntity, Long> repository) throws Exception {
        csvReader.read("/no/such/file", repository, TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    @Test
    public void readWrongTypeOfFile() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(IllegalArgumentException.class, () -> readTestEntityFromWrongTypeOfFile(repository));
        assertEquals("Cannot find any field matching attribute 'b' for " + TestEntity.class, e.getMessage());
    }

}
