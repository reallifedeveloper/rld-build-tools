package com.reallifedeveloper.tools.test;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;

public final class LogbackTestUtil {

    /**
     * The name of the {@code ListAppender} in the Logback configuration, logback-test.xml.
     */
    public static final String LIST_APPENDER_NAME = "LIST";

    /**
     * Hide the only constructor since this a utility class.
     */
    private LogbackTestUtil() {
    }

    /**
     * Gives a list of all logging events generated since startup or the latest call to {@link #clearLoggingEvents()}.
     * <p>
     * This assumes that we are using Logback for logging, with a {@code ch.qos.logback.core.read.ListAppender} configured for the root
     * logger.
     *
     * @return a list of logging events
     */
    public static List<ILoggingEvent> getLoggingEvents() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Appender<ILoggingEvent> listAppender = rootLogger.getAppender(LIST_APPENDER_NAME);
        if (listAppender == null) {
            throw new IllegalStateException("Configuration problem: No Logback appender named '" + LIST_APPENDER_NAME + "' found");
        }
        if (!(listAppender instanceof ListAppender)) {
            throw new IllegalStateException("Configuration problem: Logback appender named '" + LIST_APPENDER_NAME + "' is not a "
                    + ListAppender.class.getName() + " but a " + listAppender.getClass().getName());
        }
        return ((ListAppender<ILoggingEvent>) listAppender).list;
    }

    /**
     * Clears the list of logging events reteurned by {@link #getLoggingEvents()}.
     */
    public static void clearLoggingEvents() {
        getLoggingEvents().clear();
    }
}
