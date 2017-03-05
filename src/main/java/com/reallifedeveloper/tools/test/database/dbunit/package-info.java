/**
 * Support classes for writing <a href="http://dbunit.sourceforge.net/">DbUnit</a>
 * tests that work with a real database.
 * <p>
 * DbUnit tests are useful for testing repository implementations, for example to
 * verify that JPA mappings and queries are correct. Don't overuse this kind of
 * tests, though; they are relatively slow. For testing non-repository classes,
 * such as services, it is better to inject in-memory repositories (see
 * {@link com.reallifedeveloper.tools.test.database.inmemory.InMemoryJpaRepository}).
 *
 * @author RealLifeDeveloper
 *
 */
package com.reallifedeveloper.tools.test.database.dbunit;
