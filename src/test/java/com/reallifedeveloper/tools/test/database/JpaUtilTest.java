package com.reallifedeveloper.tools.test.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("NullAway")
public class JpaUtilTest {

    @Test
    public void getTableNameForEntityWithTableAnnotation() {
        assertEquals("simple_entity", JpaUtil.getTableName(SimpleEntity.class));
    }

    @Test
    public void getTableNameForEntityWithoutTableAnnotation() {
        assertEquals("EntityWithIdClass", JpaUtil.getTableName(EntityWithIdClass.class));
    }

    @Test
    public void getField() throws Exception {
        TestEntity testEntity = new TestEntity(42L, "foo");
        Field field = JpaUtil.getField(testEntity, "name");
        assertEquals("name", field.getName());
        assertEquals("foo", field.get(testEntity));
    }

    @Test
    public void getNonExistingField() {
        Exception e = assertThrows(NoSuchFieldException.class, () -> JpaUtil.getField("notAnEntity", "noSuchField"));
        assertEquals("noSuchField", e.getMessage());
    }

    @Test
    public void getIdField() throws Exception {
        TestEntity testEntity = new TestEntity(42L, "foo");
        Field idField = JpaUtil.getIdField(testEntity);
        assertEquals("id", idField.getName());
        assertEquals(42L, idField.get(testEntity));
    }

    @Test
    public void getNonExistingIdField() {
        Exception e = assertThrows(IllegalStateException.class, () -> JpaUtil.getIdField("notAnEntity"));
        assertEquals("Id field not found for entity notAnEntity", e.getMessage());
    }

    @Test
    public void getIdValue() throws Exception {
        TestEntity testEntity = new TestEntity(42L, "foo");
        assertEquals(42L, JpaUtil.getIdValue(testEntity));
    }

    @Test
    public void getFieldNameFromColumnAnnotation() {
        assertEquals("name", JpaUtil.getFieldName("my_name", TestEntity.class));
    }

    @Test
    public void getFieldNameFromFieldInSuperClass() {
        assertEquals("foo", JpaUtil.getFieldName("foo", TestEntity.class));
    }

    @Test
    public void getFieldNameFromJoinColunAnnotation() {
        assertEquals("simpleEntity", JpaUtil.getFieldName("simple_entity_id", TestEntity.class));
    }

    @Test
    public void getNonExistingFieldName() {
        assertThrows(IllegalArgumentException.class, () -> JpaUtil.getFieldName("noSuchField", TestEntity.class));
    }

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
    public void addObjectToCollectionField() throws Exception {
        TestEntity testEntity = new TestEntity(42L, "foo");
        assertTrue(testEntity.getSimpleEntities().isEmpty());
        Field field = JpaUtil.getField(testEntity, "simpleEntities");
        UUID uuid = UUID.randomUUID();
        JpaUtil.addObjectToCollectionField(field, testEntity, new SimpleEntity(uuid));
        assertEquals(1, testEntity.getSimpleEntities().size());
        assertEquals(uuid, testEntity.getSimpleEntities().get(0).getId());
    }

    @Test
    public void addObjectToNonCollectionField() throws Exception {
        TestEntity testEntity = new TestEntity(42L, "foo");
        Field nonCollectionField = JpaUtil.getField(testEntity, "name");
        Throwable e = assertThrows(AssertionError.class, () -> JpaUtil.addObjectToCollectionField(nonCollectionField, testEntity, "bar"));
        assertEquals("Expected field to be a Collection: field=" + nonCollectionField, e.getMessage());
    }

    @Test
    public void addEntitiesToMapField() throws Exception {
        TestEntity testEntity = new TestEntity(42L, "foo");
        SimpleEntity simpleEntity1 = new SimpleEntity(UUID.randomUUID());
        SimpleEntity simpleEntity2 = new SimpleEntity(UUID.randomUUID());
        Field field = JpaUtil.getField(testEntity, "simpleEntityMap");
        JpaUtil.addEntitiesToMapField(field, testEntity, List.of(simpleEntity1, simpleEntity2));
        assertEquals(simpleEntity1, testEntity.getSimpleEntityMap().get(simpleEntity1.getId()));
        assertEquals(simpleEntity2, testEntity.getSimpleEntityMap().get(simpleEntity2.getId()));
    }

    @Test
    public void addEntitiesToNonMapField() throws Exception {
        TestEntity testEntity = new TestEntity(42L, "foo");
        Field nonMapField = JpaUtil.getField(testEntity, "name");
        Throwable e = assertThrows(AssertionError.class, () -> JpaUtil.addEntitiesToMapField(nonMapField, testEntity, List.of("bar")));
        assertEquals("Expected field to be a Map: field=" + nonMapField, e.getMessage());
    }

    @Test
    public void fieldNameForLogging() throws Exception {
        TestEntity testEntity = new TestEntity(42L, "foo");
        assertEquals(TestEntity.class.getName() + ".name", JpaUtil.fieldNameForLogging(testEntity, JpaUtil.getField(testEntity, "name")));
    }

    @Entity
    @Table(name = "simple_entity")
    @Getter
    @AllArgsConstructor
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

        @ManyToOne
        @JoinColumn(name = "simple_entity_id")
        private SimpleEntity simpleEntity;

        AbstractEntity() {
            this(null);
        }

        AbstractEntity(@Nullable ID id) {
            this.id = id;
        }
    }

    @Entity
    @Getter
    private static class TestEntity extends AbstractEntity<String, Long> {
        @Column(name = "MY_NAME")
        private String name;

        private List<SimpleEntity> simpleEntities = new ArrayList<>();

        @MapKey(name = "id")
        private Map<UUID, SimpleEntity> simpleEntityMap = new HashMap<>();

        TestEntity(Long id, String name) {
            super(id);
            this.name = name;
        }
    }
}
