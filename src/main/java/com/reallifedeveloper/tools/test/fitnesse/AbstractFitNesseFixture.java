package com.reallifedeveloper.tools.test.fitnesse;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Abstract base class for all FitNesse fixtures.
 *
 * @author RealLifeDeveloper
 */
public abstract class AbstractFitNesseFixture {

    /**
     * The date format used by {@link #parseDate(String)} ({@value #DATE_FORMAT}).
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private static ClassPathXmlApplicationContext applicationContext;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String springConfigurationResourceName;

    private String comment;

    /**
     * Creates a new <code>AbstractFitNesseFixture</code> object.
     *
     * @param springConfigurationResourceName name of the classpath resource that contains Spring configuration
     */
    public AbstractFitNesseFixture(String springConfigurationResourceName) {
        this.springConfigurationResourceName = springConfigurationResourceName;
    }

    /**
     * A comment that is useful as documentation in most FitNesse tests.
     * <p>
     * Any FitNesse test that uses this base class can add the column "Comment", which in most cases
     * is not used by the test, but is useful as documentation of the test case.
     *
     * @return the comment that was written for the test case
     */
    public String getComment() {
        return comment;
    }

    /**
     * Specifies a comment for the current test case.
     *
     * @param comment the comment that belongs to the test case
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Gives the Spring <code>ApplicationContext</code>, created from the Spring configuration file
     * that was specified when the object was created with {@link #AbstractFitNesseFixture(String)}.
     * <p>
     * This method never returns <code>null</code>; if it is not possible to create an
     * <code>ApplicationContext</code>, an exception is thrown.
     * <p>
     * Note that the <code>ApplicationContext</code> is managed statically, so if a class already has
     * loaded a context using this method, and another class is later used that wants to use a different
     * configuration, no reload will happen automatically. In this case, you can use the method
     * {@link #resetApplicationContext()} to reset the context and force a reload.
     *
     * @return the Spring <code>ApplicationContext</code>, never <code>null</code>
     */
    protected synchronized ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            applicationContext = new ClassPathXmlApplicationContext(springConfigurationResourceName);
        }
        return applicationContext;
    }

    /**
     * Sets the Spring <code>ApplicationContext</code> to <code>null</code>. This can be used to force
     * a reload of the context the next time the method {@link #getApplicationContext()} is called.
     */
    public static void resetApplicationContext() {
        applicationContext = null;
    }

    /**
     * Looks up a Spring bean of a given class in the <code>ApplicationContext</code>. This method never
     * returns <code>null</code>; if the bean does not exist, an exception is thrown.
     *
     * @param <T> the type of the Spring bean
     * @param beanClass the class of the Spring bean
     *
     * @return the Spring bean, never <code>null</code>
     */
    protected <T> T getBean(Class<T> beanClass) {
        return getApplicationContext().getBean(beanClass);
    }

    /**
     * Gives an <code>org.slf4j.Logger</code> that can be used by the concrete fixture classes for logging.
     *
     * @return an <code>org.slf4j.Logger</code>
     */
    protected Logger logger() {
        return logger;
    }

    /**
     * Parses a date string on the form {@value #DATE_FORMAT} and returns the corresponding
     * <code>java.util.Date</code> object.
     *
     * @param date the date string to parse, should be on the form {@value #DATE_FORMAT}
     *
     * @return the <code>java.util.Date</code> corresponding to <code>date</code>
     *
     * @throws IllegalArgumentException if <code>date</code> cannot be parsed
     */
    protected Date parseDate(String date) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unparseable date: " + date, e);
        }
    }

    /**
     * A null-safe toString method.
     *
     * @param o the object to be converted to string, can be <code>null</code>
     *
     * @return <code>null</code> if <code>o</code> is <code>null</code>, otherwise <code>o.toString()</code>
     */
    protected String toString(Object o) {
        return o == null ? null : o.toString();
    }
}
