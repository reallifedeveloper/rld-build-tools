package com.reallifedeveloper.tools.test.database.inmemory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import com.reallifedeveloper.tools.test.TestUtil;

/**
 * An abstract helper class that implements the {@link CrudRepository} interface using an in-memory map instead of a database.
 * <p>
 * Contains useful methods for sub-classes implementing in-memory versions of repositories, such as {@link #findByField(String, Object)} and
 * {@link #findByUniqueField(String, Object)}.
 *
 * @param <T>  the type of the entities handled by this repository
 * @param <ID> the type of the entities' primary keys
 *
 * @author RealLifeDeveloper
 */
@SuppressWarnings({ "PMD", "checkstyle:noReturnNull" }) // TODO: Consider refactoring this class using the hints from PMD
public abstract class AbstractInMemoryCrudRepository<T, ID extends Comparable<ID>>
        implements CrudRepository<T, ID>, PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T> {

    private final Map<@NonNull ID, @NonNull T> entities = new HashMap<>();

    private final @Nullable PrimaryKeyGenerator<ID> primaryKeyGenerator;

    /**
     * Creates a new {@code InMemoryCrudRepository} with no primary key generator. If an entity with a {@code null} primary key is saved, an
     * exception is thrown.
     */
    public AbstractInMemoryCrudRepository() {
        this.primaryKeyGenerator = null;
    }

    /**
     * Creates a new {@code InMemoryCrudRepository} with the provided primary key generator. If an entity with a {@code null} primary key is
     * saved, the generator is used to create a new primary key that is stored in the entity before saving.
     *
     * @param primaryKeyGenerator the primary key generator to use, must not be {@code null}
     */
    public AbstractInMemoryCrudRepository(PrimaryKeyGenerator<ID> primaryKeyGenerator) {
        if (primaryKeyGenerator == null) {
            throw new IllegalArgumentException("primaryKeyGenerator must not be null");
        }
        this.primaryKeyGenerator = primaryKeyGenerator;
    }

    /**
     * Finds entities with a field matching a value.
     *
     * @param fieldName the name of the field to use when searching
     * @param value     the value to search for
     * @param <F>       the type of {@code value}
     * @return a list of entities {@code e} such that {@code value.equals(e.fieldName)}
     *
     * @throws IllegalArgumentException if {@code fieldName} is {@code null}
     */
    protected <F> List<@NonNull T> findByField(String fieldName, F value) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName must not be null");
        }
        return entities.values().stream().filter(entity -> Objects.equals(value, TestUtil.getFieldValue(entity, fieldName))).toList();
    }

    /**
     * Finds a unique entity with a field matching a value.
     *
     * @param fieldName the name of the field to use when searching
     * @param value     the value to search for
     * @param <F>       the type of {@code value}
     *
     * @return the unique entity {@code e} such that {@code value.equals(e.fieldName)}, or {@code null} if no such entity is found
     *
     * @throws IllegalArgumentException if either argument is {@code null}, or if more than one entity with the given value is found
     */
    protected <F> Optional<T> findByUniqueField(String fieldName, F value) {
        List<T> foundEntities = findByField(fieldName, value);
        if (foundEntities.isEmpty()) {
            return Optional.empty();
        } else if (foundEntities.size() == 1) {
            return Optional.of(foundEntities.get(0));
        } else {
            throw new IllegalArgumentException(
                    "Field " + fieldName + " is not unique, found " + foundEntities.size() + " entities: " + foundEntities);
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
    public void deleteById(ID id) {
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
    public void deleteAll(Iterable<? extends T> entitiesToDelete) {
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
    public void deleteAllById(Iterable<? extends ID> ids) {
        for (ID id : ids) {
            deleteById(id);
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
    public boolean existsById(ID id) {
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
    public List<T> findAllById(Iterable<ID> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("ids must not be null");
        }
        List<T> selectedEntities = new ArrayList<T>();
        for (ID id : ids) {
            Optional<T> optionalEntity = findById(id);
            if (optionalEntity.isPresent()) {
                selectedEntities.add(optionalEntity.get());
            }
        }
        return selectedEntities;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        T item = entities.get(id);
        return Optional.ofNullable(item);
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
                throw new IllegalStateException("Primary key is null and no primary key generator available: entity=" + entity);
            }
        }
        entities.put(id, entity);
        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entitiesToSave) {
        if (entitiesToSave == null) {
            throw new IllegalArgumentException("entitiesToSave must not be null");
        }
        List<S> savedEntities = new ArrayList<>();
        for (S entity : entitiesToSave) {
            savedEntities.add(save(entity));
        }
        return savedEntities;
    }

    //
    // PagingAndSortingRepository methods
    //

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findAll(Sort sort) {
        return SortUtil.sort(findAll(), sort);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<T> findAll(Pageable pageable) {
        List<T> allEntities = SortUtil.sort(findAll(), pageable.getSort());
        int start = (int) pageable.getOffset();
        int end = (start + pageable.getPageSize()) > allEntities.size() ? allEntities.size() : (start + pageable.getPageSize());
        List<T> pagedEntities = start <= end ? allEntities.subList(start, end) : Collections.emptyList();
        Page<T> page = new PageImpl<>(pagedEntities, pageable, allEntities.size());
        return page;
    }

    //
    // Helper methods
    //

    /**
     * Gives the value of the largest primary key in an entity currently in the repository.
     * <p>
     * This method returns {@code null} if there are no entities in the repository. Since the save methods guarantee that
     *
     * @return the largest primary key in the repository, or {@code null} if the repository is empty
     */
    private @Nullable ID maximumPrimaryKey() {
        ID max = null;
        for (T entity : findAll()) {
            ID id = getId(entity);
            if (max == null || (id != null && id.compareTo(max) > 0)) {
                max = id;
            }
        }
        return max;
    }

    /**
     * Gives the value of the ID field or method of the given entity.
     *
     * @param entity the entity to examine, should not be {@code null}
     *
     * @return the value of the ID field or method of {@code entity}, may be {@code null}
     */
    @SuppressWarnings("unchecked")
    protected @Nullable ID getId(T entity) {
        ID id = null;
        try {
            if (getCompositeIdClass(entity).isPresent()) {
                // TODO: Handle IdClass with method annotations
                List<Field> idFields = getIdFields(entity);
                id = createIdClassInstance(entity, idFields);
            } else if (getIdField(entity).isPresent()) {
                id = (ID) getIdField(entity).get().get(entity);
            } else if (getIdMethod(entity).isPresent()) {
                id = (ID) getIdMethod(entity).get().invoke(entity);
            } else {
                throw new IllegalArgumentException("Entity has no @Id annotation: " + entity);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return id;
    }

    /**
     * Sets the value of the ID field, or calls the ID setter method, for the given entity.
     *
     * @param entity the entity for which to set the ID
     * @param id     the new ID value
     */
    protected void setId(T entity, @Nullable ID id) {
        try {
            if (getCompositeIdClass(entity).isPresent()) {
                // TODO: Handle IdClass with method annotations
                List<Field> idFields = getIdFields(entity);
                if (id == null) {
                    for (Field idField : idFields) {
                        idField.set(entity, null);
                    }
                } else {
                    setIdFieldsFromIdClassInstance(entity, idFields, id);
                }
            } else if (getIdField(entity).isPresent()) {
                getIdField(entity).get().set(entity, id);
            } else if (getIdMethod(entity).isPresent()) {
                @SuppressWarnings("unchecked")
                Class<ID> idClass = (Class<ID>) getIdMethod(entity).get().getReturnType();
                Method setIdMethod = getSetMethod(entity, idClass);
                setIdMethod.invoke(entity, id);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private Optional<Field> getIdField(T entity) {
        List<Field> idFields = getIdFields(entity);
        if (idFields.isEmpty()) {
            return Optional.empty();
        } else if (idFields.size() > 1) {
            throw new IllegalStateException("Multiptle ID fields found in entity: " + entity);
        } else {
            return Optional.of(idFields.get(0));
        }
    }

    private List<Field> getIdFields(T entity) {
        List<Field> idFields = new ArrayList<>();
        Class<?> c = entity.getClass();
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (isIdField(field)) {
                    field.setAccessible(true);
                    idFields.add(field);
                }
            }
            c = c.getSuperclass();
        }
        return idFields;
    }

    private Optional<Method> getIdMethod(T entity) {
        List<Method> idMethods = getIdMethods(entity);
        if (idMethods.isEmpty()) {
            return Optional.empty();
        } else if (idMethods.size() > 1) {
            throw new IllegalStateException("Multiptle ID methods found in entity: " + entity);
        } else {
            return Optional.of(idMethods.get(0));
        }
    }

    private List<Method> getIdMethods(T entity) {
        List<Method> idMethods = new ArrayList<>();
        Class<?> c = entity.getClass();
        while (c != null) {
            for (Method method : c.getDeclaredMethods()) {
                if (isIdMethod(method)) {
                    method.setAccessible(true);
                    idMethods.add(method);
                }
            }
            c = c.getSuperclass();
        }
        return idMethods;
    }

    private Method getSetMethod(T entity, Class<ID> idClass) throws NoSuchMethodException {
        Method getMethod = getIdMethod(entity)
                .orElseThrow(() -> new NoSuchMethodException("Get method for ID not found: entity=" + entity));
        String setMethodName = getMethod.getName().replaceFirst("^get", "set");
        Method setMethod = entity.getClass().getMethod(setMethodName, idClass);
        setMethod.setAccessible(true);
        return setMethod;
    }

    private @Nullable ID createIdClassInstance(T entity, List<Field> idFields) throws ReflectiveOperationException {
        Class<ID> idClass = getCompositeIdClass(entity)
                .orElseThrow(() -> new IllegalStateException("Composibte ID class not found: entity=" + entity));
        Constructor<ID> constructor = idClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        ID id = constructor.newInstance();
        for (Field idField : idFields) {
            if (idField.get(entity) == null) {
                // If any of the ID fields is null, we say that the primary key is null.
                return null;
            }
            TestUtil.injectField(id, idField.getName(), idField.get(entity));
        }
        return id;
    }

    private void setIdFieldsFromIdClassInstance(T entity, List<Field> idFields, ID id) {
        for (Field idField : idFields) {
            Object value = TestUtil.getFieldValue(id, idField.getName());
            TestUtil.injectField(entity, idField.getName(), value);
        }
    }

    /**
     * Override this in a concrete subclass to decide if a given field is an ID field of an entity.
     *
     * @param field the field to examine
     *
     * @return {@code true} if {@code field} is an ID field, {@code false} otherwise
     */
    protected abstract boolean isIdField(Field field);

    /**
     * Override this in a concrete subclass to decide if a given method is a method giving the ID of an entity.
     *
     * @param method the method to examine
     *
     * @return {@code true} if {@code method} is an ID method, {@code false} otherwise
     */
    protected abstract boolean isIdMethod(Method method);

    /**
     * Override this in concrete subclass to give the ID class representing a composite primary key for an entity, if any.
     *
     * @param entity the entity to examine
     *
     * @return the ID class representing the composite primary key of {@code entity}, or an empty optional if there is no such class
     */
    protected abstract Optional<Class<ID>> getCompositeIdClass(Object entity);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{entities=" + entities + "}";
    }

    //
    // QueryByExampleExecutor methods
    //

    /**
     * {@inheritDoc}
     * <p>
     * This method is not yet implemented, so it always throws an exception.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public <S extends T> Optional<S> findOne(Example<S> example) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not yet implemented, so it always throws an exception.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public <S extends T> List<S> findAll(Example<S> example) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not yet implemented, so it always throws an exception.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not yet implemented, so it always throws an exception.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not yet implemented, so it always throws an exception.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public <S extends T> long count(Example<S> example) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not yet implemented, so it always throws an exception.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public <S extends T> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Make finalize method final to avoid "Finalizer attacks" and corresponding SpotBugs warning (CT_CONSTRUCTOR_THROW).
     *
     * @see <a href="https://wiki.sei.cmu.edu/confluence/display/java/OBJ11-J.+Be+wary+of+letting+constructors+throw+exceptions">
     *      Explanation of finalizer attack</a>
     */
    @Override
    @SuppressWarnings({ "deprecation", "removal", "Finalize", "checkstyle:NoFinalizer" })
    protected final void finalize() throws Throwable {
        // Do nothing
    }

}
