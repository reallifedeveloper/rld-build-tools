package com.reallifedeveloper.tools.test.database.inmemory;

/**
 * A primary key generator that generates a sequence of integers.
 *
 * @author RealLifeDeveloper
 *
 */
public class IntegerPrimaryKeyGenerator implements PrimaryKeyGenerator<Integer> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer nextPrimaryKey(Integer previousMax) {
        if (previousMax == null) {
            return 1;
        } else {
            return previousMax + 1;
        }
    }
}
