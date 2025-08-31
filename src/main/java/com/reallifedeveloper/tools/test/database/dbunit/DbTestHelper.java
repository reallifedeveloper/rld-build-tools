package com.reallifedeveloper.tools.test.database.dbunit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;
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
 * A helper class used by {@link AbstractDbTest}. It could be used directly by a test class that for some reason cannot inherit from
 * {@code AbstractDbTest}.
 *
 * @author RealLifeDeveloper
 *
 */
public final class DbTestHelper {

    private final IDatabaseTester databaseTester;

    /**
     * Creates a new {@code DbTestHelper}, with test data provided by the given {@code dataSet} and using the given
     * {@code dataSource} to insert it. The database schema name may be provided (can be {@code null}), and the type of database
     * can be defined using {@code dataTypeFactory} (can also be {@code null}).
     *
     * @param dataSource      the {@code DataSource} to use when inserting test data
     * @param dataSet         the DbUnit test data set to read
     * @param schemaName      optional name of the database schema
     * @param dataTypeFactory an optional {@code IDataTypeFactory}
     */
    public DbTestHelper(DataSource dataSource, IDataSet dataSet, Optional<String> schemaName, Optional<IDataTypeFactory> dataTypeFactory) {
        if (dataSource == null || dataSet == null || dataTypeFactory == null) {
            throw new IllegalArgumentException("Arguments must not be null: dataSource=" + dataSource + ", dataSet=" + dataSet
                    + ", schemaName=" + schemaName + ", dataTypeFactory=" + dataTypeFactory);
        }
        databaseTester = new DataSourceDatabaseTester(dataSource, schemaName.orElse(null)) {
            // We override this method to configure the dataTypeFactory for the connection, to avoid warnings from DbUnit.
            @Override
            public IDatabaseConnection getConnection() throws Exception {
                IDatabaseConnection conn = super.getConnection();
                dataTypeFactory.ifPresent(dtf -> {
                    conn.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, dtf);
                    conn.getConfig().setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
                });
                return conn;
            }
        };
        databaseTester.setDataSet(dataSet);
    }

    /**
     * Reads XML data set files from the classpath resources pointed to by {@code dataSetResourceNames}, optionally validating using
     * the DTD pointed to by {@code dataSetDtdResourceName} property.
     * <p>
     * The {@code dataSetDtdResourceName} parameter may be {@code null}, in which case no validation is performed.
     *
     * @param dataSetDtdResourceName the name of the resource containing the DTD for test data files, or {@code null}
     * @param dataSetResourceNames   the names of the resources containing test data
     *
     * @return the test data set
     *
     * @throws DataSetException if some resource if malformed
     * @throws IOException      if reading a resource failed
     */
    public static IDataSet readDataSetFromClasspath(@Nullable String dataSetDtdResourceName, String... dataSetResourceNames)
            throws DataSetException, IOException {
        if (dataSetResourceNames == null || dataSetResourceNames.length == 0) {
            throw new IllegalArgumentException("You must provide at least one dataSetResourceName");
        } else {
            FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
            builder.setColumnSensing(true);
            addPotentialDtd(builder, Optional.ofNullable(dataSetDtdResourceName));
            List<IDataSet> dataSets = readDataSets(builder, dataSetResourceNames);
            return new CompositeDataSet(dataSets.toArray(new IDataSet[0]));
        }
    }

    private static void addPotentialDtd(FlatXmlDataSetBuilder builder, Optional<String> dataSetDtdResourceName)
            throws DataSetException, IOException {
        if (dataSetDtdResourceName.isPresent()) {
            try (InputStream is = DbTestHelper.class.getResourceAsStream(dataSetDtdResourceName.get())) {
                if (is == null) {
                    throw new IllegalArgumentException("DTD not found on classpath: " + dataSetDtdResourceName.get());
                }
                builder.setMetaDataSetFromDtd(is);
            }
        }
    }

    private static List<IDataSet> readDataSets(FlatXmlDataSetBuilder builder, String... dataSetResourceNames)
            throws DataSetException, IOException {
        List<IDataSet> dataSets = new ArrayList<>();
        for (String dataSetResourceName : dataSetResourceNames) {
            try (InputStream is = DbTestHelper.class.getResourceAsStream(dataSetResourceName)) {
                if (is == null) {
                    throw new IllegalArgumentException("Dataset not found on classpath: " + dataSetResourceName);
                }
                dataSets.add(builder.build(is));
            }
        }
        return dataSets;
    }

    /**
     * Initializes the test data before each test case.
     *
     * @throws Exception if something goes wrong
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void init() throws Exception {
        databaseTester.onSetup();
    }

    /**
     * Cleans the database from test data after each test case.
     *
     * @throws Exception if something goes wrong
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void clean() throws Exception {
        databaseTester.onTearDown();
    }

    /**
     * Change the operation performed before executing each test.
     * <p>
     * The default setup operation is {@code DatabaseOperation.CLEAN_INSERT}.
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
