package com.reallifedeveloper.tools.test.database.dbunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;

import com.reallifedeveloper.tools.test.TestUtil;
import com.reallifedeveloper.tools.test.database.csv.CsvDatabaseReader;
import com.reallifedeveloper.tools.test.database.dbunit.DbUnitTestEntity.TestEnum;
import com.reallifedeveloper.tools.test.database.inmemory.InMemoryJpaRepository;

/**
 * Defines a set of test cases to be used to test different classes that read text files to populate repositories, e.g.,
 * {@link DbUnitFlatXmlReader} and {@link CsvDatabaseReader}.
 * <p>
 * To use this interface, create a test class that implements it, and implement the various read methods.
 *
 * @author RealLifeDeveloper
 */
public interface DatabaseReaderTestCases {

    /**
     * Read a file with information about {@code TestEntity} objects and save them in a repository.
     *
     * @param repository the repository in which to save the entities
     *
     * @throws Exception if something goes wrong
     */
    void readTestEntityFile(CrudRepository<TestEntity, Long> repository) throws Exception;

    /**
     * Read one or more files with information about {@code SbUnitTestEntity} objects and save them in a repository.
     *
     * @param repository the repository in which to save the entities
     *
     * @throws Exception if something goes wrong
     */
    void readDbUnitTestEntityFiles(CrudRepository<DbUnitTestEntity, Integer> repository) throws Exception;

    /**
     * Try to read {@code TestEntity} information from a file with {@code DeUnitTestEntity} object information.
     *
     * @param repository the repository in which to save the entities
     *
     * @throws Exception if something goes wrong
     */
    void readTestEntityFromWrongTypeOfFile(CrudRepository<TestEntity, Long> repository) throws Exception;

    /**
     * Read a file with information about {@code TestEntity} objects, but with the {@code name} attribute misspelled as {@code namex}.
     *
     * @param repository the repository in which to save the entities
     *
     * @throws Exception if something goes wrong
     */
    void readTestEntityFileWithIncorrectAttribute(CrudRepository<TestEntity, Long> repository) throws Exception;

    /**
     * Read a file with information about {@code TestEntity} objects, but missing the {@code id} attribute.
     *
     * @param repository the repository in which to save the entities
     *
     * @throws Exception if something goes wrong
     */
    void readTestEntityFileWithNoIdAttribute(CrudRepository<TestEntity, Long> repository) throws Exception;

    /**
     * Try to read {@code TestEntity} information from a non-existing file named {@code /no/such/file}.
     *
     * @param repository the repository in which to save the entities
     *
     * @throws Exception if something goes wrong
     */
    void readTestEntityFromNonExistingFile(CrudRepository<TestEntity, Long> repository) throws Exception;

    @Test
    default void readFileForSimpleEntity() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        readTestEntityFile(repository);

        assertEquals(3, repository.count(), "Wrong number of entities in repository: ");

        @Nullable
        TestEntity expected = new TestEntity(42L, "foo");
        @Nullable
        TestEntity actual = repository.findById(42L).get();
        verifyEntity(expected, actual);

        expected = new TestEntity(4711L, "bar");
        actual = repository.findById(4711L).get();
        verifyEntity(expected, actual);

        expected = new TestEntity(9999L, null);
        actual = repository.findById(9999L).get();
        verifyEntity(expected, actual);
    }

    @Test
    default void readFileForEntityWithAssociations() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> testEntityRepository = new InMemoryJpaRepository<>();
        InMemoryJpaRepository<DbUnitTestEntity, Integer> repository = new InMemoryJpaRepository<>();
        readTestEntityFile(testEntityRepository);
        readDbUnitTestEntityFiles(repository);

        assertEquals(2, repository.count(), "Wrong number of entities in repository: ");

        TestEntity testEntity42 = new TestEntity(42L, "foo");
        TestEntity testEntity4711 = new TestEntity(4711L, "bar");
        TestEntityWithoutRepository testEntityWithoutRepository1 = new TestEntityWithoutRepository(
                UUID.fromString("1b262fbd-5cf7-4cb6-9c91-9a5a601816d2"), "t1");
        TestEntityWithoutRepository testEntityWithoutRepository2 = new TestEntityWithoutRepository(
                UUID.fromString("dec505f2-2e23-4814-8815-3dbd5078961c"), "t2");
        Map<UUID, TestEntityWithoutRepository> mappedEntities = new HashMap<>();
        mappedEntities.put(testEntityWithoutRepository1.id(), testEntityWithoutRepository1);
        mappedEntities.put(testEntityWithoutRepository2.id(), testEntityWithoutRepository2);
        DbUnitTestEntity expected = new DbUnitTestEntity((byte) 1, (short) 2, 3, 4L, 5.0f, 6.0, false, 'a', "foo",
                TestUtil.parseDate("2014-01-01"), LocalDate.parse("2026-05-10"), LocalDateTime.parse("2026-05-10T10:45:00"),
                ZonedDateTime.parse("2026-05-10T12:45:00+02:00"), TestEnum.FOO, new BigDecimal("1234.56"), new BigInteger("9999999999"),
                Arrays.asList("foo", "bar"), new TestEntity(42L, "foo"), Arrays.asList(new TestEntity[] { testEntity42, testEntity4711 }),
                testEntityWithoutRepository2,
                Arrays.asList(new TestEntityWithoutRepository[] { testEntityWithoutRepository1, testEntityWithoutRepository2 }),
                mappedEntities);
        testEntityWithoutRepository1.dbUnitTestEntity(expected);
        testEntityWithoutRepository2.dbUnitTestEntity(expected);
        DbUnitTestEntity actual = repository.findById(3).get();
        verifyEntity(expected, actual);

        expected = new DbUnitTestEntity((byte) 10, (short) 11, 12, 13L, 14.0f, 15.0, true, 'b', "bar", TestUtil.parseDate("2015-01-01"),
                LocalDate.parse("2026-05-10"), LocalDateTime.parse("2026-05-10T11:45:00"), ZonedDateTime.parse("2026-05-10T13:45:00+02:00"),
                TestEnum.BAR, new BigDecimal("-1000.001"), new BigInteger("8888888888"), Arrays.asList("baz"), new TestEntity(4711L, "bar"),
                Collections.emptyList(), testEntityWithoutRepository1, Collections.emptyList(), Collections.emptyMap());
        actual = repository.findById(12).get();
        verifyEntity(expected, actual);
    }

    @Test
    default void readFileForEntityWithMissingAssociations() throws Exception {
        InMemoryJpaRepository<DbUnitTestEntity, Integer> repository = new InMemoryJpaRepository<>();
        // We do not read the TestEntity file before reading the DbUnitTestEntity files:
        Exception e = assertThrows(IllegalArgumentException.class, () -> readDbUnitTestEntityFiles(repository));
        assertEquals("Entity of class " + TestEntity.class.getName() + " with primary key 42 not found", e.getMessage());

    }

    @Test
    default void readWrongTypeOfFile() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        readTestEntityFromWrongTypeOfFile(repository);
        assertEquals(0, repository.count(), "Wrong number of entities in repository: ");
    }

    @Test
    default void readFileWithIncorrectAttribute() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(IllegalArgumentException.class, () -> readTestEntityFileWithIncorrectAttribute(repository));
        assertEquals("Cannot find any field matching attribute 'namex' for " + TestEntity.class, e.getMessage());
    }

    @Test
    default void readFileWithoutIdAttributeAndNoPrimaryKeyGenerator() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(IllegalStateException.class, () -> readTestEntityFileWithNoIdAttribute(repository));
        assertTrue(TestUtil.castToNonNull(e.getMessage()).matches(
                "Primary key is null and no primary key generator available: entity=" + TestEntity.class.getName() + "@[0-9a-f]+"));
    }

    @Test
    default void readNonExistingFile() throws Exception {
        InMemoryJpaRepository<TestEntity, Long> repository = new InMemoryJpaRepository<>();
        Exception e = assertThrows(FileNotFoundException.class, () -> readTestEntityFromNonExistingFile(repository));
        assertEquals("/no/such/file", e.getMessage());
    }

    static void verifyEntity(TestEntity expected, TestEntity actual) {
        assertNotNull(actual, "Entity should not be null");
        assertEquals(expected.id(), actual.id(), "Wrong id field: ");
        assertEquals(expected.name(), actual.name(), "Wrong name field: ");
    }

    static void verifyEntity(DbUnitTestEntity expected, DbUnitTestEntity actual) {
        if (expected == null) {
            assertNull("Entity should be null");
        } else {
            assertNotNull(actual, "Entity should not be null");
            assertEquals(expected.b().byteValue(), actual.b().byteValue(), "Wrong byte field: ");
            assertEquals(expected.s().shortValue(), actual.s().shortValue(), "Wrong short field: ");
            assertEquals(expected.id().intValue(), actual.id().intValue(), "Wrong integer field: ");
            assertEquals(expected.l().longValue(), actual.l().longValue(), "Wrong long field: ");
            assertEquals(expected.f().floatValue(), actual.f().floatValue(), "Wrong float field: ");
            assertEquals(expected.d().doubleValue(), actual.d().doubleValue(), "Wrong double field: ");
            assertEquals(expected.bool().booleanValue(), actual.bool().booleanValue(), "Wrong boolean field: ");
            assertEquals(expected.c().charValue(), actual.c().charValue(), "Wrong char field: ");
            assertEquals(expected.string(), actual.string(), "Wrong String field: ");
            assertEquals(expected.date(), actual.date(), "Wrong date field: ");
            assertEquals(expected.localDate(), actual.localDate());
            assertEquals(expected.localDateTime(), actual.localDateTime());
            assertEquals(expected.zonedDateTime(), actual.zonedDateTime());
            assertEquals(expected.testEnum(), actual.testEnum(), "Wrong enum field: ");
            assertEquals(expected.bd(), actual.bd(), "Wrong BigDwcimal field: ");
            assertEquals(expected.bi(), actual.bi(), "Wrong BigInteger field: ");
            assertEquals(expected.strings(), actual.strings());
            verifyEntity(expected.testEntityWithoutRepository(), actual.testEntityWithoutRepository());
            if (expected.testEntity() == null) {
                assertNull(actual.testEntity(), "Test entity should be null");
            } else {
                verifyEntity(expected.testEntity(), actual.testEntity());
            }
            assertEquals(expected.testEntities().size(), actual.testEntities().size(), "Wrong number of test entities: ");
            for (int i = 0; i < expected.testEntities().size(); i++) {
                verifyEntity(expected.testEntities().get(i), actual.testEntities().get(i));
            }
            assertEquals(expected.testEntitiesWithoutRepository().size(), actual.testEntitiesWithoutRepository().size(),
                    "Wrong number of test entities without repository");
            for (int i = 0; i < expected.testEntitiesWithoutRepository().size(); i++) {
                verifyEntity(expected.testEntitiesWithoutRepository().get(0), actual.testEntitiesWithoutRepository().get(0));
            }
            assertEquals(expected.mappedEntitiesWithoutRepository().size(), actual.mappedEntitiesWithoutRepository().size(),
                    "Wrong number of mapped test entities witout repository");
            for (Entry<UUID, TestEntityWithoutRepository> entry : expected.mappedEntitiesWithoutRepository().entrySet()) {
                verifyEntity(entry.getValue(), actual.mappedEntitiesWithoutRepository().get(entry.getKey()));
            }
        }
    }

    static void verifyEntity(TestEntityWithoutRepository expected, @Nullable TestEntityWithoutRepository actual) {
        assertNotNull(actual, "Entity should not be null");
        assertEquals(expected.id(), actual.id(), "Wrong id field: ");
        assertEquals(expected.name(), actual.name(), "Wrong name field: ");
        if (expected.dbUnitTestEntity() == null) {
            assertNull(actual.dbUnitTestEntity(), "Expected dbUnitTestEntity field to be null");
        } else {
            assertNotNull(actual.dbUnitTestEntity(), "dbUnitTestEntity should not be null");
            assertEquals(expected.dbUnitTestEntity().id(), actual.dbUnitTestEntity().id(), "Wrong dbUnitTestEntity id");
        }
    }

}
