package com.lz.sample.es.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz.sample.es.SimpleEsWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch 索引管理工具类
 * 用于处理 .deprecation 索引和 Kibana 索引相关操作
 *
 * @author Administrator
 */
@Slf4j
@Component
public class EsIndexManagementUtil {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行 Elasticsearch 原始请求
     *
     * @param method HTTP 方法 (GET, POST, PUT, DELETE)
     * @param endpoint 请求端点
     * @param requestBody 请求体 (JSON格式)
     * @return 响应结果
     * @throws IOException IO异常
     */
    public Map<String, Object> executeRequest(String method, String endpoint, String requestBody) throws IOException {
        Request request = new Request(method, endpoint);
        if (requestBody != null && !requestBody.trim().isEmpty()) {
            request.setJsonEntity(requestBody);
        }


        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        log.info("响应结果: {}", response.toString());
        HttpEntity entity = response.getEntity();
        String s = objectMapper.writeValueAsString(entity);
        log.info("响应结果: {}", s);
        return objectMapper.readValue(entity.getContent(), Map.class);
    }

    /**
     * 获取 .deprecation 索引中的文档
     *
     * @param size 返回文档数量，默认为10
     * @return 查询结果
     * @throws IOException IO异常
     */
    public Map<String, Object> getDeprecationLogs(int size) throws IOException {
        String endpoint = "/.deprecation*/_search";
        String requestBody = String.format(
            "{\n" +
            "  \"size\": %d,\n" +
            "  \"sort\": [\n" +
            "    {\n" +
            "      \"@timestamp\": {\n" +
            "        \"order\": \"desc\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}", size
        );

        return executeRequest("GET", endpoint, requestBody);
    }

    /**
     * 删除 .deprecation 索引
     *
     * @return 操作结果
     * @throws IOException IO异常
     */
    public boolean deleteDeprecationIndices() throws IOException {
        // 首先获取所有 .deprecation 索引
        String getIndicesEndpoint = "/_cat/indices/.deprecation*?format=json";
        Response response = restHighLevelClient.getLowLevelClient().performRequest(
            new Request("GET", getIndicesEndpoint)
        );

        JsonNode indices = objectMapper.readTree(response.getEntity().getContent());
        if (indices.size() == 0) {
            log.info("没有找到 .deprecation 相关索引");
            return true;
        }

        // 构建索引名称列表
        StringBuilder indexNames = new StringBuilder();
        for (JsonNode index : indices) {
            if (indexNames.length() > 0) {
                indexNames.append(",");
            }
            indexNames.append(index.get("index").asText());
        }

        // 删除索引
        DeleteIndexRequest request = new DeleteIndexRequest(indexNames.toString());
        AcknowledgedResponse deleteResponse = restHighLevelClient.indices().delete(request, null);

        return deleteResponse.isAcknowledged();
    }

    /**
     * 获取 Kibana 相关索引信息
     *
     * @return Kibana 索引列表
     * @throws IOException IO异常
     */
    public Map<String, Object> getKibanaIndices() throws IOException {
        String endpoint = "/_cat/indices/.kibana*?format=json&v";
        return executeRequest("GET", endpoint, null);
    }

    /**
     * 获取 Kibana 索引中的配置信息
     *
     * @return 配置信息
     * @throws IOException IO异常
     */
    public Map<String, Object> getKibanaConfig() throws IOException {
        String endpoint = "/.kibana/_search";
        String requestBody = "{\n" +
            "  \"query\": {\n" +
            "    \"term\": {\n" +
            "      \"type\": {\n" +
            "        \"value\": \"config\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"sort\": [\n" +
            "    {\n" +
            "      \"config.buildNum\": {\n" +
            "        \"order\": \"desc\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"size\": 1\n" +
            "}";

        return executeRequest("GET", endpoint, requestBody);
    }

    /**
     * 根据 JSON 配置执行批量操作
     *
     * @param operations 批量操作配置
     * @return 操作结果
     * @throws IOException IO异常
     */
    public List<Map<String, Object>> executeBatchOperations(List<Map<String, Object>> operations) throws IOException {
        if (operations == null || operations.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map<String, Object> operation : operations) {
            String method = (String) operation.get("method");
            String endpoint = (String) operation.get("endpoint");
            Object requestBody = operation.get("body");

            String jsonBody = null;
            if (requestBody != null) {
                if (requestBody instanceof String) {
                    jsonBody = (String) requestBody;
                } else {
                    jsonBody = objectMapper.writeValueAsString(requestBody);
                }
            }

            try {
                Map<String, Object> result = executeRequest(method, endpoint, jsonBody);
                log.info("批量操作成功: {} {} - 结果: {}", method, endpoint, result);
            } catch (IOException e) {
                log.error("批量操作失败: {} {}", method, endpoint, e);
                throw e;
            }
        }

        return operations.stream()
            .map(op -> {
                Map<String, Object> result = new HashMap<>();
                result.put("method", op.get("method"));
                result.put("endpoint", op.get("endpoint"));
                result.put("status", "completed");
                return result;
            }).collect(Collectors.toList());
    }

    /**
     * 检查特定索引是否存在
     *
     * @param indexName 索引名称
     * @return 是否存在
     * @throws IOException IO异常
     */
    public boolean indexExists(String indexName) throws IOException {
        String endpoint = "/_cat/indices/" + indexName + "?format=json";
        try {
            Response response = restHighLevelClient.getLowLevelClient().performRequest(
                new Request("GET", endpoint)
            );
            JsonNode indices = objectMapper.readTree(response.getEntity().getContent());
            return indices.size() > 0;
        } catch (Exception e) {
            log.warn("检查索引存在性时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 重新打开索引
     *
     * @param indexName 索引名称
     * @return 操作结果
     * @throws IOException IO异常
     */
    public boolean openIndex(String indexName) throws IOException {
        if (!indexExists(indexName)) {
            log.warn("索引 {} 不存在", indexName);
            return false;
        }

        String endpoint = "/" + indexName + "/_open";
        Map<String, Object> result = executeRequest("POST", endpoint, null);

        return result.get("acknowledged") != null && (Boolean) result.get("acknowledged");
    }

    /**
     * 关闭索引
     *
     * @param indexName 索引名称
     * @return 操作结果
     * @throws IOException IO异常
     */
    public boolean closeIndex(String indexName) throws IOException {
        if (!indexExists(indexName)) {
            log.warn("索引 {} 不存在", indexName);
            return false;
        }

        String endpoint = "/" + indexName + "/_close";
        Map<String, Object> result = executeRequest("POST", endpoint, null);

        return result.get("acknowledged") != null && (Boolean) result.get("acknowledged");
    }

    /**
     * 获取索引统计信息
     *
     * @param indexName 索引名称
     * @return 统计信息
     * @throws IOException IO异常
     */
    public Map<String, Object> getIndexStats(String indexName) throws IOException {
        String endpoint = "/" + indexName + "/_stats";
        return executeRequest("GET", endpoint, null);
    }

    /**
     * 清除索引缓存
     *
     * @param indexName 索引名称
     * @return 操作结果
     * @throws IOException IO异常
     */
    public boolean clearIndexCache(String indexName) throws IOException {
        String endpoint = "/" + indexName + "/_cache/clear";
        Map<String, Object> result = executeRequest("POST", endpoint, null);

        return result.get("acknowledged") != null && (Boolean) result.get("acknowledged");
    }
}
