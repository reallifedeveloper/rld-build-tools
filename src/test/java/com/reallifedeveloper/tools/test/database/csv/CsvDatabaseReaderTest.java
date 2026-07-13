package com.reallifedeveloper.tools.test.database.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter;
import com.reallifedeveloper.tools.test.database.dbunit.DatabaseReaderTestCases;
import com.reallifedeveloper.tools.test.database.dbunit.DatabaseTestConfiguration;
import com.reallifedeveloper.tools.test.database.dbunit.DbUnitTestEntity;
import com.reallifedeveloper.tools.test.database.dbunit.TestEntity;
import com.reallifedeveloper.tools.test.database.dbunit.TestEntityWithoutRepository;

/**
 * We run this test using "real" Spring Data repositories in order to verify that the {@link CrudRepositoryWriter} works with this
 * repository type, and that the JPA mappings in the test classes are correct.
 *
 * @author RealLifeDeveloper
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DatabaseTestConfiguration.class)
public class CsvDatabaseReaderTest implements DatabaseReaderTestCases {

    private CsvDatabaseReader csvReader = new CsvDatabaseReader(';', 0);

    @Autowired
    private CrudRepository<TestEntity, Long> testEntityRepository;

    @Autowired
    private CrudRepository<DbUnitTestEntity, Integer> dbUnitTestEntityRepository;

    @Override
    public CrudRepository<TestEntity, Long> testEntityRepository() {
        return testEntityRepository;
    }

    @Override
    public CrudRepository<DbUnitTestEntity, Integer> dbUnitTestEntityRepository() {
        return dbUnitTestEntityRepository;
    }

    @Override
    public void readTestEntityFile() throws Exception {
        csvReader.read("/csv/testentity.csv", testEntityRepository(), TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    public void readDbUnitTestEntityFiles() throws Exception {
        csvReader.read("/csv/dbunittestentity.csv", dbUnitTestEntityRepository(), DbUnitTestEntity.class, DbUnitTestEntity.class,
                "DBUNITTESTENTITY");
        csvReader.read("/csv/dbunittestentity_testentity.csv", dbUnitTestEntityRepository(), DbUnitTestEntity.class, null,
                "DBUNITTESTENTITY_TESTENTITY");
        csvReader.read("/csv/testentitywithoutrepository.csv", dbUnitTestEntityRepository, DbUnitTestEntity.class,
                TestEntityWithoutRepository.class, "TEST_ENTITY_WITHOUT_REPOSITORY");
    }

    @Override
    public void readTestEntityFromWrongTypeOfFile() throws Exception {
        csvReader.read("/csv/dbunittestentity.csv", testEntityRepository(), TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    public void readTestEntityFileWithIncorrectAttribute() throws Exception {
        csvReader.read("/csv/broken_wrong_attribute.csv", testEntityRepository(), TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    public void readTestEntityFileWithNoIdAttribute() throws Exception {
        csvReader.read("/csv/broken_no_id_attribute.csv", testEntityRepository(), TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    public void readTestEntityFromNonExistingFile() throws Exception {
        csvReader.read("/no/such/file", testEntityRepository(), TestEntity.class, TestEntity.class, "TEST_ENTITY");
    }

    @Override
    @Test
    public void readWrongTypeOfFile() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class, () -> readTestEntityFromWrongTypeOfFile());
        assertEquals("Cannot find any field matching attribute 'b' for " + TestEntity.class, e.getMessage());
    }

}
