package com.reallifedeveloper.tools.test.database.inmemory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * An implementation of the Spring Data JPA {@code JpaRepository} interface that holds entities in a map. Useful for testing.
 *
 * @param <T>  the type of the entities handled by this repository
 * @param <ID> the type of the entities' primary keys
 *
 * @author RealLifeDeveloper
 */
public class InMemoryJpaRepository<T, ID extends Comparable<ID>> extends AbstractInMemoryCrudRepository<T, ID>
        implements JpaRepository<T, ID> {

    /**
     * Creates a new {@code InMemoryJpaRepository} with no primary key generator. If an entity with a {@code null} primary key is saved, an
     * exception is thrown.
     */
    public InMemoryJpaRepository() {
        super();
    }

    /**
     * Creates a new {@code InMemoryJpaRepository} with the provided primary key generator. If an entity with a {@code null} primary key is
     * saved, the generator is used to create a new primary key that is stored in the entity before saving.
     *
     * @param primaryKeyGenerator the primary key generator to use
     */
    public InMemoryJpaRepository(PrimaryKeyGenerator<ID> primaryKeyGenerator) {
        super(primaryKeyGenerator);
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
    public void deleteAllInBatch(Iterable<T> entities) {
        deleteAll(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAllByIdInBatch(Iterable<ID> ids) {
        deleteAllById(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        // Do nothing.
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
    public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
        List<S> savedEntities = saveAll(entities);
        flush();
        return savedEntities;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public T getById(ID id) {
        Optional<T> optionalElement = findById(id);
        return optionalElement.orElseThrow(() -> new EntityNotFoundException("Entity with ID " + id + " not found"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public T getOne(ID id) {
        return getById(id);
    }

    @Override
    protected boolean isIdField(Field field) {
        return field.getAnnotation(Id.class) != null || field.getAnnotation(EmbeddedId.class) != null;
    }

    @Override
    protected boolean isIdMethod(Method method) {
        return method.getAnnotation(Id.class) != null || method.getAnnotation(EmbeddedId.class) != null;
    }

    @Override
    protected Optional<Class<ID>> getIdClass(Object entity) {
        IdClass idClass = entity.getClass().getAnnotation(IdClass.class);
        return Optional.ofNullable(idClass).map(IdClass::value);
    }

    @Override
    public <S extends T, R> R findBy(Example<S> example, Function<FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException("Unimplemented method 'findBy'");
    }

    @Override
    public T getReferenceById(ID id) {
        throw new UnsupportedOperationException("Unimplemented method 'getReferenceById'");
    }

}
