package com.reallifedeveloper.tools.test.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@SuppressWarnings("NullAway")
public class JpaUtilTest {

    @Test
    public void getPrimaryKeyTypeForSimpleEntity() {
        assertEquals(UUID.class, JpaUtil.getPrimaryKeyType(SimpleEntity.class));
    }

    @Test
    public void getPrimaryKeyTypeForEntityWithInheritedId() {
        assertEquals(Long.class, JpaUtil.getPrimaryKeyType(TestEntity.class));
    }

    @Test
    public void getPrimaryKeyTypeForEntityWithIdClass() {
        assertEquals(MyEmbeddedId.class, JpaUtil.getPrimaryKeyType(EntityWithIdClass.class));
    }

    @Test
    public void getPrimaryKeyTypeForEntityWithEmbeddedId() {
        assertEquals(MyEmbeddedId.class, JpaUtil.getPrimaryKeyType(EntityWithEmbeddedId.class));
    }

    @Test
    public void getPrimaryKeyTypeForNonEntity() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> JpaUtil.getPrimaryKeyType(String.class));
        assertEquals("entityType does not have @Entity annotation: entityType=class java.lang.String", e.getMessage());
    }

    @Test
    public void getPrimaryKeyforEntityWithoutId() {
        Exception e = assertThrows(IllegalStateException.class, () -> JpaUtil.getPrimaryKeyType(EntityWithoutId.class));
        assertEquals("entityType without primary key annotation: entityType=" + EntityWithoutId.class, e.getMessage());
    }

    @Test
    public void getNonExistingField() {
        Exception e = assertThrows(NoSuchFieldException.class, () -> JpaUtil.getField("myEntity", "noSuchField"));
        assertEquals("noSuchField", e.getMessage());
    }

    @Test
    public void getNonExistingIdField() {
        Exception e = assertThrows(IllegalStateException.class, () -> JpaUtil.getIdField("myEntity"));
        assertEquals("Id field not found for entity myEntity", e.getMessage());
    }

    @Entity
    @Getter
    private static class SimpleEntity {
        @Id
        private UUID id;
    }

    @Entity
    @Getter
    @IdClass(MyEmbeddedId.class)
    private static class EntityWithIdClass {
        @Id
        private String foo;
        @Id
        private String bar;
    }

    @Entity
    @Getter
    private static class EntityWithEmbeddedId {
        @EmbeddedId
        private MyEmbeddedId id;
    }

    @Embeddable
    @Getter
    private static class MyEmbeddedId {
        private String foo;
        private String bar;
    }

    @Entity
    private static class EntityWithoutId {
    }

    @MappedSuperclass
    @Getter
    private abstract static class AbstractEntity<T, ID> {
        @Id
        private final @Nullable ID id;

        private T foo;

        AbstractEntity() {
            this(null);
        }

        AbstractEntity(@Nullable ID id) {
            this.id = id;
        }
    }

    @Entity
    private static class TestEntity extends AbstractEntity<String, Long> {
        @Column(name = "NAME")
        private String name;
    }
}
