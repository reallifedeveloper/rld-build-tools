package com.reallifedeveloper.tools.test.database;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKey;
import jakarta.persistence.Table;
import lombok.experimental.UtilityClass;

/**
 * A utility class for working with JPA entities and mappings.
 *
 * @author RealLifeDeveloper
 */
@UtilityClass
@SuppressWarnings("PMD")
@SuppressFBWarnings(value = { "CRLF_INJECTION_LOGS", "IMPROPER_UNICODE" })
public class JpaUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JpaUtil.class);

    /**
     * Gives the table name associated with an {@link Entity}.
     *
     * @param <T>        the type of entity
     * @param entityType the class object representing {@code T}
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

    /**
     * Gets the {@code Field} object for a field with a given name in a given object, also making the field accessible by calling
     * {@code Field.setAccessible(true}.
     * <p>
     * This method never returns {@code null}; if the field is not found, a {@code NoSuchFieldException} is thrown
     *
     * @param entity    the object to search for the field
     * @param fieldName the name of the field for which to search
     * @return the {@code Field} object representing the field named {@code fieldName} in {@code entity
     * }
     * @throws NoSuchFieldException if the field could not be found
     */
    public static Field getField(Object entity, String fieldName) throws NoSuchFieldException {
        Class<?> entityType = entity.getClass();
        while (entityType != null) {
            for (Field field : entityType.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(fieldName)) {
                    field.setAccessible(true);
                    return field;
                }
            }
            entityType = entityType.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**
     * Gets thd ID field of an entity, i.e., the field annotated with an {@code ID} annotation.
     * <p>
     * This method never returns {@code null}; if no ID field is found, an {@code IllegalStateException} is thrown.
     *
     * @param entity the entity to search
     * @return the {@code Field} object representing the ID field of {@code entity}
     */
    public static Field getIdField(Object entity) {
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
     * Gets the value of the ID field of an entity.
     *
     * @param entity the entity to search
     *
     * @return the value of the ID Field
     *
     * @throws IllegalAccessException if there was a problem getting the value using reflection
     */
    public static Object getIdValue(Object entity) throws IllegalAccessException {
        Field idField = JpaUtil.getIdField(entity);
        idField.setAccessible(true);
        Object id = idField.get(entity);
        return id;
    }

    /**
     * Gets the name of the field representing a given attribute in a given class or one of its superclasses.
     * <p>
     * A field is considered to represent an attribute if one of the following is true:
     * <ul>
     * <li>The field has a {@code Column} annotation with a {@code name} equal to the attribute.</li>
     * <li>The field has a {@code JoinColunm} annotation with a {@code name} equal to the attribute.</li>
     * <li>The field name is the same as the attribute.</li>
     * </ul>
     * <p>
     * This method never returns {@code null}; if no matching field is found, an {@code IllegalArgumentException} is thrown.
     *
     * @param <T>           the type of the entity to search
     * @param attributeName the attribute for which to try to find a matching field
     * @param entityType    the class in which to search for the field, continuing with superclasses if necessary
     * @return the name of the field representing {@code attributeName}
     * @throws IllegalArgumentException if no matching field could be found
     */
    public static <T> String getFieldName(String attributeName, Class<T> entityType) {
        return getFieldName(attributeName, entityType, entityType);
    }

    private static <T> String getFieldName(String attributeName, Class<T> entityType, Class<?> originalEntityType) {
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

    private static boolean checkFieldName(String attributeName, Field field) {
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

    /**
     * Gives the primary key class for a given entity class.
     *
     * @param <ID>       the type of the primary key
     * @param entityType the class object representing the entity
     * @return the primary key class of {@code entityType}
     */
    @SuppressWarnings("unchecked")
    public static <ID> Class<ID> getPrimaryKeyType(Class<?> entityType) {
        if (entityType.getAnnotation(Entity.class) == null) {
            throw new IllegalArgumentException("entityType does not have @Entity annotation: entityType=" + entityType);
        }
        Class<?> entityTypeOrSuperClass = entityType;
        Type genericSuperclass = null;
        while (entityTypeOrSuperClass.getSuperclass() != null) {
            if (entityTypeOrSuperClass.getAnnotation(IdClass.class) != null) {
                return (Class<ID>) entityTypeOrSuperClass.getAnnotation(IdClass.class).value();
            }
            for (Field field : entityTypeOrSuperClass.getDeclaredFields()) {
                if (field.getAnnotation(EmbeddedId.class) != null) {
                    return (Class<ID>) field.getType();
                }
                if (field.getAnnotation(Id.class) != null) {
                    return (Class<ID>) getActualIdType(genericSuperclass, entityTypeOrSuperClass.getTypeParameters(),
                            field.getGenericType()).orElse((Class<Object>) field.getType());
                }
            }
            genericSuperclass = entityTypeOrSuperClass.getGenericSuperclass();
            entityTypeOrSuperClass = entityTypeOrSuperClass.getSuperclass();
        }
        throw new IllegalStateException("entityType without primary key annotation: entityType=" + entityType);
    }

    @SuppressWarnings("unchecked")
    private static <ID> Optional<Class<ID>> getActualIdType(@Nullable Type genericEntityType, TypeVariable<?>[] typeVariables,
            Type genericIdType) {
        if (genericEntityType instanceof ParameterizedType parameterizedType) {
            assert parameterizedType.getActualTypeArguments().length == typeVariables.length
                    : "Number of actual type arguments (" + parameterizedType.getActualTypeArguments().length
                            + ") differs from number of type variables (" + typeVariables.length + ")";
            for (int i = 0; i < typeVariables.length; i++) {
                TypeVariable<?> typeVariable = typeVariables[i];
                if (!typeVariable.getName().equals(genericIdType.getTypeName())) {
                    continue;
                }
                Type actualType = parameterizedType.getActualTypeArguments()[i];
                try {
                    return Optional.of((Class<ID>) Class.forName(actualType.getTypeName()));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Unexpected problem looking up ID class", e);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Calls the {@code add} method on the the {@code java.util.Collection} referenced by the given entity field to add the given value.
     * <p>
     * This method should only be called when you know that the field actually is a collection.
     *
     * @param field  the field holding the collection
     * @param entity the entity where {@code field} lives
     * @param value  the value to add, may be {@code null}
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "Only entity values being logged")
    public static void addObjectToCollectionField(Field field, Object entity, Object value) {
        assert Collection.class.isAssignableFrom(field.getType()) : "Expected field to be a Collection: field=" + field;
        try {
            Method add = field.getType().getMethod("add", Object.class);
            LOG.debug("Calling add method on field {} of entity {} to add entity {} to collection", field.getName(), entity, value);
            add.invoke(field.get(entity), value);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Method 'add' not found -- field " + fieldNameForLogging(entity, field) + " should be a Collection", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Unexpected problem", e);
        }
    }

    /**
     * Calls the {@code put} method on the {@code java.util.Map} referenced by the given entity field to add the entities to map.
     * <p>
     * The key is found using the {@code MapKey} annotation of the field.
     * <p>
     * This method should only be called when you know that the field actually is a map.
     *
     * @param field         the field holding the map
     * @param entity        the entity where {@code field} lives
     * @param entitiesToMap a list of entities to map
     */
    public static void addEntitiesToMapField(Field field, Object entity, List<?> entitiesToMap) {
        LOG.trace("addEntitiesToMapField: field={}, entity={}, entitiesToMap={}", field, entity, entitiesToMap);
        assert Map.class.isAssignableFrom(field.getType()) : "Expected field to be a Map: field=" + field;
        for (Object entityToMap : entitiesToMap) {
            MapKey mapKey = field.getAnnotation(MapKey.class);
            if (mapKey == null) {
                throw new IllegalStateException(
                        "Field " + fieldNameForLogging(entity, field) + " is a Map but is missing MapKey annotation");
            }
            try {
                Method put = field.getType().getMethod("put", Object.class, Object.class);
                Object key = JpaUtil.getField(entityToMap, mapKey.name()).get(entityToMap);
                LOG.debug("Adding entity {} to map {} with key {}", entityToMap, fieldNameForLogging(entity, field), key);
                put.invoke(field.get(entity), key, entityToMap);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "Method 'put' not found -- field " + fieldNameForLogging(entity, field) + " should be a Map", e);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(
                        "Field " + mapKey.name() + " not found, it is used in MapKey annotation in " + fieldNameForLogging(entity, field),
                        e);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Unexpected problem", e);
            }
        }
    }

    /**
     * Gives a string representation of an entity's field, as {@code "<entity class>.<field name>"}, useful for logging.
     *
     * @param entity the entity holding the field
     * @param field  the field
     *
     * @return the string {@code "<entity class>.<field name>"}
     */
    public static String fieldNameForLogging(Object entity, Field field) {
        return entity.getClass().getName() + "." + field.getName();
    }
}
