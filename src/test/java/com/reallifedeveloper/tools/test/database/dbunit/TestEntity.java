package com.reallifedeveloper.tools.test.database.dbunit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "TEST_ENTITY")
public class TestEntity extends AbstractEntity<Long> {

    @Column(name = "NAME")
    private String name;

    public TestEntity(Long id, String name) {
        super(id);
        this.name = name;
    }

    @SuppressWarnings("NullAway")
    TestEntity() {
    }

    public String name() {
        return name;
    }
}
