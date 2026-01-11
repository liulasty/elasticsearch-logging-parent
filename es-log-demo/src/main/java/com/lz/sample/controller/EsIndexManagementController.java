package com.lz.sample.controller;

import com.lz.sample.es.util.EsIndexManagementUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * Elasticsearch 索引管理控制器
 * 提供 .deprecation 索引和 Kibana 索引管理功能
 *
 * @author Administrator
 */
@Slf4j
@RestController
@RequestMapping("/es")
public class EsIndexManagementController {

    @Autowired
    private EsIndexManagementUtil esIndexManagementUtil;

    /**
     * 获取 .deprecation 索引中的文档
     *
     * @param size 返回文档数量
     * @return 查询结果
     */
    @GetMapping("/deprecation/logs")
    public ResponseEntity<Map<String, Object>> getDeprecationLogs(
            @RequestParam(defaultValue = "10") int size) {
        try {
            Map<String, Object> result = esIndexManagementUtil.getDeprecationLogs(size);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("获取 .deprecation 日志失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除 .deprecation 索引
     *
     * @return 操作结果
     */
    @DeleteMapping("/deprecation/delete")
    public ResponseEntity<Map<String, Object>> deleteDeprecationIndices() {
        try {
            boolean success = esIndexManagementUtil.deleteDeprecationIndices();
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "成功删除 .deprecation 索引" : "删除 .deprecation 索引失败");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("删除 .deprecation 索引失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取 Kibana 相关索引信息
     *
     * @return Kibana 索引列表
     */
    @GetMapping("/kibana/indices")
    public ResponseEntity<Map<String, Object>> getKibanaIndices() {
        try {
            Map<String, Object> result = esIndexManagementUtil.getKibanaIndices();
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("获取 Kibana 索引信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取 Kibana 配置信息
     *
     * @return 配置信息
     */
    @GetMapping("/kibana/config")
    public ResponseEntity<Map<String, Object>> getKibanaConfig() {
        try {
            Map<String, Object> result = esIndexManagementUtil.getKibanaConfig();
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("获取 Kibana 配置信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 执行批量操作
     *
     * @param operations 批量操作配置
     * @return 操作结果
     */
    @PostMapping("/batch")
    public ResponseEntity<List<Map<String, Object>>> executeBatchOperations(
            @RequestBody List<Map<String, Object>> operations) {
        try {
            List<Map<String, Object>> result = esIndexManagementUtil.executeBatchOperations(operations);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("执行批量操作失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 检查索引是否存在
     *
     * @param indexName 索引名称
     * @return 存在性检查结果
     */
    @GetMapping("/index/exists")
    public ResponseEntity<Map<String, Object>> checkIndexExists(@RequestParam String indexName) {
        try {
            boolean exists = esIndexManagementUtil.indexExists(indexName);
            Map<String, Object> result = new HashMap<>();
            result.put("index", indexName);
            result.put("exists", exists);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("检查索引存在性失败: {}", indexName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 重新打开索引
     *
     * @param indexName 索引名称
     * @return 操作结果
     */
    @PostMapping("/index/open")
    public ResponseEntity<Map<String, Object>> openIndex(@RequestParam String indexName) {
        try {
            boolean success = esIndexManagementUtil.openIndex(indexName);
            Map<String, Object> result = new HashMap<>();
            result.put("index", indexName);
            result.put("success", success);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("打开索引失败: {}", indexName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 关闭索引
     *
     * @param indexName 索引名称
     * @return 操作结果
     */
    @PostMapping("/index/close")
    public ResponseEntity<Map<String, Object>> closeIndex(@RequestParam String indexName) {
        try {
            boolean success = esIndexManagementUtil.closeIndex(indexName);
            Map<String, Object> result = new HashMap<>();
            result.put("index", indexName);
            result.put("success", success);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("关闭索引失败: {}", indexName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取索引统计信息
     *
     * @param indexName 索引名称
     * @return 统计信息
     */
    @GetMapping("/index/stats")
    public ResponseEntity<Map<String, Object>> getIndexStats(@RequestParam String indexName) {
        try {
            Map<String, Object> result = esIndexManagementUtil.getIndexStats(indexName);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("获取索引统计信息失败: {}", indexName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 清除索引缓存
     *
     * @param indexName 索引名称
     * @return 操作结果
     */
    @PostMapping("/index/clear-cache")
    public ResponseEntity<Map<String, Object>> clearIndexCache(@RequestParam String indexName) {
        try {
            boolean success = esIndexManagementUtil.clearIndexCache(indexName);
            Map<String, Object> result = new HashMap<>();
            result.put("index", indexName);
            result.put("success", success);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("清除索引缓存失败: {}", indexName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}