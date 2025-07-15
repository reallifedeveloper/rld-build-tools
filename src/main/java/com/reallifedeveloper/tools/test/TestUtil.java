package com.reallifedeveloper.tools.test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.checkerframework.checker.nullness.qual.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    @SuppressFBWarnings(value = "UNENCRYPTED_SERVER_SOCKET", justification = "Server socket only created temporarily to find free port")
    public static int findFreePort() throws IOException {
        try (ServerSocket server = new ServerSocket(0)) {
            return server.getLocalPort();
        }
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
     * Parses a date and time string on the form {@value #DATE_TIME_FORMAT} and returns the corresonding {@code java.util.Date} object.
     *
     * @param dateTime the date+time string to parse, should be on the form {@value #DATE_TIME_FORMAT}
     *
     * @return the {@code java.util.Date} corresponding to {@code dateTime}
     *
     * @throws IllegalArgumentException if {@code dateTime} cannot be parsed
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
     * @param s        the string to write
     * @param filename the name of the file to write to
     * @param charset  the character set to use, e.g., {@code java.nio.charset.StandardCharsets.UTF_8}
     *
     * @throws IOException if writing to the file failed
     */
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Use at your own risk")
    public static void writeToFile(String s, String filename, Charset charset) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename), charset)) {
            writer.write(s);
        }
    }

    /**
     * Reads a string from a classpath resource, which is assumed to be UTF-8 encoded text.
     *
     * @param resourceName the name of the classpath resource to read
     *
     * @return a string representation of the classpath resource {@code resourceName}
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
                    sb.append(s.nextLine()).append(System.lineSeparator());
                }
                return sb.toString();
            }
        }
    }

    /**
     * Injects a value into an object's field, which may be private.
     *
     * @param obj       the object in which to inject the value
     * @param fieldName the name of the field
     * @param value     the value to inject
     *
     * @throws IllegalArgumentException if {@code obj} or {@code fieldName} is {@code null}
     * @throws IllegalStateException    if reflecction failure
     */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    public static void injectField(Object obj, String fieldName, Object value) {
        try {
            Field field = getField(obj, fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Error injecting " + value + " into field " + fieldName + " of object " + obj, e);
        }
    }

    /**
     * Gives the value of an object's field, which may be private.
     *
     * @param obj       the object containing the field
     * @param fieldName the name of the field
     *
     * @return the value of the field {@code fieldName} in the object {@code obj}
     *
     * @throws IllegalArgumentException if {@code obj} or {@code fieldName} is {@code null}
     * @throws IllegalStateException    if reflection failure
     */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    public static @Nullable Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = getField(obj, fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Error getting value of field " + fieldName + " of object " + obj, e);
        }
    }

    private static Field getField(Object obj, String fieldName) throws NoSuchFieldException {
        if (obj == null || fieldName == null) {
            throw new IllegalArgumentException("Arguments must not be null: obj=%s, fieldName=%s".formatted(obj, fieldName));
        }
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
