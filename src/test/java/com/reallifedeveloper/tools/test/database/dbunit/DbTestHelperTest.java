package com.reallifedeveloper.tools.test.database.dbunit;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:META-INF/spring-context-rld-build-tools-test.xml" })
public class DbTestHelperTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
    public void readDataSetFromClasspathNonExistingDtd() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("DTD not found on classpath: /dbunit/no_such_file");
        DbTestHelper.readDataSetFromClasspath("/dbunit/no_such_file", "/dbunit/testentity.xml");
    }

    @Test
    public void readDataSetFromClasspathNonExistingResourceName() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Dataset not found on classpath: /dbunit/no_such_file");
        DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", "/dbunit/no_such_file");
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

    @Test
    public void constructorNullDataSource() throws Exception {
        IDataSet dataSet =
                DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", "/dbunit/testentity.xml");
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Arguments must not be null: dataSource=null");
        new DbTestHelper(null, dataSet, null, dataTypeFactory);
    }

    @Test
    public void constructorNullDataSet() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Arguments must not be null: dataSource=" + ds + ", dataSet=null");
        new DbTestHelper(ds, null, null, dataTypeFactory);
    }
}
