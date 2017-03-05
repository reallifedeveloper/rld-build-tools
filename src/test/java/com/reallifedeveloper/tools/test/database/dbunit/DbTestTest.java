package com.reallifedeveloper.tools.test.database.dbunit;

import javax.sql.DataSource;

import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:META-INF/spring-context-rld-build-tools-test.xml" })
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
        Assert.assertEquals("Wrong name: ", "foo", repository.findOne(42L).name());
        Assert.assertEquals("Wrong name: ", "bar", repository.findOne(4711L).name());
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
        Assert.assertEquals("Wrong schema name: ", "foo", dbTest.getSchemaName());
        Assert.assertNull("DTD resource name should be null", dbTest.getDataSetDtdResourceName());
        Assert.assertEquals("Wrong number of data set resource names: ", 1, dbTest.getDataSetResourceNames().length);
        Assert.assertNull("Data type factory should be null", dbTest.getDataTypeFactory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorThreeArgsNullDataSetResourceNames() {
        new DbTest("foo", "bar", (String[]) null);
    }

    @Override
    protected DataSource getDataSource() {
        return ds;
    }

    @Override
    protected IDataTypeFactory getDataTypeFactory() {
        return dataTypeFactory;
    }

    private static class DbTest extends AbstractDbTest {
        DbTest(String schemaName, String dataSetResourceName) {
            super(schemaName, dataSetResourceName);
        }

        DbTest(String schemaName, String dataSetDtdResourceName, String... dataSetResourceNames) {
            super(schemaName, dataSetDtdResourceName, dataSetResourceNames);
        }

        @Override
        protected DataSource getDataSource() {
            return null;
        }
    }
}
