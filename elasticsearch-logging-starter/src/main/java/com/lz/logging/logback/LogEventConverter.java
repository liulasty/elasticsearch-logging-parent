package com.lz.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.lz.logging.config.ElasticsearchLoggingProperties;
import com.lz.logging.core.model.EsLogDocument;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * @author Administrator
 */
public class LogEventConverter {

    private final DateTimeFormatter timestampFormatter;
    private final String applicationName;
    private final String environment;

    public LogEventConverter(ElasticsearchLoggingProperties properties) {
        ZoneId zoneId = ZoneId.systemDefault();
        if (properties != null && properties.getZoneId() != null) {
            try {
                zoneId = ZoneId.of(properties.getZoneId());
            } catch (Exception ignored) {
            }
        }
        this.timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(zoneId);
        this.applicationName = properties != null ? properties.getApplicationName() : null;
        this.environment = properties != null ? properties.getEnvironment() : null;
    }

    public LogEventConverter() {
        this(null);
    }

    public EsLogDocument convert(ILoggingEvent event) {
        EsLogDocument document = new EsLogDocument();

        // 设置基本属性
        document.setTimestamp(timestampFormatter.format(Instant.ofEpochMilli(event.getTimeStamp())));
        document.setLevel(event.getLevel().toString());
        document.setLogger(event.getLoggerName());
        document.setMessage(event.getFormattedMessage());
        document.setThread(event.getThreadName());

        if (applicationName != null) {
            document.setApplication(applicationName);
        }
        if (environment != null) {
            document.setEnvironment(environment);
        }

        // 设置异常信息
        ThrowableProxy throwableProxy = (ThrowableProxy) event.getThrowableProxy();
        if (throwableProxy != null) {
            document.setException(throwableProxy.getThrowable().getMessage());
        }

        // 设置 MDC 信息
        Map<String, String> mdcMap = event.getMDCPropertyMap();
        if (mdcMap != null && !mdcMap.isEmpty()) {
            document.setMdc(mdcMap);

            // 提取 TraceId 和 SpanId
            if (mdcMap.containsKey("traceId")) {
                document.setTraceId(mdcMap.get("traceId"));
            } else if (mdcMap.containsKey("trace_id")) {
                document.setTraceId(mdcMap.get("trace_id"));
            }

            if (mdcMap.containsKey("spanId")) {
                document.setSpanId(mdcMap.get("spanId"));
            } else if (mdcMap.containsKey("span_id")) {
                document.setSpanId(mdcMap.get("span_id"));
            }
        }

        return document;
    }
}
