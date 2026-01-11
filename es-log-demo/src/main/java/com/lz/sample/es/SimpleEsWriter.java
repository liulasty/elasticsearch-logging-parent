package com.lz.sample.es;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

import org.apache.http.util.EntityUtils;

/**
 * Elasticsearch 简单写入工具
 * 目标：验证Java到ES的连通性
 */
@Slf4j
@Component
public class SimpleEsWriter implements Closeable {

    private RestHighLevelClient client;
    
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Value("${es.host:localhost}")
    private String host;

    @Value("${es.port:9200}")
    private int port;

    @Value("${es.username:}")
    private String username;

    @Value("${es.password:}")
    private String password;

    /**
     * 初始化ES客户端
     * 该方法作为一个Bean工厂方法，将RestHighLevelClient注册到Spring容器中
     */
    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient() {
        log.info("正在初始化ES客户端，连接 {}:{}", host, port);

        // 构建RestClientBuilder
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, "http"));

        // 配置连接池参数
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            // 设置连接超时时间（单位：毫秒）
            requestConfigBuilder.setConnectTimeout(5000);
            // 设置Socket超时时间
            requestConfigBuilder.setSocketTimeout(60000);
            // 设置请求超时时间
            requestConfigBuilder.setConnectionRequestTimeout(10000);
            return requestConfigBuilder;
        });

        // 配置HTTP客户端参数
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            // 设置最大连接数
            httpClientBuilder.setMaxConnTotal(30);
            // 设置每个路由的最大连接数
            httpClientBuilder.setMaxConnPerRoute(10);

            // 如果有认证信息，设置认证
            if (username != null && !username.trim().isEmpty() && password != null && !password.trim().isEmpty()) {
                log.info("启用ES基础认证");
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password)
                );
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            return httpClientBuilder;
        });

        // 创建高级客户端
        this.client = new RestHighLevelClient(builder);
        log.info("ES客户端初始化完成");
        return this.client;
    }

    /**
     * 执行通用低级请求
     * 
     * @param method HTTP方法 (GET, POST, PUT, DELETE 等)
     * @param endpoint 请求端点 (例如 "/_cat/indices")
     * @param jsonBody 请求体JSON字符串，如果没有则传null
     * @return 响应体字符串
     */
    public String executeRequest(String method, String endpoint, String jsonBody) {
        try {
            Request request = new Request(method, endpoint);
            if (jsonBody != null && !jsonBody.isEmpty()) {
                request.setJsonEntity(jsonBody);
            }
            Response response = client.getLowLevelClient().performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            log.info("Execute {} {}: Status={}, Response={}", method, endpoint, response.getStatusLine().getStatusCode(), responseBody);
            return responseBody;
        } catch (IOException e) {
            log.error("Failed to execute {} {}", method, endpoint, e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 检查ES连接状态
     */
    public boolean checkConnection() {
        try {
            boolean isConnected = client.ping(RequestOptions.DEFAULT);
            log.info("ES连接检查: {}", isConnected ? "成功" : "失败");
            return isConnected;
        } catch (IOException e) {
            log.error("ES连接检查失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取集群健康状态
     */
    public ClusterHealthResponse getClusterHealth() throws IOException {
        ClusterHealthRequest request = new ClusterHealthRequest();
        return client.cluster().health(request, RequestOptions.DEFAULT);
    }

    /**
     * 获取所有索引列表（简单信息）
     * 返回：索引名称列表
     */
    public List<String> getAllIndices() throws IOException {
        GetIndexRequest request = new GetIndexRequest();
        request.indices("*");  // 通配符获取所有索引

        GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);
        String[] indices = response.getIndices();

        log.info("获取到 {} 个索引", indices.length);
        return Arrays.asList(indices);
    }

    /**
     * 获取索引详细信息
     * 包括：设置、映射、别名等
     */
    public Map<String, Object> getIndexDetail(String indexName) throws IOException {
        Map<String, Object> result = new HashMap<>();

        GetIndexRequest request = new GetIndexRequest();
        request.indices(indexName);

        // 指定需要获取的信息
        request.includeDefaults(false); // 不包含默认设置

        GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);

        // 1. 获取设置
        Settings settings = response.getSettings().get(indexName);
        if (settings != null) {
            Map<String, Object> settingsMap = new HashMap<>();
            settings.keySet().forEach(key -> {
                settingsMap.put(key, settings.get(key));
            });
            result.put("settings", settingsMap);
        }

        // 2. 获取映射
        ImmutableOpenMap<String, MappingMetadata> mappings = response.getMappings().get(indexName);
        if (mappings != null && !mappings.isEmpty()) {
            Map<String, Object> mappingInfo = new HashMap<>();
            // Elasticsearch 7.x+ 通常只有一个映射类型 "_doc"，遍历所有映射
            for (ObjectObjectCursor<String, MappingMetadata> cursor : mappings) {
                mappingInfo.put(cursor.key, cursor.value.getSourceAsMap());
            }
            result.put("mappings", mappingInfo);
        }

        // 3. 获取别名
        List<AliasMetadata> aliases = response.getAliases().get(indexName);
        if (aliases != null) {
            List<String> aliasList = new ArrayList<>();
            for (AliasMetadata cursor : aliases) {
                String alias = cursor.alias();
                aliasList.add(alias);
            }
            result.put("aliases", aliasList);
        }

        // 4. 获取索引统计信息
        try {
            Map<String, Object> stats = getIndexStats(indexName);
            result.put("stats", stats);
        } catch (Exception e) {
            log.warn("获取索引统计信息失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 获取索引统计信息（替代已废弃的IndicesStatsRequest方法）
     */
    public Map<String, Object> getIndexStats(String indexName) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 通过低级客户端发送HTTP请求获取统计信息
            Request request = new Request("GET", "/" + indexName + "/_stats");
            Response response = client.getLowLevelClient().performRequest(request);

            JsonNode responseJson = OBJECT_MAPPER.readTree(response.getEntity().getContent());

            // 获取索引的统计信息
            JsonNode indexNode = responseJson.path("indices").path(indexName);
            if (indexNode != null) {
                JsonNode primaries = indexNode.get("primaries");
                JsonNode total = indexNode.get("total");

                if (primaries != null) {
                    // docsCount: 文档总数
                    stats.put("docsCount", primaries.get("docs").get("count").asLong());

                    // docsDeleted: 已删除文档数
                    stats.put("docsDeleted", primaries.get("docs").get("deleted").asLong());

                    // storeSizeInBytes: 存储大小（字节）
                    stats.put("storeSizeInBytes", primaries.get("store").get("size_in_bytes").asLong());

                    // storeSizeHuman: 人类可读的存储大小
                    long sizeInBytes = primaries.get("store").get("size_in_bytes").asLong();
                    stats.put("storeSizeHuman", bytesToHumanReadable(sizeInBytes));
                }

                if (total != null) {
                    // 获取分片统计信息
                    stats.put("totalShards", getShardCount(indexName));
                    stats.put("successfulShards", getSuccessfulShards(indexName));
                    stats.put("failedShards", getFailedShards(indexName));
                }
            }

        } catch (Exception e) {
            log.warn("获取索引统计信息失败: {}", e.getMessage());
        }

        return stats;
    }

    public boolean bulkWrite(String indexName, List<Map<String, Object>> logDataList) {
        if (logDataList == null || logDataList.isEmpty()) {
            return true;
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (Map<String, Object> doc : logDataList) {
            if (doc == null || doc.isEmpty()) {
                continue;
            }
            IndexRequest request = new IndexRequest(indexName).source(doc);
            bulkRequest.add(request);
        }

        if (bulkRequest.numberOfActions() == 0) {
            return true;
        }

        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (response.hasFailures()) {
                log.warn("批量写入存在失败: {}", response.buildFailureMessage());
            }
            return !response.hasFailures();
        } catch (Exception e) {
            log.error("批量写入失败", e);
            return false;
        }
    }

    /**
     * 获取分片总数
     */
    private int getShardCount(String indexName) {
        try {
            JsonNode shardsNode = getShardsNode(indexName);
            if (shardsNode == null || shardsNode.isMissingNode() || !shardsNode.isObject()) {
                return 0;
            }

            int total = 0;
            Iterator<String> shardIds = shardsNode.fieldNames();
            while (shardIds.hasNext()) {
                String shardId = shardIds.next();
                JsonNode copies = shardsNode.get(shardId);
                if (copies != null && copies.isArray()) {
                    total += copies.size();
                }
            }
            return total;
        } catch (Exception e) {
            log.warn("获取分片信息失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 获取成功分片数
     */
    private int getSuccessfulShards(String indexName) {
        try {
            int successful = 0;
            JsonNode shardsNode = getShardsNode(indexName);
            if (shardsNode == null || shardsNode.isMissingNode() || !shardsNode.isObject()) {
                return 0;
            }

            Iterator<String> shardIds = shardsNode.fieldNames();
            while (shardIds.hasNext()) {
                String shardId = shardIds.next();
                JsonNode copies = shardsNode.get(shardId);
                if (copies == null || !copies.isArray()) {
                    continue;
                }

                for (JsonNode copy : copies) {
                    if ("STARTED".equals(copy.path("state").asText())) {
                        successful++;
                    }
                }
            }
            return successful;
        } catch (Exception e) {
            log.warn("获取成功分片数失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 获取失败分片数
     */
    private int getFailedShards(String indexName) {
        try {
            int failed = 0;
            JsonNode shardsNode = getShardsNode(indexName);
            if (shardsNode == null || shardsNode.isMissingNode() || !shardsNode.isObject()) {
                return 0;
            }

            Iterator<String> shardIds = shardsNode.fieldNames();
            while (shardIds.hasNext()) {
                String shardId = shardIds.next();
                JsonNode copies = shardsNode.get(shardId);
                if (copies == null || !copies.isArray()) {
                    continue;
                }

                for (JsonNode copy : copies) {
                    if (!"STARTED".equals(copy.path("state").asText())) {
                        failed++;
                    }
                }
            }
            return failed;
        } catch (Exception e) {
            log.warn("获取失败分片数失败: {}", e.getMessage());
            return 0;
        }
    }

    private JsonNode getShardsNode(String indexName) throws IOException {
        Request request = new Request("GET", "/" + indexName + "/_shards");
        Response response = client.getLowLevelClient().performRequest(request);
        JsonNode responseJson = OBJECT_MAPPER.readTree(response.getEntity().getContent());
        return responseJson.path("indices").path(indexName).path("shards");
    }

    /**
     * 将字节数转换为人类可读的格式
     */
    private String bytesToHumanReadable(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = (exp < 6) ? "KMGTPE".charAt(exp - 1) + "i" : "Xi";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 获取所有索引的详细信息（包含统计信息）
     * 返回：Map<索引名, 索引详情>
     */
    public Map<String, Map<String, Object>> getAllIndicesWithDetails() throws IOException {
        Map<String, Map<String, Object>> allIndicesDetails = new HashMap<>();

        // 1. 获取所有索引名称
        List<String> allIndices = getAllIndices();

        // 2. 为每个索引获取详细信息
        for (String indexName : allIndices) {
            try {
                Map<String, Object> indexDetails = getIndexDetail(indexName);
                allIndicesDetails.put(indexName, indexDetails);
            } catch (Exception e) {
                log.error("获取索引 {} 详情失败: {}", indexName, e.getMessage());
                allIndicesDetails.put(indexName, Collections.singletonMap("error", e.getMessage()));
            }
        }

        return allIndicesDetails;
    }

    /**
     * 获取索引统计信息（更详细）
     */
    public Map<String, Object> getIndexStatistics(String indexName) throws IOException {

        // 创建获取索引请求
        GetIndexRequest request = new GetIndexRequest();
        request.indices(indexName);

        // 执行请求获取索引详细信息
        GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);

        // 获取索引的相关信息
        String[] indexNames = response.getIndices();
        ImmutableOpenMap<String, List<AliasMetadata>> aliases = response.getAliases();
        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetadata>> mappings = response.getMappings();
        ImmutableOpenMap<String, Settings> settings = response.getSettings();

        Map<String, Object> stats = new HashMap<>();
        stats.put("indexNames", indexNames);
        stats.put("aliases", aliases);
        stats.put("mappings", mappings);
        stats.put("settings", settings);

        return stats;
    }

    /**
     * 检查索引是否存在
     */
    public boolean indexExists(String indexName) throws IOException {
        GetIndexRequest request = new GetIndexRequest();
        request.indices(indexName);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    /**
     * 删除索引
     */
    public boolean deleteIndex(String indexName) throws IOException {
        if (!indexExists(indexName)) {
            log.warn("索引 {} 不存在，无法删除", indexName);
            return false;
        }

        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        try {
            client.indices().delete(request, RequestOptions.DEFAULT);
            log.info("索引 {} 删除成功", indexName);
            return true;
        } catch (Exception e) {
            log.error("删除索引 {} 失败: {}", indexName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 写入单条日志文档
     */
    public boolean writeLog(String indexName, Map<String, Object> logData) {
        try {
            // 1. 构建索引请求
            IndexRequest request = new IndexRequest(indexName);

            // 2. 设置文档ID（如果不设置，ES会自动生成）
            // request.id("custom-id");

            request.source(logData);

            // 4. 执行写入
            log.debug("正在写入文档到索引: {}", indexName);
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);

            // 5. 处理响应
            String id = response.getId();
            log.info("文档写入成功！索引: {}, 文档ID: {}, 版本: {}",
                    indexName, id, response.getVersion());

            return true;
        } catch (Exception e) {
            log.error("文档写入失败", e);
            return false;
        }
    }

    /**
     * 使用JSON字符串写入日志
     */
    public boolean writeLogJson(String indexName, String jsonLog) {
        try {
            IndexRequest request = new IndexRequest(indexName);
            request.source(jsonLog, XContentType.JSON);

            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            log.info("JSON文档写入成功！索引: {}, 文档ID: {}",
                    indexName, response.getId());
            return true;
        } catch (Exception e) {
            log.error("JSON文档写入失败", e);
            return false;
        }
    }

    /**
     * 分页查询ES中的文档
     *
     * @param indexName 索引名称
     * @param query 查询条件，可以为null表示查询所有
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 查询结果
     */
    public SearchResponse searchWithPagination(String indexName, QueryBuilder query, int page, int size) {
        try {
            // 计算from值
            int from = (page - 1) * size;

            // 构建搜索请求
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            // 设置查询条件，如果query为null则使用MatchAllQueryBuilder查询所有文档
            if (query != null) {
                searchSourceBuilder.query(query);
            } else {
                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            }

            // 设置分页参数
            searchSourceBuilder.from(from).size(size);

            // 设置排序，按时间戳降序排列（如果有timestamp字段）
            searchSourceBuilder.sort("@timestamp", SortOrder.DESC);

            searchRequest.source(searchSourceBuilder);

            log.debug("执行分页查询，索引: {}，页码: {}，每页大小: {}", indexName, page, size);

            // 执行查询
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            log.info("分页查询成功！索引: {}，命中总数: {}，返回文档数: {}",
                    indexName, response.getHits().getTotalHits().value, response.getHits().getHits().length);

            return response;
        } catch (Exception e) {
            log.error("分页查询失败", e);
            throw new RuntimeException("分页查询失败", e);
        }
    }

    /**
     * 获取查询结果中的文档列表
     *
     * @param searchResponse 搜索响应结果
     * @return 文档列表
     */
    public List<Map<String, Object>> getDocumentsFromResponse(SearchResponse searchResponse) {
        List<Map<String, Object>> documents = new ArrayList<>();

        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            // 添加文档ID和版本信息
            sourceMap.put("_id", hit.getId());
            sourceMap.put("_version", hit.getVersion());
            documents.add(sourceMap);
        }

        return documents;
    }

    /**
     * 简单查询方法，不带查询条件的分页查询
     */
    public SearchResponse searchAllWithPagination(String indexName, int page, int size) {
        return searchWithPagination(indexName, null, page, size);
    }

    /**
     * 关闭客户端连接
     */
    @Override
    public void close() {
        // Spring容器管理Bean的生命周期，通常不需要手动调用close，
        // 但如果实现了Closeable接口，Spring销毁Bean时会调用此方法。
        if (client != null) {
            try {
                // 避免重复关闭（RestHighLevelClient.close() 是幂等的，但加个日志更好）
                client.close();
                log.info("ES客户端已关闭");
            } catch (IOException e) {
                log.error("关闭ES客户端时出错", e);
            }
        }
    }

    /**
     * 生成测试日志数据
     */
    public static Map<String, Object> createTestLog() {
        Map<String, Object> log = new HashMap<>();

        // 基本日志字段
        log.put("@timestamp", Instant.now().toString());
        log.put("level", "INFO");
        log.put("logger", "com.xxx.es.client.SimpleEsWriter");
        log.put("message", "这是一条测试日志，用于验证ES连通性");

        // 应用信息
        log.put("app_name", "es-log-demo");
        log.put("app_version", "1.0.0");

        // 服务器信息
        log.put("host", "localhost");
        log.put("port", 8080);

        // 线程信息
        log.put("thread_name", "main");

        // 自定义业务字段
        log.put("user_id", 1001);
        log.put("action", "login");
        log.put("duration_ms", 125);
        log.put("success", true);

        return log;
    }
}
