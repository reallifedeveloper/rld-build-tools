package com.reallifedeveloper.tools.test.database.dbunit;

import java.io.StringWriter;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.xml.FlatDtdWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Creates a DTD for DbUnit XML files used to populate the test database.
 *
 * @author RealLifeDeveloper
 *
 */
public class DbUnitDtdGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(DbUnitDtdGenerator.class);

    private final ApplicationContext applicationContext;

    /**
     * Creates a new <code>DbUnitDtdGenerator</code> using the given Spring XML configuration
     * loaded from the classpath. It reads all tables in the database pointed to by a given
     * data source and generates a DTD matching that database.
     * <p>
     * The Spring configuration should define at least a <code>javax.sql.DataSource</code> to read from.
     * It may optionally define a <code>org.dbunit.dataset.datatype.IDataTypeFactory</code> matching the
     * database used.
     *
     * @param springConfigResourceName the name of the classpath resource from which to read
     * the Spring XML configuration
     */
    public DbUnitDtdGenerator(String springConfigResourceName) {
        applicationContext = new ClassPathXmlApplicationContext(springConfigResourceName);
    }

    /**
     * Main method that prints the generated DTD to <code>System.out</code>.
     *
     * @param args should contain one argument, the name of the classpath resource from
     * which to read the Spring XML configuration
     * @throws SQLException if a database problem occurred
     * @throws DatabaseUnitException if a DbUnit problem occurred
     */
    public static void main(String... args) throws SQLException, DatabaseUnitException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java "
                    + DbUnitDtdGenerator.class.getName()
                    + " <classpath spring config>");
        }
        DbUnitDtdGenerator generator = new DbUnitDtdGenerator(args[0]);
        String dtd = generator.generateDtd();
        LOG.info(dtd);
    }

    /**
     * Gives the DbUnit DTD as a string.
     *
     * @return the DbUnit DTD
     *
     * @throws SQLException if a database problem occurred
     * @throws DatabaseUnitException if a DbUnit problem occurred
     */
    public String generateDtd() throws SQLException, DatabaseUnitException {
        IDatabaseConnection connection = new DatabaseConnection(getDataSource().getConnection());
        if (getDataTypeFactory() != null) {
            connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, getDataTypeFactory());
        }
        IDataSet dataSet = connection.createDataSet();
        StringWriter stringWriter = new StringWriter();
        FlatDtdWriter datasetWriter = new FlatDtdWriter(stringWriter);
        datasetWriter.setContentModel(FlatDtdWriter.SEQUENCE);
        datasetWriter.write(dataSet);
        return stringWriter.toString();
    }

    private DataSource getDataSource() {
        return applicationContext.getBean(DataSource.class);
    }

    private IDataTypeFactory getDataTypeFactory() {
        IDataTypeFactory dataTypeFactory = null;
        try {
            dataTypeFactory = applicationContext.getBean(IDataTypeFactory.class);
        } catch (NoSuchBeanDefinitionException e) {
            LOG.warn(e.getMessage());
        }
        return dataTypeFactory;
    }
}
