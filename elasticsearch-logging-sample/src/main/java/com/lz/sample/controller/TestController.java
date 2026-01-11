package com.lz.sample.controller;

import com.lz.sample.entry.LogEntry;
import com.lz.sample.service.LogStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Administrator
 */
@RestController
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private LogStorageService logStorageService;

    @PostMapping("/log")
    public String addLog(String level, String message) {
        // 1. 打印日志到 SLF4J，这将触发 Elasticsearch Appender
        logToSlf4j(level, message);

        // 2. 保留原有逻辑（写入本地文件）
        LogEntry logEntry = new LogEntry(level, message);
        logStorageService.addLogToQueue(logEntry);

        return "Log added to queue and sent to Elasticsearch";
    }


    @PostMapping("/logNull")
    public String addLog() {
        String level = "test";
        String message = "This is a null test log";
        
        logToSlf4j(level, message);
        
        LogEntry logEntry = new LogEntry(level, message);
        logStorageService.addLogToQueue(logEntry);
        return "Log added to queue";
    }
    
    @GetMapping("/logNull")
    public String adLog() {
        String level = "test";
        String message = "This is a null test log (GET)";
        
        logToSlf4j(level, message);
        
        LogEntry logEntry = new LogEntry(level, message);
        logStorageService.addLogToQueue(logEntry);
        return "Log added to queue";
    }

    private void logToSlf4j(String level, String message) {
        if (level == null) {
            logger.info(message);
            return;
        }
        switch (level.toLowerCase()) {
            case "debug":
                logger.debug(message);
                break;
            case "warn":
                logger.warn(message);
                break;
            case "error":
                logger.error(message);
                break;
            case "trace":
                logger.trace(message);
                break;
            default:
                logger.info(message);
        }
    }
}
