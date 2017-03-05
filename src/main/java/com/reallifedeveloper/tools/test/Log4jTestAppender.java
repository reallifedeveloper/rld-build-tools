package com.reallifedeveloper.tools.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A Log4j appender that keeps all logging events in a list.
 *
 * @author RealLifeDeveloper
 */
public class Log4jTestAppender extends AppenderSkeleton {

    private final List<LoggingEvent> loggingEvents = new ArrayList<>();

    @Override
    protected void append(LoggingEvent event) {
        loggingEvents.add(event);
    }

    /**
     * Gives all logging events that have been appended to this appender so far.
     *
     * @return all logging events.
     */
    public List<LoggingEvent> loggingEvents() {
        return loggingEvents;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

}
