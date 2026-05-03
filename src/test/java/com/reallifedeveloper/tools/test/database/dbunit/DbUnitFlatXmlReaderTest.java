package com.reallifedeveloper.tools.test.database.dbunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;

import com.reallifedeveloper.tools.test.database.inmemory.InMemoryJpaRepository;

public class DbUnitFlatXmlReaderTest implements DatabaseReaderTestCases {

    private DbUnitFlatXmlReader xmlReader = new DbUnitFlatXmlReader();

    @Override
    public void readTestEntityFile(CrudRepository<TestEntity, Long> repository) throws Exception {
        xmlReader.read("/dbunit/testentity.xml", repository, TestEntity.class, TestEntity.class);
    }

    @Override
    public void readDbUnitTestEntityFiles(CrudRepository<DbUnitTestEntity, Integer> repository) throws Exception {
        xmlReader.read("/dbunit/dbunittestentity.xml", repository, DbUnitTestEntity.class, DbUnitTestEntity.class);
        xmlReader.read("/dbunit/testentitywithoutrepository.xml", repository, DbUnitTestEntity.class, TestEntityWithoutRepository.class);
    }

    @Override
    public void readTestEntityFromWrongTypeOfFile(CrudRepository<TestEntity, Long> repository) throws Exception {
        xmlReader.read("/dbunit/dbunittestentity.xml", repository, TestEntity.class, DbUnitTestEntity.class);
    }

    @Override
    public void readTestEntityFileWithIncorrectAttribute(CrudRepository<TestEntity, Long> repository) throws Exception {
        xmlReader.read("/dbunit/broken_wrong_attribute.xml", repository, TestEntity.class, TestEntity.class);
    }

    @Override
    public void readTestEntityFileWithNoIdAttribute(CrudRepository<TestEntity, Long> repository) throws Exception {
        xmlReader.read("/dbunit/broken_no_id_attribute.xml", repository, TestEntity.class, TestEntity.class);
    }

    @Override
    public void readTestEntityFromNonExistingFile(CrudRepository<TestEntity, Long> repository) throws Exception {
        xmlReader.read("/no/such/file", repository, TestEntity.class, TestEntity.class);
    }

    @Override
    @Test
    public void readWrongTypeOfFile() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(IllegalArgumentException.class, () -> readTestEntityFromWrongTypeOfFile(repository));
        assertEquals("Entity of " + TestEntity.class + " with primary key 42 not found", e.getMessage());
    }

}
