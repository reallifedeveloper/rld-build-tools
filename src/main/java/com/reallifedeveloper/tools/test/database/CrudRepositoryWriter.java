package com.reallifedeveloper.tools.test.database;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;

import com.reallifedeveloper.tools.test.TestUtil;

/**
 * A helper class to write data into a {@link CrudRepository} from some data source, e.g., a CSV file, where each entity is represented by a
 * {@link DbTableRow}.
 * <p>
 * This can be useful for inserting test data into a repository, irrespective of whether the repository connects to a real database or not.
 * <p>
 * TODO: The current implementation only has basic support for "to many" associations (there must be a &amp;JoinTable annotation on a field,
 * with &amp;JoinColumn annotations), and for enums (an enum must be stored as a string).
 *
 * @author RealLifeDeveloper
 */
@Getter
@SuppressWarnings("PMD")
@SuppressFBWarnings(value = { "CRLF_INJECTION_LOGS", "IMPROPER_UNICODE" })
public class CrudRepositoryWriter {

    private static final Logger LOG = LoggerFactory.getLogger(CrudRepositoryWriter.class);

    private final Set<Class<?>> classes = new HashSet<>();
    private final List<Object> entities = new ArrayList<>();

    /**
     * Creates a new entity based on data from a {@link DbTableRow} and writes it into a repository if appropriate.
     * <p>
     * This method may create entities that are not directly handled by the repository, in which case they are assumed to be related to some
     * entity in the repository.
     *
     * @param <T>                  the type of entities in the repository
     * @param <E>                  the type of entity being created
     * @param <ID>                 the type of the primary key of the entities in the repository
     * @param tableRow             the {@code TableRow} with the data to insert into the fields of the newly created entity
     * @param repositoryEntityType the class object representing {@code T}, i.e., the type ofrepository entities
     * @param entityType           the class object representing {@code E}, i.e., the type of entity being created, or {@code null}
     * @param repository           the repository in which to insert the newly created entity
     * @param tableName            the name of the database table where the entity should be stored
     * @return {@code true} if an entity was created, no matter if it was saved in the repository, {@code false} otherwise
     * @throws ReflectiveOperationException if some reflection operation failed creating the entity or setting is fields
     */
    public <T, E, ID extends Serializable> boolean writeEntity(DbTableRow tableRow, Class<T> repositoryEntityType,
            @Nullable Class<E> entityType, CrudRepository<T, ID> repository, String tableName) throws ReflectiveOperationException {
        if (entityType == null) {
            return false;
        }
        if (entityType.getAnnotation(Embeddable.class) != null) {
            return writeEmbeddable(tableRow, repositoryEntityType, entityType, repository, tableName);
        }
        if (entityType.getAnnotation(Entity.class) == null || !JpaUtil.getTableName(entityType).equalsIgnoreCase(tableName)) {
            return false;
        }
        E entity = createEntity(entityType);
        for (DbTableField column : tableRow.columns()) {
            String fieldName = JpaUtil.getFieldName(column.name(), entityType);
            setField(entity, fieldName, column.value());
        }
        LOG.debug("Saving entity {}", entity);
        entities.add(entity);
        classes.add(entity.getClass());
        if (entityType.equals(repositoryEntityType)) {
            T entityToSave = repositoryEntityType.cast(entity);
            repository.save(entityToSave);
        }
        return true;
    }

    @SuppressWarnings("UnusedVariable")
    private <T, E, ID extends Serializable> boolean writeEmbeddable(DbTableRow tableRow, Class<T> repositoryEntityType,
            @Nullable Class<E> entityType, CrudRepository<T, ID> repository, String tableName) {
        LOG.debug("Saving embeddable {}", tableRow);
        throw new UnsupportedOperationException("writeEmbeddable not yet implemented");
    }

    /**
     * Connects entities based on data in a join table.
     *
     * @param tableRow       the {@code TableRow} with the data for the join table
     * @param joinTtableName the name of the join table to use to connect entities
     */
    public void addEntitiesFromJoinTable(DbTableRow tableRow, String joinTtableName) {
        joinTableField(joinTtableName).ifPresent(joinTableField -> {
            joinTableField.setAccessible(true);
            ParameterizedType parameterizedType = (ParameterizedType) joinTableField.getGenericType();
            Class<?> targetType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            JoinTable joinTable = joinTableField.getAnnotation(JoinTable.class);
            assert joinTable != null : "JoinTable annotation should be present when the joinTableField method returns a non-empty value";
            for (JoinColumn joinColumn : joinTable.joinColumns()) {
                for (JoinColumn inverseJoinColumn : joinTable.inverseJoinColumns()) {
                    addEntityFromJoinTable(tableRow, joinTableField, targetType, joinColumn, inverseJoinColumn);
                }
            }
        });
    }

    /**
     * Goes through all entities that have been saved, trying to fix missing associations.
     *
     * @throws ReflectiveOperationException if something went wrong using reflection to analyze the entities
     */
    public void fillReferencesBetweenEntities() throws ReflectiveOperationException {
        for (Object entity : entities) {
            for (Field field : entity.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                if (oneToOne != null) {
                    handleOneToOne(entity, field, oneToOne);
                }
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                if (oneToMany != null) {
                    handleOneToMany(entity, field, oneToMany);
                }
            }
        }
    }

    private void handleOneToOne(Object entity, Field field, OneToOne oneToOne) throws IllegalAccessException, NoSuchFieldException {
        String mappedBy = oneToOne.mappedBy();
        if (mappedBy == null || mappedBy.isEmpty()) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null) {
                mappedBy = JpaUtil.getFieldName(joinColumn.name(), field.getType());
            }
        }
        if (mappedBy == null || mappedBy.isEmpty()) {
            throw new IllegalStateException("OneToOne field " + JpaUtil.fieldNameForLogging(entity, field)
                    + " has no mappedBy and no JoinColumn annotation");
        }
        Object id = JpaUtil.getIdValue(entity);
        List<?> entitiesToMap = findEntitiesByClassAndField(field.getType(), mappedBy, id);
        if (entitiesToMap.size() > 1) {
            throw new IllegalStateException("Found multiple candidates for OneToOne mapping: entity=" + entity + ", field={}" + field);
        }
        Object value = entitiesToMap.isEmpty() ? null : entitiesToMap.get(0);
        LOG.debug("Setting OneToOne field {} to {}", JpaUtil.fieldNameForLogging(entity, field), value);
        field.set(entity, value);
    }

    private void handleOneToMany(Object entity, Field field, OneToMany oneToMany) throws IllegalAccessException, NoSuchFieldException {
        Class<?> collectionType = field.getType();
        if (Collection.class.isAssignableFrom(collectionType)) {
            saveEntityInCollection(entity, field, oneToMany);
        } else if (Map.class.isAssignableFrom(collectionType)) {
            saveEntityInMap(entity, field, oneToMany);
        }
    }

    private void saveEntityInCollection(Object entity, Field field, OneToMany oneToMany)
            throws IllegalAccessException, NoSuchFieldException {
        LOG.trace("saveEntityInCollection: entity={}, field={}, oneToMany={}", entity, field, oneToMany);
        LOG.trace("Not yet implemented");
    }

    private void saveEntityInMap(Object entity, Field field, OneToMany oneToMany) throws IllegalAccessException, NoSuchFieldException {
        LOG.trace("saveEntityInMap: entity={}, field={}, oneToMany={}", entity, field, oneToMany);
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Type[] targetTypes = parameterizedType.getActualTypeArguments();
        Class<?> targetClass = getClass(targetTypes[1].getTypeName());
        String mappedBy = oneToMany.mappedBy();
        if (mappedBy == null || mappedBy.isEmpty()) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null) {
                mappedBy = JpaUtil.getFieldName(joinColumn.name(), targetClass);
            }
        }
        if (mappedBy == null || mappedBy.isEmpty()) {
            throw new IllegalStateException("OneToMany field " + JpaUtil.fieldNameForLogging(entity, field)
                    + " has no mappedBy and no JoinColumn annotation");
        }
        Object id = JpaUtil.getIdValue(entity);
        List<?> entitiesToMap = findEntitiesByClassAndField(targetClass, mappedBy, id);
        JpaUtil.addEntitiesToMapField(field, entity, entitiesToMap);
    }

    private Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class " + className + " not found", e);
        }
    }

    private <T> List<T> findEntitiesByClassAndField(Class<T> entityClass, String fieldName, Object value)
            throws IllegalAccessException, NoSuchFieldException {
        LOG.trace("Finding entities by class={}, field={} and value={}", entityClass, fieldName, value);
        List<T> foundEntities = new ArrayList<>();
        for (T entity : entitiesOfType(entityClass)) {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            // LOG.debug("{}.{}={}", entityClass.getName(), fieldName, field.get(entity));
            Object fieldValue = field.get(entity);
            if (fieldValue == null) {
                continue;
            }
            if (fieldValue.equals(value)) {
                foundEntities.add(entity);
            } else if (fieldValue.getClass().getAnnotation(Entity.class) != null && JpaUtil.getIdValue(fieldValue).equals(value)) {
                foundEntities.add(entity);
            }
        }
        return foundEntities;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> entitiesOfType(Class<T> entityType) {
        // LOG.debug("Getting entities of type {}", entityType.getName());
        return (List<T>) entities.stream().filter(entity -> entity.getClass().equals(entityType)).toList();
    }

    private Optional<Field> joinTableField(String tableName) {
        for (Class<?> c : classes) {
            for (Field field : c.getDeclaredFields()) {
                JoinTable joinTable = field.getAnnotation(JoinTable.class);
                if (joinTable != null && tableName.equalsIgnoreCase(joinTable.name())) {
                    return Optional.of(field);
                }
            }
        }
        return Optional.empty();
    }

    private void addEntityFromJoinTable(DbTableRow tableRow, Field joinTableField, Class<?> targetType, JoinColumn joinColumn,
            JoinColumn inverseJoinColumn) {
        String lhsPrimaryKey = null;
        String rhsPrimaryKey = null;
        for (DbTableField column : tableRow.columns()) {
            if (column.name().equalsIgnoreCase(joinColumn.name())) {
                lhsPrimaryKey = column.value();
            } else if (column.name().equalsIgnoreCase(inverseJoinColumn.name())) {
                rhsPrimaryKey = column.value();
            }
        }
        if (lhsPrimaryKey == null || rhsPrimaryKey == null) {
            throw new IllegalStateException("Failed to find join table: missing attribute in DBUnit XML file: '" + joinColumn.name()
                    + "' or '" + inverseJoinColumn.name() + "'");
        }
        Object lhs = findEntity(lhsPrimaryKey, joinTableField.getDeclaringClass());
        Object rhs = findEntity(rhsPrimaryKey, targetType);
        JpaUtil.addObjectToCollectionField(joinTableField, lhs, rhs);
    }

    private <T> T createEntity(Class<T> entityType) throws ReflectiveOperationException {
        Constructor<T> constructor = entityType.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private <T> void setField(T entity, String fieldName, String attributeValue) throws ReflectiveOperationException {
        Field field = JpaUtil.getField(entity, fieldName);
        field.setAccessible(true);
        Object fieldValue = createObjectFromString(attributeValue, field, JpaUtil.getPrimaryKeyType(entity.getClass()));
        LOG.trace("Setting field {} to {}", fieldName, fieldValue);
        field.set(entity, fieldValue);
        if (fieldValue != null && fieldValue.getClass().getAnnotation(Entity.class) != null) {
            potentiallyAddValueToCollection(fieldValue, fieldName, entity);
        }
    }

    private <T> void potentiallyAddValueToCollection(Object entity, String fieldName, T value) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            if (oneToMany == null) {
                continue;
            }
            if (oneToMany.mappedBy().equals(fieldName)) {
                JpaUtil.addObjectToCollectionField(field, entity, value);
            }
        }
    }

    private @Nullable Object createObjectFromString(String s, Field field, Class<?> primaryKeyType) {
        Class<?> type;
        if (field.getAnnotation(Id.class) != null) {
            type = primaryKeyType;
        } else {
            type = field.getType();
        }
        return createObjectFromString(s, type);
    }

    @SuppressWarnings("checkstyle:noReturnNull")
    private @Nullable Object createObjectFromString(String s, Class<?> type) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        if (type == Byte.class) {
            return Byte.parseByte(s);
        } else if (type == Short.class) {
            return Short.parseShort(s);
        } else if (type == Integer.class) {
            return Integer.parseInt(s);
        } else if (type == Long.class) {
            return Long.parseLong(s);
        } else if (type == Float.class) {
            return Float.parseFloat(s);
        } else if (type == Double.class) {
            return Double.parseDouble(s);
        } else if (type == Boolean.class) {
            return Boolean.parseBoolean(s);
        } else if (type == Character.class) {
            return s.charAt(0);
        } else if (type == String.class) {
            return s;
        } else if (type == Date.class) {
            return TestUtil.parseDate(s);
        } else if (type == LocalDate.class) {
            return LocalDate.parse(s);
        } else if (type == LocalDateTime.class) {
            return LocalDateTime.parse(s);
        } else if (type == ZonedDateTime.class) {
            return ZonedDateTime.parse(s);
        } else if (type == BigDecimal.class) {
            return new BigDecimal(s);
        } else if (type == BigInteger.class) {
            return new BigInteger(s);
        } else if (type == UUID.class) {
            return UUID.fromString(s);
        } else if (type == List.class) {
            return Arrays.asList(s.replaceAll("[{}]", "").split(","));
        } else {
            return findEntity(s, type);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object findEntity(String strId, Class<?> entityType) {
        if (entityType.isEnum()) {
            Class<? extends Enum> enumType = (Class<? extends Enum>) entityType;
            return Enum.valueOf(enumType, strId);
        }
        for (Object entity : entities) {
            if (entity.getClass().equals(entityType)) {
                Field idField = JpaUtil.getIdField(entity);
                idField.setAccessible(true);
                try {
                    Object id = idField.get(entity);
                    if (id != null && id.equals(createObjectFromString(strId, id.getClass()))) {
                        return entity;
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unexpected problem looking up entity of " + entityType + " with primary key " + strId,
                            e);
                }
            }
        }
        throw new IllegalArgumentException("Entity of " + entityType + " with primary key " + strId + " not found");
    }

    /**
     * Represents one row of data from the database.
     *
     * @author RealLifeDeveloper
     */
    public interface DbTableRow {
        /**
         * Gives the fields of this row.
         *
         * @return the fields
         */
        List<DbTableField> columns();
    }

    /**
     * Represents the value of a single field in the database.
     *
     * @param name  the name of the database column
     * @param value the value of the field
     */
    public record DbTableField(String name, String value) {
    }
}
