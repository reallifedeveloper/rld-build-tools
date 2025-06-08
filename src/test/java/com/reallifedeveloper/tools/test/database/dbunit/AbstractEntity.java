package com.reallifedeveloper.tools.test.database.dbunit;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractEntity<ID> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private final ID id;

    protected AbstractEntity() {
        this(null);
    }

    protected AbstractEntity(ID id) {
        this.id = id;
    }

    public ID id() {
        return id;
    }
}
