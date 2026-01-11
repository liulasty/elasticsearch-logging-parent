package com.lz.sample.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz.sample.es.SimpleEsWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * ES 自定义命令执行服务
 * 支持传入自定义参数执行基础 ES 命令
 */
@Service
@Slf4j
public class EsCustomCommandService {

    @Autowired
    private SimpleEsWriter simpleEsWriter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通用执行方法
     */
    public String executeRequest(String method, String endpoint, String jsonBody) {
        return simpleEsWriter.executeRequest(method, endpoint, jsonBody);
    }

    /**
     * 2. 查看索引
     * @param options 可选参数，例如 "v" 显示详细信息，或者 specific index pattern
     */
    public String listIndices(String options) {
        String endpoint = "/_cat/indices";
        if (options != null && !options.isEmpty()) {
            endpoint += "?" + options;
        } else {
            endpoint += "?v"; // 默认带v
        }
        return executeRequest("GET", endpoint, null);
    }

    /**
     * 3. 创建索引
     * @param indexName 索引名称
     */
    public String createIndex(String indexName) {
        if (indexName == null || indexName.trim().isEmpty()) {
            return "Error: Index name cannot be empty";
        }
        return executeRequest("PUT", "/" + indexName, null);
    }

    /**
     * 4. 向索引写数据
     * @param indexName 索引名称
     * @param data 数据对象 (将被转换为JSON)
     */
    public String writeData(String indexName, Object data) {
        if (indexName == null || indexName.trim().isEmpty()) {
            return "Error: Index name cannot be empty";
        }
        try {
            String jsonBody = objectMapper.writeValueAsString(data);
            return executeRequest("POST", "/" + indexName + "/_doc", jsonBody);
        } catch (IOException e) {
            log.error("Failed to serialize data", e);
            return "Error: Failed to serialize data - " + e.getMessage();
        }
    }

    /**
     * 5. 搜索数据
     * @param indexName 索引名称
     * @param queryJson 查询JSON字符串，如果为null则查询所有
     */
    public String searchData(String indexName, String queryJson) {
        if (indexName == null || indexName.trim().isEmpty()) {
            return "Error: Index name cannot be empty";
        }
        String endpoint = "/" + indexName + "/_search";
        if (queryJson == null || queryJson.trim().isEmpty()) {
            queryJson = "{\"query\": {\"match_all\": {}}}";
        }
        return executeRequest("GET", endpoint, queryJson);
    }

    /**
     * 6. 查看索引详细信息
     * @param indexName 索引名称
     */
    public String getIndexDetails(String indexName) {
        if (indexName == null || indexName.trim().isEmpty()) {
            return "Error: Index name cannot be empty";
        }
        return executeRequest("GET", "/" + indexName, null);
    }

    /**
     * 7. 删除索引
     * @param indexName 索引名称
     */
    public String deleteIndex(String indexName) {
        if (indexName == null || indexName.trim().isEmpty()) {
            return "Error: Index name cannot be empty";
        }
        return executeRequest("DELETE", "/" + indexName, null);
    }
}
