package com.reallifedeveloper.tools.test.database.dbunit;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.ext.mssql.InsertIdentityOperation;
import org.dbunit.operation.DatabaseOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for DBUnit tests.
 * <p>
 * An example of how to use this class:
 *
 * <pre>
 * &#64;ExtendWith(SpringExtension.class)
 * &#64;ContextConfiguration(classes = TestConfiguration.class)
 * public class MyDbTest extends AbstractDbTest {
 *     &#64;Autowired
 *     private DataSource ds;
 *
 *     &#64;Autowired
 *     private IDataTypeFactory dataTypeFactory;
 *
 *     public MyDbTest() {
 *         super(null, "/dbunit/myschema.dtd", "/dbunit/dataset1.xml", "/dbunit/dataset2.xml");
 *     }
 *
 *     &#64;Test
 *     public void myTest() {
 *         ...
 *     }
 *
 *     &#64;Override
 *     protected DataSource getDataSource() { return ds; }
 *
 *     &#64;Override
 *     protected IDataTypeFactory getDataTypeFactory() { return dataTypeFactory; }
 * }
 * </pre>
 *
 * @author RealLifeDeveloper
 *
 */
@Transactional
public abstract class AbstractDbTest {

    /**
     * The default operation to perform before executing each test.
     */
    public static final DatabaseOperation DEFAULT_SETUP_OPERATION = InsertIdentityOperation.CLEAN_INSERT;

    /**
     * The default operation to perform after executing each test.
     */
    public static final DatabaseOperation DEFAULT_TEARDOWN_OPERATION = DatabaseOperation.NONE;

    private Optional<String> schemaName;
    private Optional<String> dataSetDtdResourceName;
    private String[] dataSetResourceNames;

    private DbTestHelper dbTestHelper;

    /**
     * Creates a new test instance, using the given schema and reading the DBUnit XML file found in the given classpath resource.
     *
     * @param schemaName          name of the database schema, or {@code null}
     * @param dataSetResourceName name of the classpath resource that contains DBUnit XML
     */
    protected AbstractDbTest(String schemaName, String dataSetResourceName) {
        this(schemaName, null, dataSetResourceName);
    }

    /**
     * Creates a new test instance, using the given schema and reading the DBUnit XML files and DTD found in the given classpath resources.
     *
     * @param schemaName             name of the database schema, or {@code null}
     * @param dataSetDtdResourceName name of the classpath resource containing the DTD for the XML, or {@code null} to not validate
     * @param dataSetResourceNames   names of classpath resources containing DBUnit XML
     */
    protected AbstractDbTest(String schemaName, @Nullable String dataSetDtdResourceName, String... dataSetResourceNames) {
        if (dataSetResourceNames == null) {
            throw new IllegalArgumentException("dataSetResourceName must not be null");
        }
        this.schemaName = Optional.ofNullable(schemaName);
        this.dataSetDtdResourceName = Optional.ofNullable(dataSetDtdResourceName);
        this.dataSetResourceNames = dataSetResourceNames.clone();
    }

    /**
     * Called before each test case to insert test data into the database.
     *
     * @throws Exception if something goes wrong
     */
    @BeforeEach
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void setUpDatabase() throws Exception {
        dbTestHelper = new DbTestHelper(getDataSource(), getDataSet(), getSchemaName(), getDataTypeFactory());
        dbTestHelper.setSetUpOperation(getSetUpOperation());
        dbTestHelper.setTearDownOperation(getTearDownOperation());
        dbTestHelper.init();
    }

    /**
     * Called after each test case to clean the database from test data.
     *
     * @throws Exception if something goes wrong
     */
    @AfterEach
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void tearDownDatabase() throws Exception {
        if (dbTestHelper != null) {
            dbTestHelper.clean();
        }
    }

    /**
     * Gives the name of the database schema. Can be {@code null}.
     *
     * @return the database schema name
     */
    protected Optional<String> getSchemaName() {
        return schemaName;
    }

    /**
     * Gives the name of the resource containing the DTD for test data files.
     *
     * @return the name of the DTD resource
     */
    protected Optional<String> getDataSetDtdResourceName() {
        return dataSetDtdResourceName;
    }

    /**
     * Gives the names of the resources containing test data.
     *
     * @return the names of the test data resources
     */
    protected String[] getDataSetResourceNames() {
        return Arrays.copyOf(dataSetResourceNames, dataSetResourceNames.length);
    }

    /**
     * Gives the data set used by DbUnit when testing. The default implementation reads XML files from the classpath resources pointed to by
     * the {@code dataSetResourceNames} property, validating using the DTD pointed to by the {@code dataSetDtdResourceName} property.
     * <p>
     * Override this method if you want to provide the DbUnit test data in some other way.
     *
     * @return the test data set
     *
     * @throws DataSetException if some resource if malformed
     * @throws IOException      if reading a resource failed
     */
    protected IDataSet getDataSet() throws DataSetException, IOException {
        String dtdResourceName = getDataSetDtdResourceName().orElse(null);
        return DbTestHelper.readDataSetFromClasspath(dtdResourceName, getDataSetResourceNames());
    }

    /**
     * Override this to point to your datasource. The datasource could, e.g., be injected by Spring.
     *
     * @return the {@code DataSource} to use
     */
    protected abstract DataSource getDataSource();

    /**
     * Gives the DbUnit {@code IDataTypeFactory} that matches the database used for testing, or {@code null} if the database type is left
     * unspecified. If left unspecified, DbUnit will probably issue a lot of warnings about "potential problems".
     * <p>
     * The default implementation returns {@code null}. Override this to return the right {@code IDataTypeFactory} for your database, e.g.,
     * {@code org.dbunit.ext.mysql.MySqlDataTypeFactory} or {@code org.dbunit.ext.oracle.Oracle10DataTypeFactory}.
     * <p>
     * To make it easy to change to use a different database, the factory could be configured by Spring and injected into your test case.
     *
     * @return the{@code IDataTypeFactory} to use
     */
    protected Optional<IDataTypeFactory> getDataTypeFactory() {
        return Optional.empty();
    }

    /**
     * Gives the operation to perform before executing each test.
     * <p>
     * The default is {@link #DEFAULT_SETUP_OPERATION}. Override this method to change this behavior.
     *
     * @return the setup operation to perform
     */
    protected DatabaseOperation getSetUpOperation() {
        return DEFAULT_SETUP_OPERATION;
    }

    /**
     * Gives the operation to perform after executing each test.
     * <p>
     * The default is {@link #DEFAULT_TEARDOWN_OPERATION}. Override this method to change this behavior.
     *
     * @return the setup operation to perform
     */
    protected DatabaseOperation getTearDownOperation() {
        return DEFAULT_TEARDOWN_OPERATION;
    }

    /**
     * Make finalize method final to avoid "Finalizer attacks" and corresponding SpotBugs warning (CT_CONSTRUCTOR_THROW).
     *
     * @see <a href="https://wiki.sei.cmu.edu/confluence/display/java/OBJ11-J.+Be+wary+of+letting+constructors+throw+exceptions">
     *      Explanation of finalizer attack</a>
     */
    @Override
    @SuppressWarnings({ "deprecation", "removal", "Finalize", "checkstyle:NoFinalizer", "PMD.EmptyFinalizer",
            "PMD.EmptyMethodInAbstractClassShouldBeAbstract" })
    protected final void finalize() throws Throwable {
        // Do nothing
    }

}
