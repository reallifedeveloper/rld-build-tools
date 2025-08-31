package com.reallifedeveloper.tools.test.database.dbunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import javax.sql.DataSource;

import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DatabaseTestConfiguration.class)
@SuppressWarnings("NullAway")
public class DbTestTest extends AbstractDbTest {

    @Autowired
    private JpaTestEntityRepository repository;

    @Autowired
    private DataSource ds;

    @Autowired
    private IDataTypeFactory dataTypeFactory;

    public DbTestTest() {
        super(null, "/dbunit/rld-build-tools.dtd", "/dbunit/testentity.xml");
    }

    @Test
    public void testRepository() {
        assertEquals("foo", repository.findById(42L).get().name(), "Wrong name: ");
        assertEquals("bar", repository.findById(4711L).get().name(), "Wrong name: ");
    }

    @Test
    public void tearDownBeforeSetUp() throws Exception {
        DbTest dbTest = new DbTest("foo", "bar");
        // Should not give a NullPointerException
        dbTest.tearDownDatabase();
    }

    @Test
    public void constructorTwoArgs() {
        DbTest dbTest = new DbTest("foo", "bar");
        assertEquals("foo", dbTest.getSchemaName().get(), "Wrong schema name: ");
        assertFalse(dbTest.getDataSetDtdResourceName().isPresent(), "DTD resource name should be Optional.empty");
        assertEquals(1, dbTest.getDataSetResourceNames().length, "Wrong number of data set resource names: ");
        assertTrue(dbTest.getDataTypeFactory().isEmpty(), "Data type factory should be empty");
    }

    @Test
    public void constructorThreeArgsNullDataSetResourceNames() {
        assertThrows(IllegalArgumentException.class, () -> new DbTest("foo", "bar", (String[]) null));
    }

    @Override
    protected DataSource getDataSource() {
        return ds;
    }

    @Override
    protected Optional<IDataTypeFactory> getDataTypeFactory() {
        return Optional.ofNullable(dataTypeFactory);
    }

    private static class DbTest extends AbstractDbTest {
        DbTest(String schemaName, String dataSetResourceName) {
            super(schemaName, dataSetResourceName);
        }

        DbTest(String schemaName, String dataSetDtdResourceName, String... dataSetResourceNames) {
            super(schemaName, dataSetDtdResourceName, dataSetResourceNames);
        }

        @Override
        @SuppressWarnings("checkstyle:noReturnNull")
        protected DataSource getDataSource() {
            return null;
        }
    }
}
