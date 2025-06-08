package com.reallifedeveloper.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to read a file and display the content as bytes.
 *
 * @author RealLifeDeveloper
 */
public final class ReadBytes {

    private static final int BLOCK_SIZE = 16;

    private static final String NEWLINE = "\\R";

    private static final Logger LOG = LoggerFactory.getLogger(ReadBytes.class);

    /**
     * This is a utility class with only static methods, so we hide the only constructor.
     */
    private ReadBytes() {
    }

    /**
     * Main method to read bytes from a URL and log the result.
     *
     * @param args a string array that should contain one element, the URL to read from
     *
     * @throws IOException        if reading from the URL failed
     * @throws URISyntaxException if the provided URL is malformed
     */
    public static void main(String... args) throws IOException, URISyntaxException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java " + ReadBytes.class.getName() + " <url>");
        }
        URL url = new URI(args[0]).toURL();
        logBytesFromUrl(url);
    }

    /**
     * Reads bytes from the given URL and logs them at info level, both as hexadecimal byte values and as an ASCII string.
     *
     * @param url the URL to read from
     *
     * @throws IOException if reading from {@code url} failed
     */
    public static void logBytesFromUrl(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        byte[] data = new byte[BLOCK_SIZE];
        try (InputStream in = url.openConnection().getInputStream()) {
            int bytesRead;
            while ((bytesRead = in.read(data)) != -1) {
                logBytes(data, bytesRead);
            }
        }
    }

    private static void logBytes(byte[] data, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            byte b = data[i];
            sb.append(String.format("%02X ", b));
        }
        if (n < BLOCK_SIZE) {
            for (int i = n; i < BLOCK_SIZE; i++) {
                sb.append("   ");
            }
        }
        sb.append(": ").append(new String(data, StandardCharsets.US_ASCII).replaceAll(NEWLINE, " "));
        LOG.info(sb.toString());
    }
}
