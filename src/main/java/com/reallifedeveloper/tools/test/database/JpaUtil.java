package com.reallifedeveloper.tools.test.database;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.experimental.UtilityClass;

/**
 * A utility class for working with JPA.
 *
 * @author RealLifeDeveloper
 */
@UtilityClass
public class JpaUtil {

    /**
     * Gives the primary key class for a given entity class.
     *
     * @param <ID>       the type of the primary key
     * @param entityType the class object representing the entity
     *
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
            assert parameterizedType.getActualTypeArguments().length == typeVariables.length;
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
}
