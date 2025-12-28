package com.lz.logging.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz.logging.autoconfigure.ElasticsearchLoggingProperties;
import com.lz.logging.model.EsLogDocument;
import com.lz.logging.support.IndexPatternResolver;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Lazy;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elasticsearch 日志客户端（ES 7.17.x）
 *
 * - Java 8
 * - Spring Boot 2.7.x
 * - RestHighLevelClient
 */
@Lazy
public class ElasticsearchLogClient implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchLogClient.class);

    private final RestHighLevelClient client;
    private final ElasticsearchLoggingProperties properties;
    private final IndexPatternResolver indexPatternResolver;
    private final ObjectMapper objectMapper;

    private final ScheduledExecutorService scheduler;
    private final BlockingQueue<EsLogDocument> queue;
    private final List<EsLogDocument> batchBuffer;

    private final AtomicBoolean running = new AtomicBoolean(true);

    /* =========================
       构造函数（只做初始化）
       ========================= */
    public ElasticsearchLogClient(ElasticsearchLoggingProperties properties) {
        this.properties = properties;
        this.indexPatternResolver = new IndexPatternResolver();
        this.objectMapper = new ObjectMapper();

        this.client = RestClientFactory.createElasticsearchClient(properties);

        this.queue = new LinkedBlockingQueue<>(properties.getQueueSize());
        this.batchBuffer = new ArrayList<>(properties.getBulkSize());

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "es-log-bulk-consumer");
            t.setDaemon(true);
            return t;
        });

        logger.info("ElasticsearchLogClient bean constructed, hosts={}", properties.getHosts());
    }

    /* =========================
       Spring 生命周期
       ========================= */

    /**
     * 容器启动完成后再启动后台任务（关键）
     */
    @PostConstruct
    public void start() {
        if (properties.isBulkEnabled()) {
            startBulkProcessor();
        }
        startHealthCheck();

        logger.info("ElasticsearchLogClient background tasks started");
    }

    @PreDestroy
    @Override
    public void destroy() {
        running.set(false);
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("ElasticsearchLogClient shutdown complete");
    }

    /* =========================
       对外 API
       ========================= */

    public void sendAsync(EsLogDocument document) {
        if (!properties.isAsync()) {
            sendSync(document);
            return;
        }

        try {
            if (!queue.offer(document, 100, TimeUnit.MILLISECONDS)) {
                logger.warn("ES log queue full, discard log: {}", document.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sendSync(EsLogDocument document) {
        try {
            String index = resolveIndexName();

            IndexRequest request = new IndexRequest(index)
                    .source(objectMapper.writeValueAsBytes(document), XContentType.JSON);

            client.index(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            logger.error("Failed to send log to Elasticsearch", e);
        }
    }

    public void sendBulk(List<EsLogDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        try {
            String index = resolveIndexName();
            BulkRequest bulkRequest = new BulkRequest()
                    .timeout(properties.getTimeout() + "ms");

            for (EsLogDocument doc : documents) {
                bulkRequest.add(
                        new IndexRequest(index)
                                .source(objectMapper.writeValueAsBytes(doc), XContentType.JSON)
                );
            }

            if (properties.isRefreshAfterWrite()) {
                bulkRequest.setRefreshPolicy("wait_for");
            }

            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);

            if (response.hasFailures()) {
                logger.error("Bulk request failures: {}", response.buildFailureMessage());
            }
        } catch (Exception e) {
            logger.error("Failed to send bulk logs to Elasticsearch", e);
        }
    }

    public boolean isHealthy() {
        try {
            return client.ping(RequestOptions.DEFAULT);
        } catch (Exception e) {
            return false;
        }
    }

    public int getQueueSize() {
        return queue.size();
    }

    /* =========================
       内部调度逻辑
       ========================= */

    private void startBulkProcessor() {
        scheduler.scheduleWithFixedDelay(() -> {
            if (!running.get()) {
                return;
            }
            consumeBatchSafely();
        }, properties.getBulkInterval(), properties.getBulkInterval(), TimeUnit.MILLISECONDS);
    }

    private void consumeBatchSafely() {
        try {
            int count = queue.drainTo(batchBuffer, properties.getBulkSize());
            if (count > 0) {
                sendBulk(batchBuffer);
                batchBuffer.clear();
            }
        } catch (Exception e) {
            logger.error("Bulk consume error", e);
        }
    }

    private void startHealthCheck() {
        scheduler.scheduleWithFixedDelay(() -> {
            if (!isHealthy()) {
                logger.warn("Elasticsearch connection unhealthy");
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private String resolveIndexName() {
        return indexPatternResolver.resolve(properties.getIndex());
    }

    /* =========================
       索引管理
       ========================= */

    public void createIndexIfNotExists(String indexName) throws IOException {
        boolean exists = client.indices()
                .exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);

        if (!exists) {
            CreateIndexRequest create = new CreateIndexRequest(indexName);
            create.settings(Settings.builder()
                    .put("index.number_of_shards", 1)
                    .put("index.number_of_replicas", 0)
            );
            client.indices().create(create, RequestOptions.DEFAULT);
            logger.info("Created index: {}", indexName);
        }
    }

    public void createIndexWithMapping(String indexName, String mappingJson) throws IOException {
        boolean exists = client.indices()
                .exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);

        if (!exists) {
            CreateIndexRequest create = new CreateIndexRequest(indexName);
            create.source(mappingJson, XContentType.JSON);
            client.indices().create(create, RequestOptions.DEFAULT);
            logger.info("Created index with mapping: {}", indexName);
        }
    }
}
