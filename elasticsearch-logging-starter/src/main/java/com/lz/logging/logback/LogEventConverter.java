package com.lz.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.lz.logging.model.EsLogDocument;
import org.slf4j.MDC;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author Administrator
 */
public class LogEventConverter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    public EsLogDocument convert(ILoggingEvent event) {
        EsLogDocument document = new EsLogDocument();

        // 设置基本属性
        document.setTimestamp(TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
        document.setLevel(event.getLevel().toString());
        document.setLogger(event.getLoggerName());
        document.setMessage(event.getFormattedMessage());
        document.setThread(event.getThreadName());

        // 设置异常信息
        ThrowableProxy throwableProxy = (ThrowableProxy) event.getThrowableProxy();
        if (throwableProxy != null) {
            document.setException(throwableProxy.getThrowable().getMessage());
        }

        // 设置 MDC 信息
        if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
            document.setMdc((MDC) event.getMDCPropertyMap());
        }

        return document;
    }
}