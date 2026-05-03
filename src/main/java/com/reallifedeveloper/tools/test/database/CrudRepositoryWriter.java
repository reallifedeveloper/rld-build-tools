package com.reallifedeveloper.tools.test.database;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;

import com.reallifedeveloper.tools.test.TestUtil;

/**
 * A helper class to write data into a {@link CrudRepository} from some data source, e.g., a CSV file, where each entity is represented by a
 * {@link DbTableRow}.
 * <p>
 * This can be useful for inserting test data into a repository, irrespective of whether the repository connects to a real database or not.
 *
 * TODO: The current implementation only has basic support for "to many" associations (there must be a &amp;JoinTable annotation on a field,
 * with &amp;JoinColumn annotations), and for enums (an enum must be stored as a string).
 *
 * @author RealLifeDeveloper
 */
@Getter
@SuppressWarnings("PMD")
@SuppressFBWarnings(value = "IMPROPER_UNICODE", justification = "equalsIgnoreCase only being used to compare table, column or field names")
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
     *
     * @param tableRow             the {@code TableRow} with the data to insert into the fields of the newly created entity
     * @param repositoryEntityType the class object representing {@code T}, i.e., the type ofrepository entities
     * @param entityType           the class object representing {@code E}, i.e., the type of entity being created, or {@code null}
     * @param repository           the repository in which to insert the newly created entity
     * @param tableName            the name of the database table where the entity should be stored
     *
     * @return {@code true} if an entity was created, no matter if it was saved in the repository, {@code false} otherwise
     *
     * @throws ReflectiveOperationException if some reflection operation failed creating the entity or setting is fields
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "Logging of entities only")
    public <T, E, ID extends Serializable> boolean writeEntity(DbTableRow tableRow, Class<T> repositoryEntityType,
            @Nullable Class<E> entityType, CrudRepository<T, ID> repository, String tableName) throws ReflectiveOperationException {
        if (entityType == null || entityType.getAnnotation(Entity.class) == null || !getTableName(entityType).equalsIgnoreCase(tableName)) {
            return false;
        }
        E entity = createEntity(entityType);
        for (DbTableField column : tableRow.columns()) {
            String fieldName = getFieldName(column.name(), entityType);
            setField(entity, fieldName, column.value());
        }
        LOG.debug("Saving entity {}", entity);
        entities.add(entity);
        classes.add(entity.getClass());
        if (entityType.equals(repositoryEntityType)) {
            T entity2 = repositoryEntityType.cast(entity);
            repository.save(entity2);
        }
        return true;
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
     * A helper method to get the table name associated with an {@link Entity}.
     *
     * @param <T>        the type of entity
     *
     * @param entityType the class object representing {@code T}
     *
     * @return the table name associated with {@code entityType}
     */
    public static <T> String getTableName(Class<T> entityType) {
        Table table = entityType.getAnnotation(Table.class);
        if (table == null) {
            return entityType.getSimpleName();
        } else {
            return table.name();
        }
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
        addObjectToCollectionField(joinTableField, lhs, rhs);
    }

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "Only entity values being logged")
    private void addObjectToCollectionField(Field field, Object lhs, Object rhs) {
        try {
            Method add = field.getType().getMethod("add", Object.class);
            LOG.debug("Calling add method on field {} of entity {} to add entity {} to collection", field.getName(), lhs, rhs);
            add.invoke(field.get(lhs), rhs);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Method 'add' not found -- field " + field + " should be a Collection", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Unexpected problem", e);
        }
    }

    private <T> String getFieldName(String attributeName, Class<T> entityType) {
        return getFieldName(attributeName, entityType, entityType);
    }

    private <T> String getFieldName(String attributeName, Class<T> entityType, Class<?> originalEntityType) {
        for (Field field : entityType.getDeclaredFields()) {
            if (checkFieldName(attributeName, field)) {
                return field.getName();
            }
        }
        if (entityType.getSuperclass() == null) {
            throw new IllegalArgumentException("Cannot find any field matching attribute '" + attributeName.toLowerCase(Locale.getDefault())
                    + "' for " + originalEntityType);
        } else {
            return getFieldName(attributeName, entityType.getSuperclass(), originalEntityType);
        }
    }

    private boolean checkFieldName(String attributeName, Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column == null || column.name() == null) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn == null || joinColumn.name() == null) {
                return field.getName().equalsIgnoreCase(attributeName);
            } else {
                return joinColumn.name().equalsIgnoreCase(attributeName);
            }
        } else {
            return column.name().equalsIgnoreCase(attributeName);
        }
    }

    private <T> T createEntity(Class<T> entityType) throws ReflectiveOperationException {
        Constructor<T> constructor = entityType.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private <T> void setField(T entity, String fieldName, String attributeValue) throws ReflectiveOperationException {
        Field field = getField(entity, fieldName);
        field.setAccessible(true);
        Object fieldValue = createObjectFromString(attributeValue, field, JpaUtil.getPrimaryKeyType(entity.getClass()));
        field.set(entity, fieldValue);
        if (fieldValue.getClass().getAnnotation(Entity.class) != null) {
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
                addObjectToCollectionField(field, entity, value);
            }
        }
    }

    private Field getField(Object entity, String fieldName) throws NoSuchFieldException {
        Class<?> entityType = entity.getClass();
        while (entityType != null) {
            for (Field field : entityType.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(fieldName)) {
                    return field;
                }
            }
            entityType = entityType.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }

    private Object createObjectFromString(String s, Field field, Class<?> primaryKeyType) {
        Class<?> type;
        if (field.getAnnotation(Id.class) != null) {
            type = primaryKeyType;
        } else {
            type = field.getType();
        }
        return createObjectFromString(s, type);
    }

    private Object createObjectFromString(String s, Class<?> type) {
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
        } else if (type == BigDecimal.class) {
            return new BigDecimal(s);
        } else if (type == BigInteger.class) {
            return new BigInteger(s);
        } else if (type == UUID.class) {
            return UUID.fromString(s);
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
                Field idField = getIdField(entity);
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

    private Field getIdField(Object entity) {
        Class<?> entityType = entity.getClass();
        while (entityType != null) {
            for (Field field : entityType.getDeclaredFields()) {
                if (field.getDeclaredAnnotation(Id.class) != null) {
                    return field;
                }
            }
            entityType = entityType.getSuperclass();
        }
        throw new IllegalStateException("Id field not found for entity " + entity);
    }

    /**
     * Represents one row of data from the database.
     *
     * @author RealLifeDeveloper
     */
    @SuppressWarnings("PMD.ImplicitFunctionalInterface")
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
