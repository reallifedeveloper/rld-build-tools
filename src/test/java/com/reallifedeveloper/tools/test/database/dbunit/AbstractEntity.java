package com.reallifedeveloper.tools.test.database.dbunit;

import org.checkerframework.checker.nullness.qual.Nullable;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractEntity<ID> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private final @Nullable ID id;

    protected AbstractEntity() {
        this(null);
    }

    protected AbstractEntity(@Nullable ID id) {
        this.id = id;
    }

    public @Nullable ID id() {
        return id;
    }
}
