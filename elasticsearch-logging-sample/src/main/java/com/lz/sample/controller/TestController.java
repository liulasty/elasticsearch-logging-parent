package com.lz.sample.controller;

import com.lz.sample.entry.LogEntry;
import com.lz.sample.service.LogStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Administrator
 */
@RestController
public class TestController {

    @Autowired
    private LogStorageService logStorageService;

    @PostMapping("/log")
    public String addLog(String level, String message) {
        LogEntry logEntry = new LogEntry(level, message);
        logStorageService.addLogToQueue(logEntry);
        return "Log added to queue";
    }


    @PostMapping("/logNull")
    public String addLog() {
        String level = "test";
        String message = "";
        LogEntry logEntry = new LogEntry(level, message);
        logStorageService.addLogToQueue(logEntry);
        return "Log added to queue";
    }
}
