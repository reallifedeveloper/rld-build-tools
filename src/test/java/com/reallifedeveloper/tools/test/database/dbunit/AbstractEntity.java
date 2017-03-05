package com.reallifedeveloper.tools.test.database.dbunit;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

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
