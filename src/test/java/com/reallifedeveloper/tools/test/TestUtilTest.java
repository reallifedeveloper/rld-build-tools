package com.reallifedeveloper.tools.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestUtilTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void findFreePort() throws Exception {
        int port = TestUtil.findFreePort();
        Assert.assertTrue("Free port should be > 1024", port > 1024);
        try {
            new Socket("localhost", port);
            Assert.fail("Connecting to localhost on port " + port + " should fail");
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
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Unparseable date: foo");
        TestUtil.parseDate("foo");
    }

    @Test
    public void parseNullDate() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("date must not be null");
        TestUtil.parseDate(null);
    }

    @Test
    public void parseDateTime() {
        Date date = TestUtil.parseDateTime("2014-12-31 17:52:30");
        verifyDate(date, 2014, Calendar.DECEMBER, 31, 17, 52, 30);
    }

    @Test
    public void parseMalformedDateTime() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Unparseable date/time: foo");
        TestUtil.parseDateTime("foo");
    }

    @Test
    public void parseNullDateTime() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("dateTime must not be null");
        TestUtil.parseDateTime(null);
    }

    private void verifyDate(Date date, int year, int month, int dayOfMonth, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Assert.assertEquals("Wrong year: ", year, calendar.get(Calendar.YEAR));
        Assert.assertEquals("Wrong month: ", month, calendar.get(Calendar.MONTH));
        Assert.assertEquals("Wrong day: ", dayOfMonth, calendar.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals("Wrong hour: ", hour, calendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals("Wrong minute: ", minute, calendar.get(Calendar.MINUTE));
        Assert.assertEquals("Wrong second: ", second, calendar.get(Calendar.SECOND));
    }

    @Test
    public void writeToFile() throws Exception {
        String filename = System.getProperty("java.io.tmpdir") + "junitWriteToFile.txt";
        String s = "foo" + System.lineSeparator() + "bar" + System.lineSeparator() + "\u03B1\u03B2\u03B3";
        TestUtil.writeToFile(s, filename, "UTF-8");
        List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
        Assert.assertEquals("Wrong number of lines in file: ", 3, lines.size());
        Assert.assertEquals("Wrong content in line 1: ", "foo", lines.get(0));
        Assert.assertEquals("Wrong content in line 2: ", "bar", lines.get(1));
        Assert.assertEquals("Wrong content in line 3: ", "\u03B1\u03B2\u03B3", lines.get(2));
    }

    @Test
    public void writeToReadOnlyFile() throws Exception {
        String filename = System.getProperty("java.io.tmpdir") + "junitWriteToReadOnlyFile.txt";
        File file = new File(filename);
        file.createNewFile();
        file.setWritable(false);
        try {
            expectedException.expect(FileNotFoundException.class);
            TestUtil.writeToFile("foo", filename, "UTF-8");
        } finally {
            file.delete();
        }
    }

    @Test
    public void writeToFileIllegalCharSetName() throws Exception {
        String filename = System.getProperty("java.io.tmpdir") + "junitWriteToFile.txt";
        expectedException.expect(UnsupportedEncodingException.class);
        expectedException.expectMessage("bar");
        TestUtil.writeToFile("foo", filename, "bar");
    }

    @Test
    public void writeToNonExistingDirectory() throws Exception {
        String filename = "/no_such_directory/junitWriteToFile.txt";
        expectedException.expect(FileNotFoundException.class);
        TestUtil.writeToFile("foo", filename, "UTF-8");
    }

    @Test
    public void readResource() throws Exception {
        String s = TestUtil.readResource("META-INF/hsql-test.properties");
        Assert.assertNotNull("String should not be null", s);
        String[] lines = s.split(System.lineSeparator());
        Assert.assertEquals("Wrong number of lines: ", 12, lines.length);
        Assert.assertEquals("Wrong content in line 10: ", "jpa.showSql=false", lines[9]);
    }

    @Test
    public void readNonExistingResource() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Resource not found: no such resource");
        TestUtil.readResource("no such resource");
    }

    @Test
    public void readResourceNullResourceName() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("resourceName must not be null");
        TestUtil.readResource(null);
    }

    @Test
    public void injectField() {
        Foo foo = new Foo();
        Assert.assertEquals("Wrong value of s: ", "foo", foo.s());
        TestUtil.injectField(foo, "s", "bar");
        Assert.assertEquals("Wrong value of s: ", "bar", foo.s());
    }

    @Test
    public void injectFieldInSubClass() {
        Bar bar = new Bar();
        Assert.assertEquals("Wrong value of s: ", "foo", bar.s());
        TestUtil.injectField(bar, "s", "bar");
        Assert.assertEquals("Wrong value of s: ", "bar", bar.s());
    }

    @Test
    public void injectNonExistingField() {
        Object obj = new Object();
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Error injecting bar into field foo of object " + obj);
        TestUtil.injectField(obj, "foo", "bar");
    }

    private static class Foo {
        private String s = "foo";

        public String s() {
            return s;
        }
    }

    private static class Bar extends Foo {

    }
}
