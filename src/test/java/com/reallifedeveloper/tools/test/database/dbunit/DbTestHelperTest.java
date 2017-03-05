package com.reallifedeveloper.tools.test.database.dbunit;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:META-INF/spring-context-rld-build-tools-test.xml" })
public class DbTestHelperTest {

    @Autowired
    private DataSource ds;

    @Autowired
    private IDataTypeFactory dataTypeFactory;

    @Test
    public void normalUseWithDtd() throws Exception {
        IDataSet dataSet =
                DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", "/dbunit/testentity.xml");
        verifyDataSet(dataSet, dataTypeFactory, 3);
    }

    @Test
    public void normalUseWithDtdAndWithoutDataTypeFactory() throws Exception {
        IDataSet dataSet =
                DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", "/dbunit/single_testentity.xml");
        verifyDataSet(dataSet, null, 1);
    }

    @Test
    public void normalUseWithoutDtd() throws Exception {
        IDataSet dataSet = DbTestHelper.readDataSetFromClasspath(null, "/dbunit/testentity_without_dtd.xml");
        verifyDataSet(dataSet, dataTypeFactory, 3);
    }

    private void verifyDataSet(IDataSet dataSet, IDataTypeFactory dtf, int expectedNumberOfRows)
            throws Exception, SQLException {
        DbTestHelper dbTestHelper = new DbTestHelper(ds, dataSet, null, dtf);
        dbTestHelper.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        dbTestHelper.setTearDownOperation(DatabaseOperation.DELETE_ALL);
        dbTestHelper.init();
        ResultSet rs = ds.getConnection().createStatement().executeQuery("SELECT * FROM TEST_ENTITY");
        int numRows = 0;
        while (rs.next()) {
            Assert.assertNotNull("ID should not be null", rs.getDouble("ID"));
            Assert.assertNotNull("NAME should not be null", rs.getString("NAME"));
            numRows++;
        }
        Assert.assertEquals("Wrong number of rows: ", expectedNumberOfRows, numRows);
        dbTestHelper.clean();
        rs = ds.getConnection().createStatement().executeQuery("SELECT * FROM TEST_ENTITY");
        Assert.assertFalse("Database should be empty after clean", rs.next());
    }

    @Test
    public void readDataSetFromClasspathNullResourceNames() throws Exception {
        String[] resourceNames = null;
        Assert.assertNull("Reading data from null resources should be null",
                DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", resourceNames));
    }

    @Test
    public void readDataSetFromClasspathEmptyResourceNames() throws Exception {
        String[] resourceNames = {};
        Assert.assertNull("Reading data from null resources should be null",
                DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", resourceNames));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullDataSource() throws Exception {
        IDataSet dataSet =
                DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", "/dbunit/testentity.xml");
        new DbTestHelper(null, dataSet, null, dataTypeFactory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullDataSet() throws Exception {
        new DbTestHelper(ds, null, null, dataTypeFactory);
    }
}
