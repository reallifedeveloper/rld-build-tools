package com.reallifedeveloper.tools.test.database.dbunit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "TEST_ENTITY")
public class TestEntity extends AbstractEntity<Long> {

    @Column(name = "NAME")
    private String name;

    public TestEntity(Long id, String name) {
        super(id);
        this.name = name;
    }

    TestEntity() {
    }

    public String name() {
        return name;
    }
}
