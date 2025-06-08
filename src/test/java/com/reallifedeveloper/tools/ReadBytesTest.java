package com.reallifedeveloper.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.reallifedeveloper.tools.test.LogbackTestUtil;

public class ReadBytesTest {

    @BeforeEach
    public void init() {
        LogbackTestUtil.clearLoggingEvents();
    }

    @Test
    public void logBytesFromUrl() throws Exception {
        // Given
        URL url = getClass().getResource("/dbunit/testentity.xml");

        // When
        ReadBytes.logBytesFromUrl(url);

        // Then
        List<ILoggingEvent> loggingEvents = LogbackTestUtil.getLoggingEvents();
        assertEquals(15, loggingEvents.size(), "Wrong number of logging events: ");
        String xmlHeader = "3C 3F 78 6D 6C"; // <?xml
        assertTrue(loggingEvents.get(0).getMessage().toString().startsWith(xmlHeader),
                "First logging event should start with '" + xmlHeader + "'");
    }

    @Test
    public void logBytesFromNonExistingUrl() throws Exception {
        URL url = new URI("file:///no_such_file").toURL();
        assertThrows(FileNotFoundException.class, () -> ReadBytes.logBytesFromUrl(url));
    }

    @Test
    public void logBytesFromNullUrl() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> ReadBytes.logBytesFromUrl(null));
    }

    @Test
    public void main() throws Exception {
        // Given
        URL url = getClass().getResource("/dbunit/testentity.xml");

        // When
        ReadBytes.main(url.toExternalForm());

        // Then
        List<ILoggingEvent> loggingEvents = LogbackTestUtil.getLoggingEvents();
        assertEquals(15, loggingEvents.size(), "Wrong number of logging events: ");
        String xmlHeader = "3C 3F 78 6D 6C"; // <?xml
        assertTrue(loggingEvents.get(0).getMessage().toString().startsWith(xmlHeader),
                "First logging event should start with '" + xmlHeader + "'");
    }

    @Test
    public void mainWrongNumberOfArgument() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> ReadBytes.main("foo", "bar"));
    }

}
