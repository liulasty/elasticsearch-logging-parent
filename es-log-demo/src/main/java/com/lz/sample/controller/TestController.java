package com.lz.sample.controller;

import com.lz.sample.entry.LogEntry;
import com.lz.sample.service.LogStorageService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 日志管理控制器
 * 提供日志记录、查询、分页等功能的REST接口
 *
 * @author Administrator
 */
@Slf4j
@RestController
public class TestController {

    private final LogStorageService logStorageService;

    public TestController(LogStorageService logStorageService) {
        this.logStorageService = logStorageService;
    }

    /**
     * 添加日志到队列
     *
     * @param level   日志级别
     * @param message 日志消息内容
     * @return 操作结果信息
     */
    @PostMapping("/log")
    public String addLog(@RequestParam String level, @RequestParam String message) {
        LogEntry logEntry = new LogEntry(level, message);
        logStorageService.addLogToQueue(logEntry);
        return "Log added to queue";
    }

    /**
     * 添加空消息日志到队列
     * 测试方法，添加一个消息为空的日志条目
     *
     * @return 操作结果信息
     */
    @PostMapping("/logNull")
    public String addLog() {
        String level = "test";
        String message = "";
        LogEntry logEntry = new LogEntry(level, message);
        logStorageService.addLogToQueue(logEntry);
        return "Log added to queue";
    }

    /**
     * 分页查询ES中的文档
     *
     * @param indexName 索引名称
     * @param query     查询条件（可选）
     * @param page      页码，默认为1
     * @param size      每页大小，默认为10
     * @return 查询结果列表
     */
    @PostMapping("/search/pagination")
    public ResponseEntity<List<Map<String, Object>>> searchWithPagination(
            @RequestParam String indexName,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            if (!isValidPagination(page, size)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            QueryBuilder queryBuilder = buildQuery(query);
            List<Map<String, Object>> results = logStorageService.searchWithPagination(indexName, queryBuilder, page, size);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("分页查询失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 查询所有文档（带分页）
     * 查询指定索引中的所有文档，并支持分页功能
     *
     * @param indexName 索引名称
     * @param page      页码，默认为1
     * @param size      每页大小，默认为10
     * @return 查询结果列表
     */
    @PostMapping("/search/all/pagination")
    public ResponseEntity<List<Map<String, Object>>> searchAllWithPagination(
            @RequestParam String indexName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            if (!isValidPagination(page, size)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            List<Map<String, Object>> results = logStorageService.searchAllWithPagination(indexName, page, size);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("查询所有文档失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取指定索引的总文档数
     *
     * @param indexName 索引名称
     * @param query     查询条件（可选）
     * @return 总文档数量
     */
    @GetMapping("/search/total-count")
    public ResponseEntity<Long> getTotalCount(
            @RequestParam String indexName,
            @RequestParam(required = false) String query) {

        try {
            QueryBuilder queryBuilder = buildQuery(query);
            long totalCount = logStorageService.getTotalCount(indexName, queryBuilder);
            return ResponseEntity.ok(totalCount);
        } catch (Exception e) {
            log.error("获取总文档数失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 查询指定索引中的所有文档
     * 默认查询第一页，每页10条记录
     *
     * @param indexName 索引名称
     * @return 查询结果列表
     */
    @GetMapping("/search/all")
    public ResponseEntity<List<Map<String, Object>>> searchAll(
            @RequestParam String indexName) {

        try {
            List<Map<String, Object>> results = logStorageService.searchAllWithPagination(indexName, 1, 10);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("查询所有文档失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取所有索引及其详细信息
     *
     * @return 包含所有索引详细信息的响应
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Map<String, Object>>> getAll() {

        try {
            Map<String, Map<String, Object>> results = logStorageService.getAllIndicesWithDetails();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("查询所有文档失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取所有索引名称列表
     *
     * @return 索引名称列表
     */
    @GetMapping("/allIndex")
    public ResponseEntity<List<String>> getAllIndex() {

        try {
            List<String> results = logStorageService.getAllIndices();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("查询所有文档失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 验证分页参数是否有效
     * 检查页码和每页大小是否符合要求：页码>=1，每页大小在1-1000之间
     *
     * @param page 当前页码，必须大于等于1
     * @param size 每页大小，必须大于等于1且小于等于1000
     * @return 参数有效返回true，否则返回false
     */
    private boolean isValidPagination(int page, int size) {
        return page >= 1 && size >= 1 && size <= 1000;
    }

    /**
     * 构建Elasticsearch查询对象
     * 根据输入的查询字符串创建QueryBuilder，如果查询字符串为空则返回null
     *
     * @param query 查询字符串，用于构建Elasticsearch查询条件
     * @return QueryBuilder对象，如果查询字符串为空则返回null
     */
    private QueryBuilder buildQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        return QueryBuilders.queryStringQuery(query).defaultField("message");
    }
}
