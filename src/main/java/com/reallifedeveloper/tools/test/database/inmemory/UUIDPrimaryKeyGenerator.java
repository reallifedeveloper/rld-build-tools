package com.reallifedeveloper.tools.test.database.inmemory;

import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A primary key generator that generates random UUIDs.
 *
 * @author RealLifeDeveloper
 */
public class UUIDPrimaryKeyGenerator implements PrimaryKeyGenerator<String> {

    @Override
    public String nextPrimaryKey(@Nullable String previousMax) {
        return UUID.randomUUID().toString();
    }

}
