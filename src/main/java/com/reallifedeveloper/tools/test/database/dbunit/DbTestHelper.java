package com.reallifedeveloper.tools.test.database.dbunit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;

/**
 * A helper class used by {@link AbstractDbTest}. It could be used directly by a test
 * class that for some reason cannot inherit from <code>AbstractDbTest</code>.
 *
 * @author RealLifeDeveloper
 *
 */
public class DbTestHelper {

    private final IDatabaseTester databaseTester;

    /**
     * Creates a new <code>DbTestHelper</code>, with test data provided by the given
     * <code>dataSet</code> and using the given <code>dataSource</code> to insert it.
     * The database schema name may be provided (can be <code>null</code>), and the
     * type of database can be defined using <code>dataTypeFactory</code>
     * (can also be <code>null</code>).
     *
     * @param dataSource the <code>DataSource</code> to use when inserting test data
     * @param dataSet the DbUnit test data set to read
     * @param schemaName the name of the database schema, or <code>null</code>
     * @param dataTypeFactory an <code>IDataTypeFactory</code>, or <code>null</code>
     */
    public DbTestHelper(DataSource dataSource, IDataSet dataSet, String schemaName,
            final IDataTypeFactory dataTypeFactory) {
        if (dataSource == null || dataSet == null) {
            throw new IllegalArgumentException("Arguments must not be null: dataSource=" + dataSource + ", dataSet="
                    + dataSet + ", schemaName=" + schemaName + ", dataTypeFactory=" + dataTypeFactory);
        }
        databaseTester = new DataSourceDatabaseTester(dataSource, schemaName) {
            // We override this method to configure the dataTypeFactory for the connection, to avoid warnings
            // from DbUnit.
            @Override
            public IDatabaseConnection getConnection() throws Exception {
                IDatabaseConnection conn = super.getConnection();
                if (dataTypeFactory != null) {
                    conn.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, dataTypeFactory);
                    conn.getConfig().setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
                }
                return conn;
            }
        };
        databaseTester.setDataSet(dataSet);
    }

    /**
     * Reads XML data set files from the classpath resources pointed to by <code>dataSetResourceNames</code>,
     * optionally validating using the DTD pointed to by <code>dataSetDtdResourceName</code>
     * property.
     * <p>
     * The <code>dataSetDtdResourceName</code> parameter may be <code>null</code>, in which case no
     * validation is performed.
     *
     * @param dataSetDtdResourceName the name of the resource containing the DTD for test data files,
     * or <code>null</code>
     * @param dataSetResourceNames the names of the resources containing test data
     *
     * @return the test data set
     *
     * @throws DataSetException if some resource if malformed
     * @throws IOException if reading a resource failed
     */
    public static IDataSet readDataSetFromClasspath(String dataSetDtdResourceName, String... dataSetResourceNames)
            throws DataSetException, IOException {
        if (dataSetResourceNames == null || dataSetResourceNames.length == 0) {
            return null;
        } else {
            FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
            if (dataSetDtdResourceName != null) {
                builder.setMetaDataSetFromDtd(DbTestHelper.class.getResourceAsStream(dataSetDtdResourceName));
            }
            builder.setColumnSensing(true);
            List<IDataSet> dataSets = new ArrayList<>();
            for (String dataSetResourceName : dataSetResourceNames) {
                dataSets.add(builder.build(DbTestHelper.class.getResourceAsStream(dataSetResourceName)));
            }
            return new CompositeDataSet(dataSets.toArray(new IDataSet[dataSets.size()]));
        }
    }

    /**
     * Initializes the test data before each test case.
     *
     * @throws Exception if something goes wrong
     */
    public void init() throws Exception {
        databaseTester.onSetup();
    }

    /**
     * Cleans the database from test data after each test case.
     *
     * @throws Exception if something goes wrong
     */
    public void clean() throws Exception {
        databaseTester.onTearDown();
    }

    /**
     * Change the operation performed before executing each test.
     * <p>
     * The default setup operation is <code>DatabaseOperation.CLEAN_INSERT</code>.
     *
     * @param setUpOperation the new setup operation to use
     */
    public void setSetUpOperation(DatabaseOperation setUpOperation) {
        databaseTester.setSetUpOperation(setUpOperation);
    }

    /**
     * Change the operation performed after executing each test.
     * <p>
     * The default is to perform no cleanup after the test.
     *
     * @param tearDownOperation the new teardown operation to use
     */
    public void setTearDownOperation(DatabaseOperation tearDownOperation) {
        databaseTester.setTearDownOperation(tearDownOperation);
    }
}
