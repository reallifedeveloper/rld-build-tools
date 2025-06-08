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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Generates a DTD for DbUnit XML files used to populate the test database.
 *
 * @author RealLifeDeveloper
 *
 */
public class DbUnitDtdGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(DbUnitDtdGenerator.class);

    private final ApplicationContext applicationContext;

    /**
     * Creates a new {@code DbUnitDtdGenerator} using the given Spring application context. It reads all tables in the database pointed to
     * by a data source from the application context and generates a DTD matching that database.
     * <p>
     * The application context should define at least a {@code javax.sql.DataSource} to read from. It may optionally define a
     * {@code org.dbunit.dataset.datatype.IDataTypeFactory} matching the database used.
     *
     * @param applicationContext the application context to use to find the {@code DataSource} and possibly {@code IDataTypeFactory}
     */
    public DbUnitDtdGenerator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Factory method to create a new {@code DbUnitDtdGenerator} instance using an application context constructed from a Spring XML
     * configuration file loaded from the classpath.
     *
     * @param springConfigResourceName the name of the classpath resource containing the Spring XML configuration
     *
     * @return the new {@code DbUnitDtdGenerator} instance
     */
    public static DbUnitDtdGenerator createFromSpringXmlConfiguration(String springConfigResourceName) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(springConfigResourceName);
        return new DbUnitDtdGenerator(applicationContext);
    }

    /**
     * Factory method to create a new {@code DbUnitDtdGenerator} instance using an application context constructed from a Spring
     * configuration class, i.e., a class annotated with {@code org.springframework.context.annotation.Configuration}.
     *
     * @param configClass the {@code &#064;Configuration} class
     *
     * @return the new {@code DbUnitDtdGenerator} instance
     */
    public static DbUnitDtdGenerator createFromSpringConfigurationClass(Class<?> configClass) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(configClass);
        return new DbUnitDtdGenerator(applicationContext);
    }

    /**
     * Main method that prints the generated DTD to {@code System.out}. The only argument should be either the fully-qualified name of a
     * Spring configuration class on the classpath, or the name of a Spring XML configuration file, also on the classpath.
     *
     * @param args should contain one argument, either configuration class or XML configuration file on classpath
     *
     * @throws SQLException          if a database problem occurred
     * @throws DatabaseUnitException if a DbUnit problem occurred
     */
    public static void main(String... args) throws SQLException, DatabaseUnitException {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: java %s <classpath Spring config class or XML file>".formatted(DbUnitDtdGenerator.class.getName()));
        }
        DbUnitDtdGenerator generator;
        try {
            Class<?> configClass = Class.forName(args[0]);
            generator = createFromSpringConfigurationClass(configClass);
        } catch (ClassNotFoundException e) {
            generator = createFromSpringXmlConfiguration(args[0]);
        }
        String dtd = generator.generateDtd();
        LOG.info(dtd);
    }

    /**
     * Gives the DbUnit DTD as a string.
     *
     * @return the DbUnit DTD
     *
     * @throws SQLException          if a database problem occurred
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
