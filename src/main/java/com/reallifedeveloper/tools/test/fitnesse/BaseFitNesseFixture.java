package com.reallifedeveloper.tools.test.fitnesse;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Base class for all FitNesse fixtures.
 *
 * @author RealLifeDeveloper
 */
public class BaseFitNesseFixture {

    /**
     * The date format used by {@link #parseDate(String)} ({@value #DATE_FORMAT}).
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final ThreadLocal<ApplicationContext> APPLICATION_CONTEXT = ThreadLocal
            .withInitial(() -> new GenericApplicationContext());

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private @Nullable String comment;

    /**
     * Creates a new {@code BaseFitNesseFixture} object using the given Spring application context. The application context is associated
     * with the current thread, so this fixture instance is assumed to be used by a single thread.
     *
     * @param applicationContext the Spring application context to use, must not be {@code null}
     */
    public BaseFitNesseFixture(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            throw new IllegalArgumentException("applicationContext must not be null");
        }
        APPLICATION_CONTEXT.set(applicationContext);
    }

    /**
     * Creates a new {@code BaseFitNesseFixture} object reading the Spring application context from the given classpath resource, which
     * should be a Spring XML configuration file.
     *
     * @param springConfigurationResourceName name of the classpath resource containing Spring XML configuration, must not be {@code null}
     */
    public BaseFitNesseFixture(String springConfigurationResourceName) {
        this(initApplicationContext(springConfigurationResourceName));
    }

    private static ApplicationContext initApplicationContext(String springConfigurationResourceName) {
        if (springConfigurationResourceName == null) {
            throw new IllegalArgumentException("springConfigurationResourceName must not be null");
        }
        return new ClassPathXmlApplicationContext(springConfigurationResourceName);
    }

    /**
     * A comment that is useful as documentation in most FitNesse tests.
     * <p>
     * Any FitNesse test that uses this base class can add the column "Comment", which in most cases is not used by the test, but is useful
     * as documentation of the test case.
     *
     * @return the comment that was written for the test case
     */
    public @Nullable String getComment() {
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
     * Gives the Spring {@code ApplicationContext} associated with this thread.
     * <p>
     * This method never returns {@code null}; if it is not possible to create an {@code ApplicationContext}, an exception is thrown.
     * <p>
     * Note that the {@code ApplicationContext} is managed statically, so if a class already has loaded a context using this method, and
     * another class is later used that wants to use a different configuration, no reload will happen automatically. In this case, you can
     * use the method {@link #resetApplicationContext()} to reset the context and force a reload.
     *
     * @return the Spring {@code ApplicationContext}, never {@code null}
     */
    protected ApplicationContext getApplicationContext() {
        return APPLICATION_CONTEXT.get();
    }

    /**
     * Sets the Spring {@code ApplicationContext} to {@code null}. This can be used to force a reload of the context the next time the
     * method {@link #getApplicationContext()} is called.
     */
    public static void resetApplicationContext() {
        // applicationContext = null;
    }

    /**
     * Looks up a Spring bean of a given class in the {@code ApplicationContext}. This method never returns {@code null}; if the bean does
     * not exist, an exception is thrown.
     *
     * @param <T>       the type of the Spring bean
     * @param beanClass the class of the Spring bean
     *
     * @return the Spring bean, never {@code null}
     */
    protected <T> T getBean(Class<T> beanClass) {
        return getApplicationContext().getBean(beanClass);
    }

    /**
     * Gives an {@code org.slf4j.Logger} that can be used by the concrete fixture classes for logging.
     *
     * @return an {@code org.slf4j.Logger}
     */
    protected Logger logger() {
        return logger;
    }

    /**
     * Parses a date string on the form {@value #DATE_FORMAT} and returns the corresponding {@code java.util.Date} object.
     *
     * @param date the date string to parse, should be on the form {@value #DATE_FORMAT}
     *
     * @return the {@code java.util.Date} corresponding to {@code date}
     *
     * @throws IllegalArgumentException if {@code date} cannot be parsed
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
     * @param o the object to be converted to string, can be {@code null}
     *
     * @return {@code null} if {@code o} is {@code null}, otherwise {@code o.toString()}
     */
    protected @Nullable String toString(Object o) {
        return o == null ? null : o.toString();
    }

    /**
     * Make finalize method final to avoid "Finalizer attacks" and corresponding SpotBugs warning (CT_CONSTRUCTOR_THROW).
     *
     * @see <a href="https://wiki.sei.cmu.edu/confluence/display/java/OBJ11-J.+Be+wary+of+letting+constructors+throw+exceptions">
     *      Explanation of finalizer attack</a>
     */
    @Override
    @SuppressWarnings({ "checkstyle:NoFinalizer", "PMD.EmptyFinalizer", "PMD.EmptyMethodInAbstractClassShouldBeAbstract" })
    protected final void finalize() throws Throwable {
        // Do nothing
    }
}
