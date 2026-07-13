package com.reallifedeveloper.tools.test.database.dbunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;

import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter;
import com.reallifedeveloper.tools.test.database.inmemory.InMemoryJpaRepository;

/**
 * We run this test using in-memory repositories to verify that the {@link CrudRepositoryWriter} works with this repository type.
 *
 * @author RealLifeDeveloper
 */
public class DbUnitFlatXmlReaderTest implements DatabaseReaderTestCases {

    private DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();
    private CrudRepository<TestEntity, Long> testEntityRepository = new InMemoryJpaRepository<>();
    private CrudRepository<DbUnitTestEntity, Integer> dbUnitTestEntityRepository = new InMemoryJpaRepository<>();

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
        xmlReader.read("/dbunit/testentity.xml", testEntityRepository(), TestEntity.class, TestEntity.class);
    }

    @Override
    public void readDbUnitTestEntityFiles() throws Exception {
        xmlReader.read("/dbunit/dbunittestentity.xml", dbUnitTestEntityRepository(), DbUnitTestEntity.class, DbUnitTestEntity.class);
        xmlReader.read("/dbunit/testentitywithoutrepository.xml", dbUnitTestEntityRepository(), DbUnitTestEntity.class,
                TestEntityWithoutRepository.class);
    }

    @Override
    public void readTestEntityFromWrongTypeOfFile() throws Exception {
        xmlReader.read("/dbunit/dbunittestentity.xml", testEntityRepository(), TestEntity.class, DbUnitTestEntity.class);
    }

    @Override
    public void readTestEntityFileWithIncorrectAttribute() throws Exception {
        xmlReader.read("/dbunit/broken_wrong_attribute.xml", testEntityRepository(), TestEntity.class, TestEntity.class);
    }

    @Override
    public void readTestEntityFileWithNoIdAttribute() throws Exception {
        xmlReader.read("/dbunit/broken_no_id_attribute.xml", testEntityRepository(), TestEntity.class, TestEntity.class);
    }

    @Override
    public void readTestEntityFromNonExistingFile() throws Exception {
        xmlReader.read("/no/such/file", testEntityRepository(), TestEntity.class, TestEntity.class);
    }

    @Override
    @Test
    public void readWrongTypeOfFile() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class, () -> readTestEntityFromWrongTypeOfFile());
        assertEquals("Entity of " + TestEntity.class + " with primary key 42 not found", e.getMessage());
    }

}
