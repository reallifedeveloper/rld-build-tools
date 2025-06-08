package com.reallifedeveloper.tools.test.database.dbunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DatabaseTestConfiguration.class)
public class DbTestHelperTest {

    @Autowired
    private DataSource ds;

    @Autowired
    private IDataTypeFactory dataTypeFactory;

    @Test
    public void normalUseWithDtd() throws Exception {
        IDataSet dataSet = DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", "/dbunit/testentity.xml");
        verifyDataSet(dataSet, dataTypeFactory, 3);
    }

    @Test
    public void normalUseWithDtdAndWithoutDataTypeFactory() throws Exception {
        IDataSet dataSet = DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", "/dbunit/single_testentity.xml");
        verifyDataSet(dataSet, null, 1);
    }

    @Test
    public void normalUseWithoutDtd() throws Exception {
        IDataSet dataSet = DbTestHelper.readDataSetFromClasspath(null, "/dbunit/testentity_without_dtd.xml");
        verifyDataSet(dataSet, dataTypeFactory, 3);
    }

    private void verifyDataSet(IDataSet dataSet, IDataTypeFactory dtf, int expectedNumberOfRows) throws Exception, SQLException {
        DbTestHelper dbTestHelper = new DbTestHelper(ds, dataSet, null, Optional.ofNullable(dtf));
        dbTestHelper.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        dbTestHelper.setTearDownOperation(DatabaseOperation.DELETE_ALL);
        dbTestHelper.init();
        ResultSet rs = ds.getConnection().createStatement().executeQuery("SELECT * FROM TEST_ENTITY");
        int numRows = 0;
        while (rs.next()) {
            assertNotNull(rs.getDouble("ID"), "ID should not be null");
            assertNotNull(rs.getString("NAME"), "NAME should not be null");
            numRows++;
        }
        assertEquals(expectedNumberOfRows, numRows, "Wrong number of rows: ");
        dbTestHelper.clean();
        rs = ds.getConnection().createStatement().executeQuery("SELECT * FROM TEST_ENTITY");
        assertFalse(rs.next(), "Database should be empty after clean");
    }

    @Test
    public void readDataSetFromClasspathNonExistingDtd() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> DbTestHelper.readDataSetFromClasspath("/dbunit/no_such_file", "/dbunit/testentity.xml"));
        assertEquals("DTD not found on classpath: /dbunit/no_such_file", e.getMessage());
    }

    @Test
    public void readDataSetFromClasspathNonExistingResourceName() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", "/dbunit/no_such_file"));
        assertEquals("Dataset not found on classpath: /dbunit/no_such_file", e.getMessage());
    }

    @Test
    public void readDataSetFromClasspathNullResourceNames() throws Exception {
        String[] resourceNames = null;
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", resourceNames));
        assertEquals("You must provide at least one dataSetResourceName", e.getMessage());
    }

    @Test
    public void readDataSetFromClasspathEmptyResourceNames() throws Exception {
        String[] resourceNames = {};
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", resourceNames));
        assertEquals("You must provide at least one dataSetResourceName", e.getMessage());

    }

    @Test
    public void constructorNullDataSource() throws Exception {
        IDataSet dataSet = DbTestHelper.readDataSetFromClasspath("/dbunit/rld-build-tools.dtd", "/dbunit/testentity.xml");
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> new DbTestHelper(null, dataSet, null, Optional.of(dataTypeFactory)));
        assertTrue(e.getMessage().contains("Arguments must not be null: dataSource=null"));
    }

    @Test
    public void constructorNullDataSet() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class, () -> new DbTestHelper(ds, null, null, Optional.of(dataTypeFactory)));
        assertTrue(e.getMessage().contains("Arguments must not be null: dataSource=" + ds + ", dataSet=null"));
    }

}
