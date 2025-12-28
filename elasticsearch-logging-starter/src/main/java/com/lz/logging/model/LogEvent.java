package com.lz.logging.model;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

import java.time.Instant;
import java.util.Map;

public class LogEvent {

    private final ILoggingEvent event;
    private final Instant timestamp;
    private final Level level;
    private final String threadName;
    private final String loggerName;
    private final String formattedMessage;
    private final Map<String, String> mdcPropertyMap;
    private final IThrowableProxy throwableProxy;
    private final Object[] argumentArray;

    public LogEvent(ILoggingEvent event) {
        this.event = event;
        this.timestamp = Instant.ofEpochMilli(event.getTimeStamp());
        this.level = event.getLevel();
        this.threadName = event.getThreadName();
        this.loggerName = event.getLoggerName();
        this.formattedMessage = event.getFormattedMessage();
        this.mdcPropertyMap = event.getMDCPropertyMap();
        this.throwableProxy = event.getThrowableProxy();
        this.argumentArray = event.getArgumentArray();
    }

    // Getters
    public Instant getTimestamp() {
        return timestamp;
    }

    public Level getLevel() {
        return level;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    public Map<String, String> getMdcPropertyMap() {
        return mdcPropertyMap;
    }

    public IThrowableProxy getThrowableProxy() {
        return throwableProxy;
    }

    public Object[] getArgumentArray() {
        return argumentArray;
    }

    public ILoggingEvent getEvent() {
        return event;
    }
}
