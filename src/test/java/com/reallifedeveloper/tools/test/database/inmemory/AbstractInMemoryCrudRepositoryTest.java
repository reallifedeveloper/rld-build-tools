package com.reallifedeveloper.tools.test.database.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@SuppressWarnings("NullAway")
public abstract class AbstractInMemoryCrudRepositoryTest {

    @SuppressWarnings("TypeParameterUnusedInFormals")
    protected abstract <T extends AbstractInMemoryCrudRepository<TestEntity, Integer>> T repository();

    @SuppressWarnings("TypeParameterUnusedInFormals")
    protected abstract <T extends TestEntity> T createTestEntity(@Nullable Integer id, String name);

    @Test
    public void findByField() {
        repository().save(createTestEntity(1, "foo"));
        repository().save(createTestEntity(2, "bar"));
        repository().save(createTestEntity(3, "foo"));
        repository().save(createTestEntity(4, "baz"));
        List<TestEntity> foundEntities = repository().findByField("name", "foo");
        assertEquals(2, foundEntities.size(), "Wrong number of 'foo' entities: ");
        assertEquals(1, foundEntities.get(0).getId().longValue(), "Wrong id for first 'foo' entity: ");
        assertEquals("foo", foundEntities.get(0).getName(), "Wrong name for first 'foo' entity: ");
        assertEquals(3, foundEntities.get(1).getId().longValue(), "Wrong id for second 'foo' entity: ");
        assertEquals("foo", foundEntities.get(1).getName(), "Wrong name for second 'foo' entity: ");
        foundEntities = repository().findByField("name", "bar");
        assertEquals(1, foundEntities.size(), "Wrong number of 'bar' entities: ");
        assertEquals(2, foundEntities.get(0).getId().longValue(), "Wrong id for first 'bar' entity: ");
        assertEquals("bar", foundEntities.get(0).getName(), "Wrong name for first 'bar' entity: ");
        foundEntities = repository().findByField("name", "baz");
        assertEquals(1, foundEntities.size(), "Wrong number of 'baz' entities: ");
        assertEquals(4, foundEntities.get(0).getId().longValue(), "Wrong id for first 'baz' entity: ");
        assertEquals("baz", foundEntities.get(0).getName(), "Wrong name for first 'baz' entity: ");
    }

    @Test
    public void findByFieldEmptyRepository() {
        List<TestEntity> foundEntities = repository().findByField("name", "foo");
        assertNotNull(foundEntities, "List of found entities should not be null");
        assertTrue(foundEntities.isEmpty(), "No 'foo' entities should be found");
    }

    @Test
    public void findByFieldNoneFound() {
        repository().save(createTestEntity(1, "foo"));
        List<TestEntity> foundEntities = repository().findByField("name", "bar");
        assertNotNull(foundEntities, "List of found entities should not be null");
        assertTrue(foundEntities.isEmpty(), "No 'bar' entities should be found");
    }

    @Test
    public void findByFieldNoSuchField() {
        repository().save(createTestEntity(1, "foo"));
        Exception e = assertThrows(IllegalStateException.class, () -> repository().findByField("noSuchField", "foo"));
        assertEquals("Error getting value of field noSuchField of object foo", e.getMessage());
    }

    @Test
    public void findByFieldNullFieldName() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().findByField(null, "foo"));
        assertEquals("fieldName must not be null", e.getMessage());
    }

    @Test
    public void findByFieldNullValue() {
        repository().save(createTestEntity(1, "foo"));
        repository().save(createTestEntity(2, null));
        repository().save(createTestEntity(3, "bar"));
        List<TestEntity> foundEntities = repository().findByField("name", null);
        assertNotNull(foundEntities, "List of found entities should not be null");
        assertEquals(1, foundEntities.size(), "Exactly one entity should be found: ");
        assertEquals(2, foundEntities.get(0).getId().longValue(), "Wrong id for 'null' entity: ");
    }

    @Test
    public void findByUniqueField() {
        repository().save(createTestEntity(1, "foo"));
        repository().save(createTestEntity(2, "bar"));
        repository().save(createTestEntity(3, "baz"));
        TestEntity entity = repository().findByUniqueField("name", "foo").get();
        assertEquals(1, entity.getId().longValue(), "Wrong id for 'foo' entity: ");
        assertEquals("foo", entity.getName(), "Wrong name for 'foo' entity: ");
        entity = repository().findByUniqueField("name", "bar").get();
        assertEquals(2, entity.getId().longValue(), "Wrong id for 'bar' entity: ");
        assertEquals("bar", entity.getName(), "Wrong name for 'bar' entity: ");
        entity = repository().findByUniqueField("name", "baz").get();
        assertEquals(3, entity.getId().longValue(), "Wrong id for 'baz' entity: ");
        assertEquals("baz", entity.getName(), "Wrong name for 'baz' entity: ");
    }

    @Test
    public void findByUniqueFieldNotFound() {
        repository().save(createTestEntity(1, "foo"));
        assertFalse(repository().findByUniqueField("name", "bar").isPresent(), "'bar' entity should not be present");
    }

    @Test
    public void findByUniqueFieldNotUnique() {
        repository().save(createTestEntity(1, "foo"));
        repository().save(createTestEntity(2, "foo"));
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().findByUniqueField("name", "foo"));
        assertEquals("Field name is not unique, found 2 entities: [foo, foo]", e.getMessage());
    }

    @Test
    public void countEmpty() {
        assertEquals(0, repository().count(), "Empty repository should have count 0");
    }

    @Test
    public void count() {
        repository().save(createTestEntity(1, "foo"));
        assertEquals(1, repository().count());
        repository().save(createTestEntity(2, "bar"));
        assertEquals(2, repository().count());
    }

    @Test
    public void deleteByIdNullId() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().deleteById((Integer) null));
        assertEquals("id must not be null", e.getMessage());
    }

    @Test
    public void deleteById() {
        assertEquals(0, repository().count());
        repository().save(createTestEntity(1, "foo"));
        assertEquals(1, repository().count());
        repository().deleteById(1);
        assertEquals(0, repository().count());
    }

    @Test
    public void deleteByIdNonExistingId() {
        Exception e = assertThrows(EmptyResultDataAccessException.class, () -> repository().deleteById(-1));
        assertEquals("Entity with id -1 not found", e.getMessage());
    }

    @Test
    public void deleteAllNullEntities() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().deleteAll((Iterable<TestEntity>) null));
        assertEquals("entitiesToDelete must not be null", e.getMessage());
    }

    @Test
    public void deleteAllEntities() {
        TestEntity foo = createTestEntity(1, "foo");
        TestEntity bar = createTestEntity(2, "bar");
        TestEntity baz = createTestEntity(3, "baz");
        repository().saveAll(Arrays.asList(new TestEntity[] { foo, bar, baz }));
        assertEquals(3, repository().count());
        repository().deleteAll(Arrays.asList(new TestEntity[] { bar }));
        assertEquals(2, repository().count());
        repository().deleteAll(Arrays.asList(new TestEntity[] { foo, baz }));
        assertEquals(0, repository().count());
        repository().deleteAll(Arrays.asList(new TestEntity[] { bar }));
        assertEquals(0, repository().count());
    }

    @Test
    public void deleteNullEntity() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().delete((TestEntity) null));
        assertEquals("entity must not be null", e.getMessage());
    }

    @Test
    public void deleteEntityWithNullId() {
        TestEntity entity = createTestEntity(null, "foo");
        // Check for exception
        repository().delete(entity);
    }

    @Test
    public void deleteAllEmpty() {
        // Check for no exception
        repository().deleteAll();
    }

    @Test
    public void deleteAll() {
        repository().save(createTestEntity(1, "foo"));
        repository().save(createTestEntity(2, "bar"));
        assertEquals(2, repository().count());
        repository().deleteAll();
        assertEquals(0, repository().count());
    }

    @Test
    public void deleteAllById() {
        repository().saveAll(Arrays.asList(createTestEntity(1, "foo"), createTestEntity(2, "bar"), createTestEntity(3, "baz")));
        repository().deleteAllById(Arrays.asList(1, 3));
        assertEquals(1, repository().count());
        assertTrue(repository().existsById(2), "Entity 2 should be found");
    }

    @Test
    public void existsByIdNullId() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().existsById((Integer) null));
        assertEquals("id must not be null", e.getMessage());
    }

    @Test
    public void existsById() {
        assertFalse(repository().existsById(1), "Nothing should be found");
        repository().save(createTestEntity(1, "foo"));
        assertTrue(repository().existsById(1), "Entity 1 should be found");
        assertFalse(repository().existsById(2), "Entity 2 should not be found");
    }

    @Test
    public void findAll() {
        TestEntity foo = createTestEntity(1, "foo");
        TestEntity bar = createTestEntity(2, "bar");
        repository().save(foo);
        repository().save(bar);
        Iterable<TestEntity> entities = repository().findAll();
        assertTrue(find(foo, entities), "findAll should contain entity foo");
        assertTrue(find(bar, entities), "findAll should contain entity bar");
    }

    @Test
    public void findAllByIdNullIds() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().findAllById((Iterable<Integer>) null));
        assertEquals("ids must not be null", e.getMessage());
    }

    @Test
    public void findAllByIdNoIds() {
        TestEntity foo = createTestEntity(1, "foo");
        TestEntity bar = createTestEntity(2, "bar");
        TestEntity baz = createTestEntity(3, "baz");
        repository().save(foo);
        repository().save(bar);
        repository().save(baz);
        Iterable<TestEntity> entities = repository().findAllById(Arrays.asList(new Integer[] {}));
        assertEquals(0, size(entities));
    }

    @Test
    public void findAllById() {
        TestEntity foo = createTestEntity(1, "foo");
        TestEntity bar = createTestEntity(2, "bar");
        TestEntity baz = createTestEntity(3, "baz");
        repository().save(foo);
        repository().save(bar);
        repository().save(baz);
        Iterable<TestEntity> entities = repository().findAllById(Arrays.asList(new Integer[] { 1, 2 }));
        assertTrue(find(foo, entities), "findAll should contain entity foo");
        assertTrue(find(bar, entities), "findAll should contain entity bar");
        assertFalse(find(baz, entities), "findAll should not contain entity baz");
    }

    @Test
    public void findAllByIdNotFound() {
        TestEntity foo = createTestEntity(1, "foo");
        repository().save(foo);
        Iterable<TestEntity> entities = repository().findAllById(Arrays.asList(new Integer[] { -1, -2 }));
        assertFalse(find(foo, entities), "findAll should not contain entity foo");
    }

    @Test
    public void findByIdNullId() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().findById((Integer) null));
        assertEquals("id must not be null", e.getMessage());
    }

    @Test
    public void findById() {
        TestEntity foo = createTestEntity(1, "foo");
        TestEntity bar = createTestEntity(2, "bar");
        repository().save(foo);
        repository().save(bar);
        assertEquals(foo, repository().findById(1).get(), "Entity 1 should be found");
        assertEquals(bar, repository().findById(2).get(), "Entity 2 should be found");
        assertFalse(repository().findById(3).isPresent(), "Entity 3 should not be found");
    }

    @Test
    public void saveNullEntity() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().save((TestEntity) null));
        assertEquals("entity must not be null", e.getMessage());
    }

    @Test
    public void saveAllNullEntities() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> repository().saveAll((Iterable<TestEntity>) null));
        assertEquals("entitiesToSave must not be null", e.getMessage());
    }

    @Test
    public void saveNullPrimaryKeyWithNoGenerator() {
        Exception e = assertThrows(IllegalStateException.class, () -> repository().save(createTestEntity(null, "foo")));
        assertEquals("Primary key is null and no primary key generator available: entity=foo", e.getMessage());
    }

    @Test
    public void saveAll() {
        TestEntity foo = createTestEntity(1, "foo");
        TestEntity bar = createTestEntity(2, "bar");
        TestEntity baz = createTestEntity(3, "baz");
        repository().save(foo);
        assertEquals(1, repository().count());
        List<TestEntity> entities = new ArrayList<>();
        repository().saveAll(entities);
        assertEquals(1, repository().count());
        entities.add(bar);
        entities.add(baz);
        repository().saveAll(entities);
        assertEquals(3, repository().count());
        assertEquals(foo, repository().findById(1).get(), "Entity 1 should be found");
        assertEquals(bar, repository().findById(2).get(), "Entity 2 should be found");
        assertEquals(baz, repository().findById(3).get(), "Entity 3 should be found");
    }

    @Test
    public void save() {
        TestEntity foo = createTestEntity(1, "foo");
        repository().save(foo);
        TestEntity newFoo = createTestEntity(1, "frotz");
        newFoo = repository().save(newFoo);
        assertEquals(1, repository().count());
        assertEquals(newFoo, repository().findById(1).get(), "Entity frotz should be found");
        assertEquals("frotz", repository().findById(1).get().getName(), "Entity frotz has wrong name: ");
        assertFalse(find(foo, repository().findAll()), "Entity foo should not be found");
    }

    @Test
    public void entityWithNoIdAnnotation() {
        InMemoryJpaRepository<String, Integer> repo = new InMemoryJpaRepository<>();
        Exception e = assertThrows(IllegalArgumentException.class, () -> repo.save("foo"));
        assertEquals("Entity has no @Id annotation: foo", e.getMessage());
    }

    @Test
    public void findAllPageable() {
        repository().save(createTestEntity(1, "foo"));
        repository().save(createTestEntity(2, "bar"));
        repository().save(createTestEntity(3, "baz"));
        // First page
        Page<TestEntity> page = repository().findAll(PageRequest.of(0, 2));
        assertEquals(2, page.getContent().size(), "Unexpected number of entities in page: ");
        assertEquals(0, page.getNumber(), "Unexpected page number: ");
        assertEquals(2, page.getSize(), "Unexptected page size: ");
        assertEquals(2, page.getTotalPages(), "Unexpected total pages: ");
        assertEquals(3, page.getTotalElements(), "Unexpected total elements: ");
        assertTrue(page.isFirst(), "Page should be first");
        assertFalse(page.isLast(), "Page should not be last");
        // Second page
        page = repository().findAll(PageRequest.of(1, 2));
        assertEquals(1, page.getContent().size(), "Unexpected number of entities in page: ");
        assertEquals(1, page.getNumber(), "Unexpected page number: ");
        assertEquals(2, page.getSize(), "Unexptected page size: ");
        assertEquals(2, page.getTotalPages(), "Unexpected total pages: ");
        assertEquals(3, page.getTotalElements(), "Unexpected total elements: ");
        assertFalse(page.isFirst(), "Page should not be first");
        assertTrue(page.isLast(), "Page should be last");
        // Third page
        page = repository().findAll(PageRequest.of(2, 2));
        assertEquals(0, page.getContent().size(), "Unexpected number of entities in page: ");
        assertEquals(2, page.getNumber(), "Unexpected page number: ");
        assertEquals(2, page.getSize(), "Unexptected page size: ");
        assertEquals(2, page.getTotalPages(), "Unexpected total pages: ");
        assertEquals(3, page.getTotalElements(), "Unexpected total elements: ");
        assertFalse(page.isFirst(), "Page should not be first");
        assertTrue(page.isLast(), "Page should be last");
    }

    @Test
    public void findAllPageableWithSorting() {
        repository().save(createTestEntity(1, "foo"));
        repository().save(createTestEntity(2, "bar"));
        repository().save(createTestEntity(3, "baz"));
        Page<TestEntity> page = repository().findAll(PageRequest.of(0, 2).withSort(Sort.by("name")));
        assertEquals(2, page.getContent().size(), "Unexpected number of entities in page: ");
        assertEquals(2, page.getContent().get(0).getId().intValue(), "Unexpected ID in sorted page 0:0: ");
        assertEquals(3, page.getContent().get(1).getId().intValue(), "Unexpected ID in sorted page 0:1: ");
        page = repository().findAll(PageRequest.of(1, 2).withSort(Sort.by("name")));
        assertEquals(1, page.getContent().size(), "Unexpected number of entities in page: ");
        assertEquals(1, page.getContent().get(0).getId().intValue(), "Unexpected ID in sorted page 1:0: ");
    }

    @Test
    public void findAllSort() {
        repository().save(createTestEntity(1, "foo"));
        repository().save(createTestEntity(2, "bar"));
        repository().save(createTestEntity(3, "foo"));
        List<TestEntity> sortedEntities = repository().findAll(Sort.by("name", "id").descending());
        assertEquals(3, sortedEntities.get(0).getId().intValue(), "Unexpected ID in sorted entity 0: ");
        assertEquals(1, sortedEntities.get(1).getId().intValue(), "Unexpected ID in sorted entity 1: ");
        assertEquals(2, sortedEntities.get(2).getId().intValue(), "Unexpected ID in sorted entity 2: ");
    }

    @Test
    public void countByExample() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository().count((Example<TestEntity>) null));
        assertEquals("Not yet implemented", e.getMessage());
    }

    @Test
    public void existsByExample() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository().exists((Example<TestEntity>) null));
        assertEquals("Not yet implemented", e.getMessage());
    }

    @Test
    public void findAllByExample() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository().findAll((Example<TestEntity>) null));
        assertEquals("Not yet implemented", e.getMessage());
    }

    @Test
    public void findAllByExampleAndPageable() {
        Exception e = assertThrows(UnsupportedOperationException.class,
                () -> repository().findAll((Example<TestEntity>) null, (Pageable) null));
        assertEquals("Not yet implemented", e.getMessage());
    }

    @Test
    public void findAllByExampleAndSort() {
        Exception e = assertThrows(UnsupportedOperationException.class,
                () -> repository().findAll((Example<TestEntity>) null, (Sort) null));
        assertEquals("Not yet implemented", e.getMessage());
    }

    @Test
    public void findOneByExample() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository().findOne((Example<TestEntity>) null));
        assertEquals("Not yet implemented", e.getMessage());
    }

    protected boolean find(Object o, Iterable<?> objects) {
        for (Object obj : objects) {
            if (obj.equals(o)) {
                return true;
            }
        }
        return false;
    }

    protected int size(Iterable<?> objects) {
        int size = 0;
        for (@SuppressWarnings("unused")
        Object obj : objects) {
            size++;
        }
        return size;
    }

    public interface TestEntity {
        Integer getId();

        String getName();
    }
}
