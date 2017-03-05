package com.reallifedeveloper.tools.test.database.inmemory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * An implementation of the Spring Data JPA <code>JpaRepository</code> interface that
 * holds entities in a map. Useful for testing.
 *
 * @author RealLifeDeveloper
 *
 * @param <T> the type of the entities handled by this repository
 * @param <ID> the type of the entities' primary keys
 */
public class InMemoryJpaRepository<T, ID extends Serializable & Comparable<ID>> implements JpaRepository<T, ID> {

    private final Map<ID, T> entities = new HashMap<>();

    private final PrimaryKeyGenerator<ID> primaryKeyGenerator;

    /**
     * Creates a new <code>InMemoryJpaRepository</code> with no primary key generator.
     * If an entity with a <code>null</code> primary key is saved, an exception
     * is thrown.
     */
    public InMemoryJpaRepository() {
        this.primaryKeyGenerator = null;
    }

    /**
     * Creates a new <code>InMemoryJpaRepository</code> with the provided primary key
     * generator. If an entity with a <code>null</code> primary key is saved, the
     * generator is used to create a new primary key that is stored in the entity
     * before saving.
     *
     * @param primaryKeyGenerator the primary key generator to use
     */
    public InMemoryJpaRepository(PrimaryKeyGenerator<ID> primaryKeyGenerator) {
        this.primaryKeyGenerator = primaryKeyGenerator;
    }

    /**
     * Finds entities with a field matching a value.
     *
     * @param fieldName the name of the field to use when searching
     * @param value the value to search for
     * @param <F> the type of <code>value</code>
     * @return a list of entities <code>e</code> such that <code>value.equals(e.fieldName)</code>
     *
     * @throws IllegalArgumentException if <code>fieldName</code> is <code>null</code>
     */
    protected <F> List<T> findByField(String fieldName, F value) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName must not be null");
        }
        List<T> foundEntities = new ArrayList<>();
        for (T entity : entities.values()) {
            Field field;
            try {
                field = getField(entity, fieldName);
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        field.setAccessible(true);
                        return null;
                    }
                });
                if (haveSameValue(value, field.get(entity))) {
                    foundEntities.add(entity);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException("Field not found or not accessible: " + fieldName);
            }
        }
        return foundEntities;
    }

    private static boolean haveSameValue(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            return o1.equals(o2);
        }
    }

    private Field getField(Object entity, String fieldName) throws NoSuchFieldException {
        Class<?> entityType = entity.getClass();
        while (entityType != null) {
            for (Field field : entityType.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    return field;
                }
            }
            entityType = entityType.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**
     * Finds a unique entity with a field matching a value.
     *
     * @param fieldName the name of the field to use when searching
     * @param value the value to search for
     * @param <F> the type of <code>value</code>
     * @return the unique entity <code>e</code> such that <code>value.equals(e.fieldName)</code>,
     * or <code>null</code> if no such entity is found
     * @throws IllegalArgumentException if either argument is <code>null</code>, or if more
     * than one entity with the given value is found
     */
    protected <F> T findByUniqueField(String fieldName, F value) {
        List<T> foundEntities = findByField(fieldName, value);
        if (foundEntities.isEmpty()) {
            return null;
        } else if (foundEntities.size() == 1) {
            return foundEntities.get(0);
        } else {
            throw new IllegalArgumentException("Field " + fieldName + " is not unique, found "
                    + foundEntities.size() + " entities: " + foundEntities);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count() {
        return entities.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        T removedEntity = entities.remove(id);
        if (removedEntity == null) {
            throw new EmptyResultDataAccessException("Entity with id " + id + " not found", 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Iterable<? extends T> entitiesToDelete) {
        if (entitiesToDelete == null) {
            throw new IllegalArgumentException("entitiesToDelete must not be null");
        }
        for (T entity : entitiesToDelete) {
            delete(entity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        entities.remove(getId(entity));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll() {
        entities.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return entities.containsKey(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findAll(Iterable<ID> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("ids must not be null");
        }
        List<T> selectedEntities = new ArrayList<T>();
        for (ID id : ids) {
            T selectedEntity = findOne(id);
            if (selectedEntity != null) {
                selectedEntities.add(selectedEntity);
            }
        }
        return selectedEntities;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not yet implemented, so it always throws an exception.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public Page<T> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInBatch(Iterable<T> entitiesToDelete) {
        delete(entitiesToDelete);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not yet implemented, so it always throws an exception.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<T> findAll(Sort sort) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends T> S saveAndFlush(S entity) {
        S savedEntity = save(entity);
        flush();
        return savedEntity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T findOne(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return entities.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getOne(ID id) {
        return findOne(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends T> List<S> save(Iterable<S> entitiesToSave) {
        if (entitiesToSave == null) {
            throw new IllegalArgumentException("entitiesToSave must not be null");
        }
        List<S> savedEntities = new ArrayList<>();
        for (S entity : entitiesToSave) {
            savedEntities.add(save(entity));
        }
        return savedEntities;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends T> S save(S entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        ID id = getId(entity);
        if (id == null) {
            if (primaryKeyGenerator != null) {
                id = primaryKeyGenerator.nextPrimaryKey(maximumPrimaryKey());
                setId(entity, id);
            } else {
                throw new IllegalStateException("Primary key is null: " + entity);
            }
        }
        entities.put(id, entity);
        return entity;
    }

    private ID maximumPrimaryKey() {
        ID max = null;
        for (T entity : findAll()) {
            ID id = getId(entity);
            if (max == null || id.compareTo(max) > 0) {
                max = id;
            }
        }
        return max;
    }

    @SuppressWarnings("unchecked")
    private ID getId(T entity) {
        ID id = null;
        try {
            if (getIdField(entity) != null) {
                id = (ID) getIdField(entity).get(entity);
            } else if (getIdMethod(entity) != null) {
                id = (ID) getIdMethod(entity).invoke(entity);
            } else {
                throw new IllegalArgumentException("Entity has no @Id annontation: " + entity);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return id;
    }

    private void setId(T entity, ID id) {
        try {
            if (getIdField(entity) != null) {
                getIdField(entity).set(entity, id);
            } else if (getIdMethod(entity) != null) {
                Method setMethod = getSetMethod(entity, id);
                setMethod.invoke(entity, id);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Field getIdField(T entity) {
        Class<?> c = entity.getClass();
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getAnnotation(Id.class) != null || field.getAnnotation(EmbeddedId.class) != null) {
                    field.setAccessible(true);
                    return field;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private Method getIdMethod(T entity) {
        Class<?> c = entity.getClass();
        while (c != null) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.getAnnotation(Id.class) != null || method.getAnnotation(EmbeddedId.class) != null) {
                    method.setAccessible(true);
                    return method;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private Method getSetMethod(T entity, ID id) throws NoSuchMethodException {
        Method getMethod = getIdMethod(entity);
        String setMethodName = getMethod.getName().replaceFirst("^get", "set");
        Method setMethod = entity.getClass().getMethod(setMethodName, id.getClass());
        setMethod.setAccessible(true);
        return setMethod;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{entities=" + entities + "}";
    }

}

