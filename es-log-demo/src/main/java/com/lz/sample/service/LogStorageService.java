package com.lz.sample.service;

import com.lz.sample.entry.LogEntry;
import com.lz.sample.es.SimpleEsWriter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@Slf4j
public class LogStorageService {
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    private final String logFilePath = "application_logs.txt";

    public void addLogToQueue(LogEntry logEntry) {
        SimpleEsWriter simpleEsWriter = new SimpleEsWriter();
        simpleEsWriter.writeLog("log", SimpleEsWriter.createTestLog());
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
            SimpleEsWriter simpleEsWriter = new SimpleEsWriter();

            // 调用SimpleEsWriter的分页查询方法
            SearchResponse response = simpleEsWriter.searchWithPagination(indexName, query, page, size);

            // 从响应中提取文档列表
            List<Map<String, Object>> documents = simpleEsWriter.getDocumentsFromResponse(response);

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
            SimpleEsWriter simpleEsWriter = new SimpleEsWriter();

            // 调用SimpleEsWriter的查询所有方法
            SearchResponse response = simpleEsWriter.searchAllWithPagination(indexName, page, size);

            // 从响应中提取文档列表
            List<Map<String, Object>> documents = simpleEsWriter.getDocumentsFromResponse(response);

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
            SimpleEsWriter simpleEsWriter = new SimpleEsWriter();
            SearchResponse response = simpleEsWriter.searchWithPagination(indexName, query, 1, 1);
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
        SimpleEsWriter simpleEsWriter = new SimpleEsWriter();
        return simpleEsWriter.getAllIndicesWithDetails();
    }

    public List<String> getAllIndices() throws IOException {
        SimpleEsWriter simpleEsWriter = new SimpleEsWriter();
        return simpleEsWriter.getAllIndices();
    }
}
