package com.lz.sample.service;

import com.lz.sample.entry.LogEntry;
import com.lz.sample.es.SimpleEsWriter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@Slf4j
public class LogStorageService {

    private static final String DEFAULT_INDEX_NAME = "log";
    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;
    private static final int MAX_BATCH_SIZE = 5000;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final BlockingQueue<LogEntry> logQueue;
    private final SimpleEsWriter esWriter;
    private final String indexName;
    private final int batchSize;

    public void addLogToQueue(LogEntry logEntry) {
        if (logEntry == null) {
            return;
        }
        logQueue.offer(logEntry);
    }

    public LogStorageService(
            SimpleEsWriter esWriter,
            @Value("${es.log.index:" + DEFAULT_INDEX_NAME + "}") String indexName,
            @Value("${es.log.batch-size:" + DEFAULT_BATCH_SIZE + "}") int batchSize,
            @Value("${es.log.queue-capacity:" + DEFAULT_QUEUE_CAPACITY + "}") int queueCapacity
    ) {
        this.esWriter = esWriter;
        this.indexName = indexName;
        this.batchSize = normalizeBatchSize(batchSize);
        this.logQueue = new LinkedBlockingQueue<>(normalizeQueueCapacity(queueCapacity));
    }

    @Scheduled(fixedDelayString = "${es.log.flush-delay-ms:1000}")
    public void flushQueueToElasticsearch() {
        List<LogEntry> batch = new ArrayList<>(batchSize);
        logQueue.drainTo(batch, batchSize);

        if (batch.isEmpty()) {
            return;
        }

        List<Map<String, Object>> docs = batch.stream()
                .map(this::toEsDocument)
                .collect(Collectors.toList());

        boolean ok = esWriter.bulkWrite(indexName, docs);
        if (!ok) {
            log.warn("批量写入ES失败，本次批量大小: {}", docs.size());
        }
    }

    private Map<String, Object> toEsDocument(LogEntry entry) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("@timestamp", formatTimestamp(entry));
        doc.put("level", entry.getLevel());
        doc.put("message", entry.getMessage());
        doc.put("app_name", "es-log-demo");
        return doc;
    }

    private String formatTimestamp(LogEntry entry) {
        if (entry.getTimestamp() == null) {
            return TIMESTAMP_FORMATTER.format(Instant.now());
        }
        Instant instant = entry.getTimestamp().atZone(ZoneId.systemDefault()).toInstant();
        return TIMESTAMP_FORMATTER.format(instant);
    }

    private int normalizeBatchSize(int batchSize) {
        if (batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }
        return Math.min(batchSize, MAX_BATCH_SIZE);
    }

    private int normalizeQueueCapacity(int queueCapacity) {
        if (queueCapacity <= 0) {
            return DEFAULT_QUEUE_CAPACITY;
        }
        return queueCapacity;
    }

    /**
     * 分页查询ES中的文档
     *
     * @param indexName 索引名称
     * @param query 查询条件，可以为null表示查询所有
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 查询结果文档列表
     */
    public List<Map<String, Object>> searchWithPagination(String indexName, QueryBuilder query, int page, int size) {
        try {
            log.info("开始分页查询，索引: {}，页码: {}，每页大小: {}", indexName, page, size);

            // 调用SimpleEsWriter的分页查询方法
            SearchResponse response = esWriter.searchWithPagination(indexName, query, page, size);

            // 从响应中提取文档列表
            List<Map<String, Object>> documents = esWriter.getDocumentsFromResponse(response);

            log.info("分页查询完成，返回文档数量: {}", documents.size());
            return documents;

        } catch (Exception e) {
            log.error("分页查询失败，索引: {}, 页码: {}", indexName, page, e);
            throw new RuntimeException("分页查询失败", e);
        }
    }

    /**
     * 不带查询条件的分页查询
     *
     * @param indexName 索引名称
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 查询结果文档列表
     */
    public List<Map<String, Object>> searchAllWithPagination(String indexName, int page, int size) {
        try {
            log.info("开始查询所有文档，索引: {}，页码: {}，每页大小: {}", indexName, page, size);

            // 调用SimpleEsWriter的查询所有方法
            SearchResponse response = esWriter.searchAllWithPagination(indexName, page, size);

            // 从响应中提取文档列表
            List<Map<String, Object>> documents = esWriter.getDocumentsFromResponse(response);

            log.info("查询所有文档完成，返回文档数量: {}", documents.size());
            return documents;

        } catch (Exception e) {
            log.error("查询所有文档失败，索引: {}, 页码: {}", indexName, page, e);
            throw new RuntimeException("查询所有文档失败", e);
        }
    }

    /**
     * 获取指定索引的总文档数
     *
     * @param indexName 索引名称
     * @param query 查询条件，可以为null表示查询所有
     * @return 总文档数
     */
    public long getTotalCount(String indexName, QueryBuilder query) {
        try {
            SearchResponse response = esWriter.searchWithPagination(indexName, query, 1, 1);
            return response.getHits().getTotalHits().value;
        } catch (Exception e) {
            log.error("获取总文档数失败，索引: {}", indexName, e);
            throw new RuntimeException("获取总文档数失败", e);
        }
    }

    /**
     * 获取所有索引的详细信息（包含统计信息）
     * 返回：Map<索引名, 索引详情>
     */
    public Map<String, Map<String, Object>> getAllIndicesWithDetails() throws IOException {
        return esWriter.getAllIndicesWithDetails();
    }

    public List<String> getAllIndices() throws IOException {
        return esWriter.getAllIndices();
    }
}
