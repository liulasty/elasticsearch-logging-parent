package com.lz.sample.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * ES 基础命令执行服务
 * 用于演示和执行基础的 ES 增删改查命令
 */
@Service
@Slf4j
public class EsBasicCommandService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 通用执行方法
     */
    public String executeRequest(String method, String endpoint, String jsonBody) {
        try {
            Request request = new Request(method, endpoint);
            if (jsonBody != null && !jsonBody.isEmpty()) {
                request.setJsonEntity(jsonBody);
            }
            Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            log.info("Execute {} {}: Status={}, Response={}", method, endpoint, response.getStatusLine().getStatusCode(), responseBody);
            return responseBody;
        } catch (IOException e) {
            log.error("Failed to execute {} {}", method, endpoint, e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 2. 查看有哪些索引
     * GET /_cat/indices?v
     */
    public String listIndices() {
        return executeRequest("GET", "/_cat/indices?v", null);
    }

    /**
     * 3. 创建一个测试索引
     * PUT /test_practice_001
     */
    public String createTestIndex() {
        return executeRequest("PUT", "/test_practice_001", null);
    }

    /**
     * 4. 向索引写点数据
     * POST /test_practice_001/_doc
     * { "user": "小白", "message": "我正在学习ES命令", "timestamp": "2024-01-10" }
     */
    public String writeTestData() {
        String jsonBody = "{\n" +
                "  \"user\": \"小白\",\n" +
                "  \"message\": \"我正在学习ES命令\",\n" +
                "  \"timestamp\": \"2024-01-10\"\n" +
                "}";
        return executeRequest("POST", "/test_practice_001/_doc", jsonBody);
    }

    /**
     * 5. 搜索一下刚才写的数据
     * GET /test_practice_001/_search
     * { "query": { "match_all": {} } }
     */
    public String searchTestData() {
        String jsonBody = "{\n" +
                "  \"query\": {\n" +
                "    \"match_all\": {}\n" +
                "  }\n" +
                "}";
        return executeRequest("GET", "/test_practice_001/_search", jsonBody);
    }

    /**
     * 6. 查看这个索引的详细信息
     * GET /test_practice_001
     */
    public String getIndexDetails() {
        return executeRequest("GET", "/test_practice_001", null);
    }

    /**
     * 7. 最后，清理掉测试索引
     * DELETE /test_practice_001
     */
    public String deleteTestIndex() {
        return executeRequest("DELETE", "/test_practice_001", null);
    }
}
