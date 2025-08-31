package com.reallifedeveloper.tools.test.database.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@SuppressWarnings({ "unchecked", "NullAway" })
public class InMemoryJpaRepositoryTest extends AbstractInMemoryCrudRepositoryTest {

    private final InMemoryJpaRepository<TestEntityWithFieldAnnotations, Integer> repository = new InMemoryJpaRepository<>();

    @Override
    protected InMemoryJpaRepository<TestEntityWithFieldAnnotations, Integer> repository() {
        return repository;
    }

    @Override
    protected TestEntityWithFieldAnnotations createTestEntity(Integer id, String name) {
        return new TestEntityWithFieldAnnotations(id, name);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void deleteNullEntitiesInBatch() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> repository().deleteInBatch((Iterable<TestEntityWithFieldAnnotations>) null));
        assertEquals("entitiesToDelete must not be null", e.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void deleteEntitiesInBatch() {
        TestEntityWithFieldAnnotations foo = createTestEntity(1, "foo");
        TestEntityWithFieldAnnotations bar = createTestEntity(2, "bar");
        TestEntityWithFieldAnnotations baz = createTestEntity(3, "baz");
        repository().saveAll(Arrays.asList(new TestEntityWithFieldAnnotations[] { foo, bar, baz }));
        assertEquals(3, repository().count());
        repository().deleteInBatch(Arrays.asList(new TestEntityWithFieldAnnotations[] { bar }));
        assertEquals(2, repository().count());
        repository().deleteInBatch(Arrays.asList(new TestEntityWithFieldAnnotations[] { foo, baz }));
        assertEquals(0, repository().count());
        repository().deleteInBatch(Arrays.asList(new TestEntityWithFieldAnnotations[] { bar }));
        assertEquals(0, repository().count());
    }

    @Test
    public void deleteAllInBatchEmpty() {
        // Check for no exception
        repository().deleteAllInBatch();
    }

    @Test
    public void deleteAllInBatch() {
        repository().save(createTestEntity(1, "foo"));
        repository().save(createTestEntity(2, "bar"));
        assertEquals(2, repository().count());
        repository().deleteAllInBatch();
        assertEquals(0, repository().count());
    }

    @Test
    public void deleteAllByIdInBatch() {
        repository().saveAll(Arrays.asList(createTestEntity(1, "foo"), createTestEntity(2, "bar"), createTestEntity(3, "baz")));
        repository().deleteAllByIdInBatch(Arrays.asList(1, 3));
        assertEquals(1, repository().count());
        assertTrue(repository().existsById(2), "foo");
    }

    @Test
    public void getByIdEmpty() {
        Exception e = assertThrows(EntityNotFoundException.class, () -> repository().getOne(1));
        assertEquals("Entity with ID 1 not found", e.getMessage());
    }

    @Test
    public void getById() {
        Exception e = assertThrows(EntityNotFoundException.class, () -> repository().getById(1));
        assertEquals("Entity with ID 1 not found", e.getMessage());
        TestEntityWithFieldAnnotations foo = createTestEntity(1, "foo");
        repository().save(foo);
        assertEquals(foo, repository().getById(1), "Entity 1 should be found");
        e = assertThrows(EntityNotFoundException.class, () -> repository().getById(3));
        assertEquals("Entity with ID 3 not found", e.getMessage());
    }

    @Test
    public void getOne() {
        Exception e = assertThrows(EntityNotFoundException.class, () -> repository().getOne(1));
        assertEquals("Entity with ID 1 not found", e.getMessage());
        TestEntityWithFieldAnnotations foo = createTestEntity(1, "foo");
        repository().save(foo);
        assertEquals(foo, repository().getOne(1), "Entity 1 should be found");
    }

    @Test
    public void saveNullPrimaryKeyWithGenerator() {
        InMemoryJpaRepository<TestEntityWithFieldAnnotations, Integer> repositoryWithKeyGenerator = new InMemoryJpaRepository<>(
                new IntegerPrimaryKeyGenerator());
        TestEntityWithFieldAnnotations foo = createTestEntity(null, "foo");
        TestEntityWithFieldAnnotations bar = createTestEntity(42, "bar");
        TestEntityWithFieldAnnotations baz = createTestEntity(null, "baz");
        repositoryWithKeyGenerator.save(foo);
        assertNotNull(foo.getId(), "Primary key should have been set");
        assertEquals(1, foo.getId().longValue(), "Wrong primary key: ");
        repositoryWithKeyGenerator.save(bar);
        repositoryWithKeyGenerator.save(baz);
        assertNotNull(baz.getId(), "Primary key should have been set");
        assertEquals(43, baz.getId().longValue(), "Wrong primary key: ");
    }

    @Test
    public void saveNullPrimaryKeyWithGeneratorAndMethodAnnotations() {
        InMemoryJpaRepository<TestEntityWithMethodAnnotations, Long> repositoryWithKeyGenerator = new InMemoryJpaRepository<>(
                new LongPrimaryKeyGenerator());
        TestEntityWithMethodAnnotations foo = new TestEntityWithMethodAnnotations(null, "foo");
        TestEntityWithMethodAnnotations bar = new TestEntityWithMethodAnnotations(null, "bar");
        repositoryWithKeyGenerator.save(foo);
        assertNotNull(foo.getId(), "Primary key should have been set");
        assertEquals(1, foo.getId().longValue(), "Wrong primary key: ");
        repositoryWithKeyGenerator.save(bar);
        assertNotNull(bar.getId(), "Primary key should have been set");
        assertEquals(2, bar.getId().longValue(), "Wrong primary key: ");
    }

    @Test
    public void saveAndFlushNullEntity() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().saveAndFlush(null));
        assertEquals("entity must not be null", e.getMessage());
    }

    @Test
    public void saveAndFlush() {
        TestEntityWithFieldAnnotations foo = createTestEntity(1, "foo");
        assertFalse(repository().findById(1).isPresent(), "Entity 1 should not be found");
        repository().saveAndFlush(foo);
        assertEquals(foo, repository().getById(1), "Entity 1 should be found");
    }

    @Test
    public void saveAllAndFlush() {
        TestEntityWithFieldAnnotations foo = createTestEntity(1, "foo");
        TestEntityWithFieldAnnotations bar = createTestEntity(2, "bar");
        assertFalse(repository().findById(1).isPresent(), "Entity 1 should not be found");
        assertFalse(repository().findById(2).isPresent(), "Entity 2 should not be found");
        repository().saveAllAndFlush(Arrays.asList(foo, bar));
        assertEquals(foo, repository().getById(1), "Entity 1 should be found");
        assertEquals(bar, repository().getById(2), "Entity 2 should be found");
    }

    @Test
    public void saveTestEntityWithMethodAnnotations() {
        InMemoryJpaRepository<TestEntityWithMethodAnnotations, Long> repo = new InMemoryJpaRepository<>();
        TestEntityWithMethodAnnotations foo = new TestEntityWithMethodAnnotations(1L, "foo");
        repo.save(foo);
        assertEquals(1, repo.count());
        assertEquals(foo, repo.getById(1L), "Entity 1 should be found");
    }

    @Test
    public void mappedSuperclassWithFieldAnnotations() {
        InMemoryJpaRepository<ConcreteEntityWithFieldAnnotations, Integer> repo = new InMemoryJpaRepository<>();
        ConcreteEntityWithFieldAnnotations foo = new ConcreteEntityWithFieldAnnotations(1, "foo");
        repo.save(foo);
        assertEquals(1, repo.count());
        assertEquals(foo, repo.getById(1), "Entity 1 should be found");
    }

    @Test
    public void mappedSuperclassWithMethodAnnotations() {
        InMemoryJpaRepository<ConcreteEntityWithMethodAnnotations, Integer> repo = new InMemoryJpaRepository<>();
        ConcreteEntityWithMethodAnnotations foo = new ConcreteEntityWithMethodAnnotations(1, "foo");
        repo.save(foo);
        assertEquals(1, repo.count());
        assertEquals(foo, repo.getById(1), "Entity 1 should be found");
    }

    @Test
    public void idClassSaveWithPrimaryKeys() {
        InMemoryJpaRepository<TestEntityWithFieldAnnotationsAndIdClass, PrimaryKeyClass> repo = new InMemoryJpaRepository<>();
        TestEntityWithFieldAnnotationsAndIdClass foo = new TestEntityWithFieldAnnotationsAndIdClass(1, 10, "foo");
        repo.save(foo);
        assertEquals(1, repo.count());
        assertEquals(foo, repo.getById(new PrimaryKeyClass(1, 10)), "Entity (1, 10) should be found");
    }

    @Test
    public void idClassSaveWithoutPrimaryKeys() {
        InMemoryJpaRepository<TestEntityWithFieldAnnotationsAndIdClass, PrimaryKeyClass> repo = new InMemoryJpaRepository<>(
                new PrimaryKeyClass.Generator());
        TestEntityWithFieldAnnotationsAndIdClass foo = new TestEntityWithFieldAnnotationsAndIdClass(null, null, "foo");
        TestEntityWithFieldAnnotationsAndIdClass savedFoo = repo.save(foo);
        assertEquals(1, savedFoo.getId1().intValue(), "Unexpected id1: ");
        assertEquals(1, savedFoo.getId2().intValue(), "Unexpected id2: ");
        assertEquals(1, repo.count());
        assertEquals(foo, repo.getById(new PrimaryKeyClass(1, 1)), "Entity (1, 1) should be found");
    }

    @Test
    public void testToString() {
        assertEquals("InMemoryJpaRepository{entities={}}", repository().toString(), "Unexpected result from toString: ");
    }

    /**
     * A JPA entity that uses field access, i.e., puts JPA annotations on fields.
     */
    @Entity
    private static class TestEntityWithFieldAnnotations implements TestEntity {

        @Id
        private final Integer id;

        @Column
        private final String name;

        TestEntityWithFieldAnnotations(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * A JPA entity that uses property access, i.e., puts JPA annotations on get methods.
     */
    @Entity
    @SuppressWarnings("UnusedMethod")
    public static class TestEntityWithMethodAnnotations {

        private Long id;
        private String name;

        TestEntityWithMethodAnnotations(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Id
        Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Column
        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }
    }

    /**
     * A base class for JPA entities that use field access, i.e., puts JPA annotations on fields.
     *
     * @param <ID> the type of the primary key
     */
    @MappedSuperclass
    @SuppressWarnings("UnusedMethod")
    private abstract static class AbstractEntityWithFieldAnnotations<ID> {

        @Id
        private final ID id;

        AbstractEntityWithFieldAnnotations(ID id) {
            this.id = id;
        }

        ID getId() {
            return id;
        }
    }

    /**
     * A JPA entity that inherits from {@link AbstractEntityWithFieldAnnotations} and uses field access.
     */
    @Entity
    @SuppressWarnings("UnusedMethod")
    private static class ConcreteEntityWithFieldAnnotations extends AbstractEntityWithFieldAnnotations<Integer> {

        @Column
        private final String name;

        ConcreteEntityWithFieldAnnotations(Integer id, String name) {
            super(id);
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * A base class for JPA entities that use property access, i.e., puts JPA annotations on get methods.
     *
     * @param <ID> the type of the primary key
     */
    @MappedSuperclass
    @SuppressWarnings("UnusedMethod")
    private abstract static class AbstractEntityWithMethodAnnotations<ID> {

        private final ID id;

        AbstractEntityWithMethodAnnotations(ID id) {
            this.id = id;
        }

        @Id
        ID getId() {
            return id;
        }
    }

    /**
     * A JPA entity that inherits from {@link AbstractEntityWithMethodAnnotations} and uses property access.
     */
    @Entity
    @SuppressWarnings("UnusedMethod")
    private static class ConcreteEntityWithMethodAnnotations extends AbstractEntityWithMethodAnnotations<Integer> {

        private final String name;

        ConcreteEntityWithMethodAnnotations(Integer id, String name) {
            super(id);
            this.name = name;
        }

        @Column
        String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Entity
    @IdClass(PrimaryKeyClass.class)
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    @Getter
    private static class TestEntityWithFieldAnnotationsAndIdClass {
        @Id
        private Integer id1;

        @Id
        private Integer id2;

        @Column
        private String name;

    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    @Getter
    @EqualsAndHashCode
    @ToString
    @SuppressWarnings("serial")
    private static class PrimaryKeyClass implements Serializable, Comparable<PrimaryKeyClass> {
        private Integer id1;
        private Integer id2;

        @Override
        public int compareTo(PrimaryKeyClass o) {
            int id1Diff = Integer.compare(id1, o.id1);
            if (id1Diff != 0) {
                return id1Diff;
            } else {
                return Integer.compare(id2, o.id2);
            }
        }

        private static class Generator implements PrimaryKeyGenerator<PrimaryKeyClass> {

            @Override
            public PrimaryKeyClass nextPrimaryKey(PrimaryKeyClass previousMax) {
                if (previousMax == null) {
                    return new PrimaryKeyClass(1, 1);
                } else {
                    return new PrimaryKeyClass(previousMax.id1 + 1, previousMax.id2 + 1);
                }
            }

        }
    }
}
