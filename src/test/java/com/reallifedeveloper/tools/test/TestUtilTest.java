package com.reallifedeveloper.tools.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtilTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestUtilTest.class);

    @Test
    @SuppressWarnings("try")
    public void findFreePort() throws Exception {
        LOG.info("foo");
        int port = TestUtil.findFreePort();
        assertTrue(port > 1024, "Free port should be > 1024");
        try (Socket socket = new Socket("localhost", port)) {
            fail("Connecting to localhost on port " + port + " should fail");
        } catch (ConnectException e) {
            // OK
        }
    }

    @Test
    public void parseDate() {
        Date date = TestUtil.parseDate("2014-12-31");
        verifyDate(date, 2014, Calendar.DECEMBER, 31, 0, 0, 0);
    }

    @Test
    public void parseMalformedDate() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.parseDate("foo"));
        assertEquals("Unparseable date: foo", e.getMessage());
    }

    @Test
    @SuppressWarnings("NullAway")
    public void parseNullDate() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.parseDate(null));
        assertEquals("date must not be null", e.getMessage());
    }

    @Test
    public void parseDateTime() {
        Date date = TestUtil.parseDateTime("2014-12-31 17:52:30");
        verifyDate(date, 2014, Calendar.DECEMBER, 31, 17, 52, 30);
    }

    @Test
    public void parseMalformedDateTime() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.parseDateTime("foo"));
        assertEquals("Unparseable date/time: foo", e.getMessage());
    }

    @Test
    @SuppressWarnings("NullAway")
    public void parseNullDateTime() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.parseDateTime(null));
        assertEquals(e.getMessage(), "dateTime must not be null");
    }

    private void verifyDate(Date date, int year, int month, int dayOfMonth, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        assertEquals(year, calendar.get(Calendar.YEAR), "Wrong year: ");
        assertEquals(month, calendar.get(Calendar.MONTH), "Wrong month: ");
        assertEquals(dayOfMonth, calendar.get(Calendar.DAY_OF_MONTH), "Wrong day: ");
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY), "Wrong hour: ");
        assertEquals(minute, calendar.get(Calendar.MINUTE), "Wrong minute: ");
        assertEquals(second, calendar.get(Calendar.SECOND), "Wrong second: ");
    }

    @Test
    public void writeToFile() throws Exception {
        String filename = Paths.get(System.getProperty("java.io.tmpdir"), "junitWriteToFile.txt").toString();
        try {
            String s = "foo" + System.lineSeparator() + "bar" + System.lineSeparator() + "\u03B1\u03B2\u03B3";
            TestUtil.writeToFile(s, filename, StandardCharsets.UTF_8);
            List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
            assertEquals(3, lines.size(), "Wrong number of lines in file: ");
            assertEquals("foo", lines.get(0), "Wrong content in line 1: ");
            assertEquals("bar", lines.get(1), "Wrong content in line 2: ");
            assertEquals("\u03B1\u03B2\u03B3", lines.get(2), "Wrong content in line 3: ");
        } finally {
            File file = new File(filename);
            file.delete();
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    public void writeToNonExistingFile() throws Exception {
        Exception e = assertThrows(NoSuchFileException.class,
                () -> TestUtil.writeToFile("foo", "noSuchDirectory/test.txt", StandardCharsets.UTF_8));
        assertTrue(e.getMessage().contains("noSuchDirectory"));
    }

    @Test
    public void writeToReadOnlyFile() throws Exception {
        String filename = Paths.get(System.getProperty("java.io.tmpdir"), "junitWriteToReadOnlyFile.txt").toString();
        File file = new File(filename);
        file.createNewFile();
        file.setWritable(false);
        if (Files.isWritable(file.toPath())) {
            // We are probably running as root -- skip this test.
            return;
        }
        try {
            assertThrows(AccessDeniedException.class, () -> TestUtil.writeToFile("foo", filename, StandardCharsets.UTF_8));
        } finally {
            file.delete();
        }
    }

    @Test
    public void readResource() throws Exception {
        String s = TestUtil.readResource("dbunit/testentity.xml");
        assertNotNull(s, "String should not be null");
        String[] lines = s.split(System.lineSeparator(), -1);
        assertEquals(10, lines.length, "Wrong number of lines: ");
        assertEquals("</dataset>", lines[8], "Wrong content in line 9: ");
    }

    @Test
    @SuppressWarnings("NullAway")
    public void readNonExistingResource() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.readResource("no such resource"));
        assertTrue(e.getMessage().contains("Resource not found: no such resource"));
    }

    @Test
    @SuppressWarnings("NullAway")
    public void readResourceNullResourceName() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.readResource(null));
        assertTrue(e.getMessage().contains("resourceName must not be null"));
    }

    @Test
    public void injectField() {
        Foo foo = new Foo();
        assertEquals("foo", foo.s(), "Wrong value of s: ");
        TestUtil.injectField(foo, "s", "bar");
        assertEquals("bar", foo.s(), "Wrong value of s: ");
    }

    @Test
    public void injectFieldNullValue() {
        Foo foo = new Foo();
        TestUtil.injectField(foo, "s", null);
        assertNull(foo.s(), "Expected foo.s to be null after field injection");
    }

    @Test
    public void injectFieldInSubClass() {
        Bar bar = new Bar();
        assertEquals("foo", bar.s(), "Wrong value of s: ");
        TestUtil.injectField(bar, "s", "bar");
        assertEquals("bar", bar.s(), "Wrong value of s: ");
    }

    @Test
    @SuppressWarnings("NullAway")
    public void injectNonExistingField() {
        Object obj = new Object();
        Exception e = assertThrows(IllegalStateException.class, () -> TestUtil.injectField(obj, "noSuchField", "foo"));
        assertTrue(e.getMessage().contains("Error injecting foo into field noSuchField of object " + obj));
    }

    @Test
    @SuppressWarnings("NullAway")
    public void injectFieldInNullObject() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.injectField("foo", null, "bar"));
        assertEquals("Arguments must not be null: obj=foo, fieldName=null", e.getMessage());
    }

    @Test
    @SuppressWarnings("NullAway")
    public void injectFieldWithNullFieldName() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.injectField(null, "foo", "bar"));
        assertEquals("Arguments must not be null: obj=null, fieldName=foo", e.getMessage());
    }

    @Test
    public void getFieldValue() throws Exception {
        Foo foo = new Foo();
        assertEquals("foo", TestUtil.getFieldValue(foo, "s"));
    }

    @Test
    public void getValueOfNonExistingField() {
        Foo foo = new Foo();
        Exception e = assertThrows(IllegalStateException.class, () -> TestUtil.getFieldValue(foo, "noSuchField"));
        assertEquals("Error getting value of field noSuchField of object " + foo, e.getMessage());
    }

    @Test
    @SuppressWarnings("NullAway")
    public void getValueOfNullObject() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.getFieldValue(null, "foo"));
        assertEquals("Arguments must not be null: obj=null, fieldName=foo", e.getMessage());
    }

    @Test
    @SuppressWarnings("NullAway")
    public void getValueOfNullFieldName() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> TestUtil.getFieldValue("foo", null));
        assertEquals("Arguments must not be null: obj=foo, fieldName=null", e.getMessage());
    }

    private static class Foo {
        private String s = "foo";

        String s() {
            return s;
        }
    }

    private static class Bar extends Foo {

    }
}
