package com.reallifedeveloper.tools.test.database.inmemory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class InMemoryJpaRepositoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final InMemoryJpaRepository<TestEntityWithFieldAnnotations, Integer> repository =
            new InMemoryJpaRepository<>();

    @Test
    public void findByField() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        repository.save(new TestEntityWithFieldAnnotations(2, "bar"));
        repository.save(new TestEntityWithFieldAnnotations(3, "foo"));
        repository.save(new TestEntityWithFieldAnnotations(4, "baz"));
        List<TestEntityWithFieldAnnotations> foundEntities = repository.findByField("name", "foo");
        Assert.assertEquals("Wrong number of 'foo' entities: ", 2, foundEntities.size());
        Assert.assertEquals("Wrong id for first 'foo' entity: ", 1, foundEntities.get(0).getId().longValue());
        Assert.assertEquals("Wrong name for first 'foo' entity: ", "foo", foundEntities.get(0).getName());
        Assert.assertEquals("Wrong id for second 'foo' entity: ", 3, foundEntities.get(1).getId().longValue());
        Assert.assertEquals("Wrong name for second 'foo' entity: ", "foo", foundEntities.get(1).getName());
        foundEntities = repository.findByField("name", "bar");
        Assert.assertEquals("Wrong number of 'bar' entities: ", 1, foundEntities.size());
        Assert.assertEquals("Wrong id for first 'bar' entity: ", 2, foundEntities.get(0).getId().longValue());
        Assert.assertEquals("Wrong name for first 'bar' entity: ", "bar", foundEntities.get(0).getName());
        foundEntities = repository.findByField("name", "baz");
        Assert.assertEquals("Wrong number of 'baz' entities: ", 1, foundEntities.size());
        Assert.assertEquals("Wrong id for first 'baz' entity: ", 4, foundEntities.get(0).getId().longValue());
        Assert.assertEquals("Wrong name for first 'baz' entity: ", "baz", foundEntities.get(0).getName());
    }

    @Test
    public void findByFieldEmptyRepository() {
        List<TestEntityWithFieldAnnotations> foundEntities = repository.findByField("name", "foo");
        Assert.assertNotNull("List of found entities should not be null", foundEntities);
        Assert.assertTrue("No 'foo' entities should be found", foundEntities.isEmpty());
    }

    @Test
    public void findByFieldNoneFound() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        List<TestEntityWithFieldAnnotations> foundEntities = repository.findByField("name", "bar");
        Assert.assertNotNull("List of found entities should not be null", foundEntities);
        Assert.assertTrue("No 'bar' entities should be found", foundEntities.isEmpty());
    }

    @Test
    public void findByFieldNoSuchField() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Field not found or not accessible: noSuchField");
        repository.findByField("noSuchField", "foo");
    }

    @Test
    public void findByFieldNullFieldName() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("fieldName must not be null");
        repository.findByField(null, "foo");
    }

    @Test
    public void findByFieldNullValue() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        repository.save(new TestEntityWithFieldAnnotations(2, null));
        repository.save(new TestEntityWithFieldAnnotations(3, "bar"));
        List<TestEntityWithFieldAnnotations> foundEntities = repository.findByField("name", null);
        Assert.assertNotNull("List of found entities should not be null", foundEntities);
        Assert.assertEquals("Exactly one entity should be found: ", 1, foundEntities.size());
        Assert.assertEquals("Wrong id for 'null' entity: ", 2, foundEntities.get(0).getId().longValue());
    }

    @Test
    public void findByUniqueField() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        repository.save(new TestEntityWithFieldAnnotations(2, "bar"));
        repository.save(new TestEntityWithFieldAnnotations(3, "baz"));
        TestEntityWithFieldAnnotations entity = repository.findByUniqueField("name", "foo");
        Assert.assertEquals("Wrong id for 'foo' entity: ", 1, entity.getId().longValue());
        Assert.assertEquals("Wrong name for 'foo' entity: ", "foo", entity.getName());
        entity = repository.findByUniqueField("name", "bar");
        Assert.assertEquals("Wrong id for 'bar' entity: ", 2, entity.getId().longValue());
        Assert.assertEquals("Wrong name for 'bar' entity: ", "bar", entity.getName());
        entity = repository.findByUniqueField("name", "baz");
        Assert.assertEquals("Wrong id for 'baz' entity: ", 3, entity.getId().longValue());
        Assert.assertEquals("Wrong name for 'baz' entity: ", "baz", entity.getName());
    }

    @Test
    public void findByUniqueFieldNotFound() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        Assert.assertNull("'bar' entity should not be found", repository.findByUniqueField("name", "bar"));
    }

    @Test
    public void findByUniqueFieldNotUnique() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        repository.save(new TestEntityWithFieldAnnotations(2, "foo"));
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Field name is not unique, found 2 entities: [foo, foo]");
        repository.findByUniqueField("name", "foo");
    }

    @Test
    public void countEmpty() {
        Assert.assertEquals("Empty repository should have count 0", 0, repository.count());
    }

    @Test
    public void count() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        Assert.assertEquals(1, repository.count());
        repository.save(new TestEntityWithFieldAnnotations(2, "bar"));
        Assert.assertEquals(2, repository.count());
    }

    @Test
    public void deleteNullId() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("id must not be null");
        repository.delete((Integer) null);
    }

    @Test
    public void deleteId() {
        Assert.assertEquals(0, repository.count());
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        Assert.assertEquals(1, repository.count());
        repository.delete(1);
        Assert.assertEquals(0, repository.count());
    }

    @Test
    public void deleteNonExistingId() {
        expectedException.expect(EmptyResultDataAccessException.class);
        expectedException.expectMessage("Entity with id -1 not found");
        repository.delete(-1);
    }

    @Test
    public void deleteNullEntities() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("entitiesToDelete must not be null");
        repository.delete((Iterable<TestEntityWithFieldAnnotations>) null);
    }

    @Test
    public void deleteNullEntitiesInBatch() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("entitiesToDelete must not be null");
        repository.deleteInBatch((Iterable<TestEntityWithFieldAnnotations>) null);
    }

    @Test
    public void deleteEntities() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        TestEntityWithFieldAnnotations bar = new TestEntityWithFieldAnnotations(2, "bar");
        TestEntityWithFieldAnnotations baz = new TestEntityWithFieldAnnotations(3, "baz");
        repository.save(Arrays.asList(new TestEntityWithFieldAnnotations[] { foo, bar, baz }));
        Assert.assertEquals(3, repository.count());
        repository.delete(Arrays.asList(new TestEntityWithFieldAnnotations[] { bar }));
        Assert.assertEquals(2, repository.count());
        repository.delete(Arrays.asList(new TestEntityWithFieldAnnotations[] { foo, baz }));
        Assert.assertEquals(0, repository.count());
        repository.delete(Arrays.asList(new TestEntityWithFieldAnnotations[] { bar }));
        Assert.assertEquals(0, repository.count());
    }

    @Test
    public void deleteEntitiesInBatch() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        TestEntityWithFieldAnnotations bar = new TestEntityWithFieldAnnotations(2, "bar");
        TestEntityWithFieldAnnotations baz = new TestEntityWithFieldAnnotations(3, "baz");
        repository.save(Arrays.asList(new TestEntityWithFieldAnnotations[] { foo, bar, baz }));
        Assert.assertEquals(3, repository.count());
        repository.deleteInBatch(Arrays.asList(new TestEntityWithFieldAnnotations[] { bar }));
        Assert.assertEquals(2, repository.count());
        repository.deleteInBatch(Arrays.asList(new TestEntityWithFieldAnnotations[] { foo, baz }));
        Assert.assertEquals(0, repository.count());
        repository.deleteInBatch(Arrays.asList(new TestEntityWithFieldAnnotations[] { bar }));
        Assert.assertEquals(0, repository.count());
    }

    @Test
    public void deleteNullEntity() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("entity must not be null");
        repository.delete((TestEntityWithFieldAnnotations) null);
    }

    @Test
    public void deleteAllEmpty() {
        // Check for no exception
        repository.deleteAll();
    }

    @Test
    public void deleteAllInBatchEmpty() {
        // Check for no exception
        repository.deleteAllInBatch();
    }

    @Test
    public void deleteAll() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        repository.save(new TestEntityWithFieldAnnotations(2, "bar"));
        Assert.assertEquals(2, repository.count());
        repository.deleteAll();
        Assert.assertEquals(0, repository.count());
    }

    @Test
    public void deleteAllInBatch() {
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        repository.save(new TestEntityWithFieldAnnotations(2, "bar"));
        Assert.assertEquals(2, repository.count());
        repository.deleteAllInBatch();
        Assert.assertEquals(0, repository.count());
    }

    @Test
    public void existsNullId() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("id must not be null");
        repository.exists((Integer) null);
    }

    @Test
    public void exists() {
        Assert.assertFalse("Nothing should be found", repository.exists(1));
        repository.save(new TestEntityWithFieldAnnotations(1, "foo"));
        Assert.assertTrue("Entity 1 should be found", repository.exists(1));
        Assert.assertFalse("Entity 2 should not be found", repository.exists(2));
    }

    @Test
    public void findAll() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        TestEntityWithFieldAnnotations bar = new TestEntityWithFieldAnnotations(2, "bar");
        repository.save(foo);
        repository.save(bar);
        Iterable<TestEntityWithFieldAnnotations> entities = repository.findAll();
        Assert.assertTrue("findAll should contain entity foo", find(foo, entities));
        Assert.assertTrue("findAll should contain entity bar", find(bar, entities));
    }

    @Test
    public void findAllNullIds() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("ids must not be null");
        repository.findAll((Iterable<Integer>) null);
    }

    @Test
    public void findAllNoIds() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        TestEntityWithFieldAnnotations bar = new TestEntityWithFieldAnnotations(2, "bar");
        TestEntityWithFieldAnnotations baz = new TestEntityWithFieldAnnotations(3, "baz");
        repository.save(foo);
        repository.save(bar);
        repository.save(baz);
        Iterable<TestEntityWithFieldAnnotations> entities = repository.findAll(Arrays.asList(new Integer[] {}));
        Assert.assertEquals(0, size(entities));
    }

    @Test
    public void findAllIds() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        TestEntityWithFieldAnnotations bar = new TestEntityWithFieldAnnotations(2, "bar");
        TestEntityWithFieldAnnotations baz = new TestEntityWithFieldAnnotations(3, "baz");
        repository.save(foo);
        repository.save(bar);
        repository.save(baz);
        Iterable<TestEntityWithFieldAnnotations> entities = repository.findAll(Arrays.asList(new Integer[] { 1, 2 }));
        Assert.assertTrue("findAll should contain entity foo", find(foo, entities));
        Assert.assertTrue("findAll should contain entity bar", find(bar, entities));
        Assert.assertFalse("findAll should not contain entity baz", find(baz, entities));
    }

    @Test
    public void findAllIdNotFound() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        repository.save(foo);
        Iterable<TestEntityWithFieldAnnotations> entities =
                repository.findAll(Arrays.asList(new Integer[] { -1, -2 }));
        Assert.assertFalse("findAll should not contain entity foo", find(foo, entities));
    }

    @Test
    public void findAllPageable() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Not yet implemented");
        repository.findAll(new PageRequest(0, 10));
    }

    @Test
    public void findAllSort() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Not yet implemented");
        repository.findAll(new Sort("foo"));
    }

    @Test
    public void findOneNullId() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("id must not be null");
        repository.findOne((Integer) null);
    }

    @Test
    public void findOneEmpty() {
        Assert.assertNull("Entity 1 should not be found", repository.findOne(1));
    }

    @Test
    public void findOne() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        TestEntityWithFieldAnnotations bar = new TestEntityWithFieldAnnotations(2, "bar");
        repository.save(foo);
        repository.save(bar);
        Assert.assertEquals("Entity 1 should be found", foo, repository.findOne(1));
        Assert.assertEquals("Entity 2 should be found", bar, repository.findOne(2));
        Assert.assertNull("Entity 3 should not be found", repository.findOne(3));
    }

    @Test
    public void getOne() {
        // In the implementation of InMemoryJpaRepository, getOne is identical to findOne. Is this correct?
        Assert.assertNull("Entity 1 should not be found", repository.getOne(1));
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        repository.save(foo);
        Assert.assertEquals("Entity 1 should be found", foo, repository.getOne(1));
        Assert.assertNull("Entity 3 should not be found", repository.getOne(3));

    }

    @Test
    public void saveNullEntity() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("entity must not be null");
        repository.save((TestEntityWithFieldAnnotations) null);
    }

    @Test
    public void saveNullEntities() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("entitiesToSave must not be null");
        repository.save((Iterable<TestEntityWithFieldAnnotations>) null);
    }

    @Test
    public void saveNullPrimaryKeyWithNoGenerator() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Primary key is null: foo");
        repository.save(new TestEntityWithFieldAnnotations(null, "foo"));
    }

    @Test
    public void saveNullPrimaryKeyWithGenerator() {
        InMemoryJpaRepository<TestEntityWithFieldAnnotations, Integer> repositoryWithKeyGenerator =
                new InMemoryJpaRepository<>(new IntegerPrimaryKeyGenerator());
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(null, "foo");
        TestEntityWithFieldAnnotations bar = new TestEntityWithFieldAnnotations(42, "bar");
        TestEntityWithFieldAnnotations baz = new TestEntityWithFieldAnnotations(null, "baz");
        repositoryWithKeyGenerator.save(foo);
        Assert.assertNotNull("Primary key should have been set", foo.getId());
        Assert.assertEquals("Wrong primary key: ", 1, foo.getId().longValue());
        repositoryWithKeyGenerator.save(bar);
        repositoryWithKeyGenerator.save(baz);
        Assert.assertNotNull("Primary key should have been set", baz.getId());
        Assert.assertEquals("Wrong primary key: ", 43, baz.getId().longValue());
    }

    @Test
    public void saveNullPrimaryKeyWithGeneratorAndMethodAnnotations() {
        InMemoryJpaRepository<TestEntityWithMethodAnnotations, Long> repositoryWithKeyGenerator =
                new InMemoryJpaRepository<>(new LongPrimaryKeyGenerator());
        TestEntityWithMethodAnnotations foo = new TestEntityWithMethodAnnotations(null, "foo");
        TestEntityWithMethodAnnotations bar = new TestEntityWithMethodAnnotations(null, "bar");
        repositoryWithKeyGenerator.save(foo);
        Assert.assertNotNull("Primary key should have been set", foo.getId());
        Assert.assertEquals("Wrong primary key: ", 1, foo.getId().longValue());
        repositoryWithKeyGenerator.save(bar);
        Assert.assertNotNull("Primary key should have been set", bar.getId());
        Assert.assertEquals("Wrong primary key: ", 2, bar.getId().longValue());
    }

    @Test
    public void saveEntities() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        TestEntityWithFieldAnnotations bar = new TestEntityWithFieldAnnotations(2, "bar");
        TestEntityWithFieldAnnotations baz = new TestEntityWithFieldAnnotations(3, "baz");
        repository.save(foo);
        Assert.assertEquals(1, repository.count());
        List<TestEntityWithFieldAnnotations> entities = new ArrayList<>();
        repository.save(entities);
        Assert.assertEquals(1, repository.count());
        entities.add(bar);
        entities.add(baz);
        repository.save(entities);
        Assert.assertEquals(3, repository.count());
        Assert.assertEquals("Entity 1 should be found", foo, repository.findOne(1));
        Assert.assertEquals("Entity 2 should be found", bar, repository.findOne(2));
        Assert.assertEquals("Entity 3 should be found", baz, repository.findOne(3));
    }

    @Test
    public void saveAndFlushNullEntity() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("entity must not be null");
        repository.saveAndFlush(null);
    }

    @Test
    public void saveAndFlush() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        Assert.assertNull("Entity 1 should not be found", repository.findOne(1));
        repository.saveAndFlush(foo);
        Assert.assertEquals("Entity 1 should not be found", foo, repository.findOne(1));
    }

    @Test
    public void update() {
        TestEntityWithFieldAnnotations foo = new TestEntityWithFieldAnnotations(1, "foo");
        repository.save(foo);
        TestEntityWithFieldAnnotations newFoo = new TestEntityWithFieldAnnotations(1, "frotz");
        newFoo = repository.save(newFoo);
        Assert.assertEquals(1, repository.count());
        Assert.assertEquals("Entity frotz should be found", newFoo, repository.findOne(1));
        Assert.assertEquals("Entity frotz has wrong name: ", "frotz", repository.findOne(1).getName());
        Assert.assertFalse("Entity foo should not be found", find(foo, repository.findAll()));
    }

    @Test
    public void testEntityWithMethodAnnotations() {
        InMemoryJpaRepository<TestEntityWithMethodAnnotations, Long> repo = new InMemoryJpaRepository<>();
        TestEntityWithMethodAnnotations foo = new TestEntityWithMethodAnnotations(1L, "foo");
        repo.save(foo);
        Assert.assertEquals(1, repo.count());
        Assert.assertEquals("Entity 1 should be found", foo, repo.findOne(1L));
    }

    @Test
    public void entityWithNoIdAnnotation() {
        InMemoryJpaRepository<String, Integer> repo = new InMemoryJpaRepository<>();
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Entity has no @Id annotation: foo");
        repo.save("foo");
    }

    @Test
    public void mappedSuperclassWithFieldAnnotations() {
        InMemoryJpaRepository<ConcreteEntityWithFieldAnnotations, Integer> repo = new InMemoryJpaRepository<>();
        ConcreteEntityWithFieldAnnotations foo = new ConcreteEntityWithFieldAnnotations(1, "foo");
        repo.save(foo);
        Assert.assertEquals(1, repo.count());
        Assert.assertEquals("Entity 1 should be found", foo, repo.findOne(1));
    }

    @Test
    public void mappedSuperclassWithMethodAnnotations() {
        InMemoryJpaRepository<ConcreteEntityWithMethodAnnotations, Integer> repo = new InMemoryJpaRepository<>();
        ConcreteEntityWithMethodAnnotations foo = new ConcreteEntityWithMethodAnnotations(1, "foo");
        repo.save(foo);
        Assert.assertEquals(1, repo.count());
        Assert.assertEquals("Entity 1 should be found", foo, repo.findOne(1));
    }

    @Test
    public void testToString() {
        Assert.assertEquals("Unexpected result from toString: ", "InMemoryJpaRepository{entities={}}",
                repository.toString());
    }

    private boolean find(Object o, Iterable<?> objects) {
        for (Object obj : objects) {
            if (obj.equals(o)) {
                return true;
            }
        }
        return false;
    }

    private int size(Iterable<?> objects) {
        int size = 0;
        for (@SuppressWarnings("unused")
        Object obj : objects) {
            size++;
        }
        return size;
    }

    @Test
    public void countByExample() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Not yet implemented");
        repository.count((Example<TestEntityWithFieldAnnotations>) null);
    }

    @Test
    public void existsByExample() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Not yet implemented");
        repository.exists((Example<TestEntityWithFieldAnnotations>) null);
    }

    @Test
    public void findAllByExample() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Not yet implemented");
        repository.findAll((Example<TestEntityWithFieldAnnotations>) null);
    }

    @Test
    public void findAllByExampleAndPageable() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Not yet implemented");
        repository.findAll((Example<TestEntityWithFieldAnnotations>) null, (Pageable) null);
    }

    @Test
    public void findAllByExampleAndSort() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Not yet implemented");
        repository.findAll((Example<TestEntityWithFieldAnnotations>) null, (Sort) null);
    }

    @Test
    public void findOneByExample() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Not yet implemented");
        repository.findOne((Example<TestEntityWithFieldAnnotations>) null);
    }

    /**
     * A JPA entity that uses field access, i.e., puts JPA annotations on fields.
     */
    @Entity
    static class TestEntityWithFieldAnnotations {

        @Id
        private final Integer id;

        @Column
        private final String name;

        TestEntityWithFieldAnnotations(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

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
    static class TestEntityWithMethodAnnotations {

        private Long id;
        private String name;

        TestEntityWithMethodAnnotations(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Id
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Column
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * A base class for JPA entities that use field access, i.e., puts JPA annotations
     * on fields.
     *
     * @param <ID> the type of the primary key
     */
    @MappedSuperclass
    abstract static class AbstractEntityWithFieldAnnotations<ID> {

        @Id
        private final ID id;

        protected AbstractEntityWithFieldAnnotations(ID id) {
            this.id = id;
        }

        public ID getId() {
            return id;
        }
    }

    /**
     * A JPA entity that inherits from {@link AbstractEntityWithFieldAnnotations} and
     * uses field access.
     */
    @Entity
    static class ConcreteEntityWithFieldAnnotations extends AbstractEntityWithFieldAnnotations<Integer> {

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
     * A base class for JPA entities that use property access, i.e., puts JPA annotations
     * on get methods.
     *
     * @param <ID> the type of the primary key
     */
    @MappedSuperclass
    abstract static class AbstractEntityWithMethodAnnotations<ID> {

        private final ID id;

        protected AbstractEntityWithMethodAnnotations(ID id) {
            this.id = id;
        }

        @Id
        protected ID getId() {
            return id;
        }
    }

    /**
     * A JPA entity that inherits from {@link AbstractEntityWithMethodAnnotations} and
     * uses property access.
     */
    @Entity
    static class ConcreteEntityWithMethodAnnotations extends AbstractEntityWithMethodAnnotations<Integer> {

        private final String name;

        ConcreteEntityWithMethodAnnotations(Integer id, String name) {
            super(id);
            this.name = name;
        }

        @Column
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
