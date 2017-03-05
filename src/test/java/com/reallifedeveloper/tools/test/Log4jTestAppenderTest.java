package com.reallifedeveloper.tools.test;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Log4jTestAppenderTest {

    private static final Logger LOG4JLOG = Logger.getLogger(Log4jTestAppenderTest.class);

    private Log4jTestAppender testAppender = new Log4jTestAppender();

    @Before
    public void init() {
        testAppender.setThreshold(Level.INFO);
        Logger.getRootLogger().addAppender(testAppender);
    }

    @Test
    public void append() {
        LOG4JLOG.error("error");
        LOG4JLOG.warn("warn");
        LOG4JLOG.info("info");
        LOG4JLOG.debug("debug");
        LOG4JLOG.trace("trace");
        verifyLoggingEvents("error", "warn", "info");
    }

    @Test
    public void close() {
        LOG4JLOG.error("error");
        testAppender.close();
        LOG4JLOG.error("warn");
        verifyLoggingEvents("error", "warn");
    }

    private void verifyLoggingEvents(String... messages) {
        Assert.assertEquals("Unexpected number of logging events: ", messages.length,
                testAppender.loggingEvents().size());
        for (int i = 0; i < messages.length; i++) {
            Assert.assertEquals("Unexpected logging event: ", messages[i],
                    testAppender.loggingEvents().get(i).getMessage());
        }
    }

    @Test
    public void requiresLayout() {
        Assert.assertFalse("Log4jTestAppender should not require layout", testAppender.requiresLayout());
    }
}
