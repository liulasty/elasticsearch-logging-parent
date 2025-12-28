package com.lz.sample.service;

import com.lz.sample.entry.LogEntry;
import org.springframework.stereotype.Service;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

@Service
public class LogStorageService {
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    private final String logFilePath = "application_logs.txt";

    public void addLogToQueue(LogEntry logEntry) {
        logQueue.offer(logEntry);
    }

    public void storeLogsToFile() {
        List<LogEntry> logsToWrite = new ArrayList<>();
        logQueue.drainTo(logsToWrite);

        if (!logsToWrite.isEmpty()) {
            try (FileWriter writer = new FileWriter(logFilePath, true)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                for (LogEntry log : logsToWrite) {
                    String logLine = String.format("[%s] %s - %s%n",
                            log.getTimestamp().format(formatter),
                            log.getLevel(),
                            log.getMessage());
                    writer.write(logLine);
                }
            } catch (IOException e) {
                System.err.println("Error writing logs to file: " + e.getMessage());
            }
        }
    }
}
