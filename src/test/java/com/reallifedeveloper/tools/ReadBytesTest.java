package com.reallifedeveloper.tools;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.reallifedeveloper.tools.test.Log4jTestAppender;

public class ReadBytesTest {

    private Log4jTestAppender testAppender = new Log4jTestAppender();

    @Before
    public void init() {
        testAppender.setThreshold(Level.INFO);
        Logger.getRootLogger().addAppender(testAppender);
    }

    @Test
    public void logBytesFromUrl() throws Exception {
        URL url = ReadBytesTest.class.getResource("/dbunit/testentity.xml");
        ReadBytes.logBytesFromUrl(url);
        List<LoggingEvent> loggingEvents = testAppender.loggingEvents();
        Assert.assertEquals("Wrong number of logging events: ", 15, loggingEvents.size());
        String xmlHeader = "3C 3F 78 6D 6C"; // <?xml
        Assert.assertTrue("First logging event should start with '" + xmlHeader + "'",
                loggingEvents.get(0).getMessage().toString().startsWith(xmlHeader));
    }

    @Test(expected = FileNotFoundException.class)
    public void logBytesFromNonExistingUrl() throws Exception {
        URL url = new URL("file:///no_such_file");
        ReadBytes.logBytesFromUrl(url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void logBytesFromNullUrl() throws Exception {
        ReadBytes.logBytesFromUrl(null);
    }

    @Test
    public void main() throws Exception {
        URL url = ReadBytesTest.class.getResource("/dbunit/testentity.xml");
        ReadBytes.main(url.toExternalForm());
        List<LoggingEvent> loggingEvents = testAppender.loggingEvents();
        Assert.assertEquals("Wrong number of logging events: ", 15, loggingEvents.size());
        String xmlHeader = "3C 3F 78 6D 6C"; // <?xml
        Assert.assertTrue("First logging event should start with '" + xmlHeader + "'",
                loggingEvents.get(0).getMessage().toString().startsWith(xmlHeader));
    }

    @Test(expected = IllegalArgumentException.class)
    public void mainWrongNumberOfArgument() throws Exception {
        ReadBytes.main("foo", "bar");
    }
}
