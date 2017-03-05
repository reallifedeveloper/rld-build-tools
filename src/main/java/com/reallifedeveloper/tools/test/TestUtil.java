package com.reallifedeveloper.tools.test;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * Miscellaneous utility methods that are useful when testing.
 *
 * @author RealLifeDeveloper
 */
public final class TestUtil {

    /**
     * The date format used by {@link #parseDate(String)} ({@value #DATE_FORMAT}).
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * The date+time format used by {@link #parseDateTime(String)} ({@value #DATE_TIME_FORMAT}).
     */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * This is a utility class with only static methods, so we hide the only constructor.
     */
    private TestUtil() {
    }

    /**
     * Gives a port number on the local machine that no server process is listening to.
     *
     * @return a free port number
     *
     * @throws IOException if an I/O error occurs when trying to open a socket
     */
    public static int findFreePort() throws IOException {
        int port = -1;
        try (ServerSocket server = new ServerSocket(0)) {
            port = server.getLocalPort();
        }
        return port;
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
    public static Date parseDate(String date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unparseable date: " + date, e);
        }
    }

    /**
     * Parses a date and time string on the form {@value #DATE_TIME_FORMAT} and returns the
     * corresonding <code>java.util.Date</code> object.
     *
     * @param dateTime the date+time string to parse, should be on the form {@value #DATE_TIME_FORMAT}
     *
     * @return the <code>java.util.Date</code> corresponding to <code>dateTime</code>
     *
     * @throws IllegalArgumentException if <code>dateTime</code> cannot be parsed
     */
    public static Date parseDateTime(String dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime must not be null");
        }
        DateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
        try {
            return dateFormat.parse(dateTime);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unparseable date/time: " + dateTime, e);
        }
    }

    /**
     * Writes a string to a file using the given character encoding.
     *
     * @param s the string to write
     * @param filename the name of the file to write to
     * @param charsetName the name of the character encoding to use, e.g., "UTF-8"
     *
     * @throws IOException if writing to the file failed
     */
    public static void writeToFile(String s, String filename, String charsetName) throws IOException {
        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), charsetName))) {
            writer.write(s);
        }
    }

    /**
     * Reads a string from a classpath resource, which is assumed to be UTF-8 encoded text.
     *
     * @param resourceName the name of the classpath resource to read
     *
     * @return a string representation of the classpath resource <code>resourceName</code>
     *
     * @throws IOException if reading the resource failed
     */
    public static String readResource(String resourceName) throws IOException {
        if (resourceName == null) {
            throw new IllegalArgumentException("resourceName must not be null");
        }
        StringBuilder sb = new StringBuilder();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceName);
            }
            try (Scanner s = new Scanner(is, "UTF-8")) {
                while (s.hasNextLine()) {
                    sb.append(s.nextLine());
                    sb.append(System.lineSeparator());
                }
                return sb.toString();
            }
        }
    }

    /**
     * Injects a value into an object's field, which may be private.
     *
     * @param obj the object in which to inject the value
     * @param fieldName the name of the field
     * @param value the value to inject
     *
     * @throws IllegalStateException if injection failure
     */
    public static void injectField(Object obj, String fieldName, Object value) {
        try {
            Field field = getField(obj, fieldName);
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    field.setAccessible(true);
                    return null;
                }
            });
            field.set(obj, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Error injecting " + value + " into field " + fieldName + " of object " + obj, e);
        }
    }

    private static Field getField(Object obj, String fieldName) throws NoSuchFieldException {
        Class<?> entityType = obj.getClass();
        while (entityType != null) {
            for (Field field : entityType.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    return field;
                }
            }
            entityType = entityType.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }
}
