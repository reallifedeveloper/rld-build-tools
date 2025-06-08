package com.reallifedeveloper.tools.test.database.inmemory;

/**
 * A generator of primary keys of a certain type.
 *
 * @author RealLifeDeveloper
 *
 * @param <ID> the type of the keys generated
 */
public interface PrimaryKeyGenerator<ID> {

    /**
     * Gives the next primary key given the previous maximum primary key value.
     *
     * @param previousMax the previous maximum primary key value of any saved entity, or {@code null} to start from scratch
     *
     * @return the next primary key
     */
    ID nextPrimaryKey(ID previousMax);

}
