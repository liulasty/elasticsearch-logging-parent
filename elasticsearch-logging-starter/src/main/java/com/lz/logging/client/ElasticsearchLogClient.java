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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Elasticsearch 日志客户端（ES 7.17.x）
 *
 * - Java 8
 * - Spring Boot 2.7.x
 * - RestHighLevelClient
 */

public class ElasticsearchLogClient {

    private static final Logger logger =
            LoggerFactory.getLogger(ElasticsearchLogClient.class);

    private final RestHighLevelClient client;
    private final ElasticsearchLoggingProperties properties;
    private final IndexPatternResolver indexResolver;
    private final ObjectMapper objectMapper;

    private final BlockingQueue<EsLogDocument> queue;
    private final List<EsLogDocument> batchBuffer;

    private final ScheduledExecutorService bulkScheduler;
    private final ScheduledExecutorService healthScheduler;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong dropCounter = new AtomicLong();

    public ElasticsearchLogClient(ElasticsearchLoggingProperties properties) {
        this(properties, null);
    }

    public ElasticsearchLogClient(ElasticsearchLoggingProperties properties,
                                  ObjectMapper objectMapper) {

        this.properties = properties;
        this.indexResolver = new IndexPatternResolver();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        this.client = RestClientFactory.createElasticsearchClient(properties);

        this.queue = new LinkedBlockingQueue<>(properties.getQueueSize());
        this.batchBuffer = new ArrayList<>(properties.getBulkSize());

        this.bulkScheduler = Executors.newSingleThreadScheduledExecutor(r ->
                new Thread(r, "es-log-bulk-consumer"));

        this.healthScheduler = Executors.newSingleThreadScheduledExecutor(r ->
                new Thread(r, "es-log-health-check"));

        logger.info("ElasticsearchLogClient initialized, hosts={}", properties.getHosts());
    }

    /* ================= 生命周期 ================= */

    @PostConstruct
    public void start() {
        if (properties.isBulkEnabled()) {
            startBulkProcessor();
        }
        startHealthCheck();
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);

        bulkScheduler.shutdown();
        healthScheduler.shutdown();

        try {
            bulkScheduler.awaitTermination(5, TimeUnit.SECONDS);
            healthScheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            client.close();
        } catch (IOException e) {
            logger.warn("Failed to close Elasticsearch client", e);
        }

        logger.info("ElasticsearchLogClient shutdown completed");
    }

    /* ================= 对外 API ================= */

    public void sendAsync(EsLogDocument document) {
        if (!properties.isAsync()) {
            sendSync(document);
            return;
        }

        if (!queue.offer(document)) {
            long dropped = dropCounter.incrementAndGet();
            if (dropped % 1000 == 0) {
                logger.warn("ES log queue full, dropped {} logs", dropped);
            }
        }
    }

    public void sendSync(EsLogDocument document) {
        try {
            IndexRequest request = new IndexRequest(resolveIndex())
                    .source(objectMapper.writeValueAsBytes(document), XContentType.JSON);
            client.index(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            logger.error("Failed to send log to Elasticsearch", e);
        }
    }

    /* ================= 内部逻辑 ================= */

    private void startBulkProcessor() {
        bulkScheduler.scheduleWithFixedDelay(
                this::consumeBatchSafely,
                properties.getBulkInterval(),
                properties.getBulkInterval(),
                TimeUnit.MILLISECONDS
        );
    }

    private void consumeBatchSafely() {
        if (!running.get()) {
            return;
        }
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

    private void sendBulk(List<EsLogDocument> batchBuffer) {
        BulkRequest bulkRequest = new BulkRequest();

        for (EsLogDocument document : batchBuffer) {
            try {
                String index = resolveIndex();
                IndexRequest request = new IndexRequest(index)
                        .source(objectMapper.writeValueAsBytes(document), XContentType.JSON);
                bulkRequest.add(request);
            } catch (Exception e) {
                logger.error("Failed to serialize document for bulk request", e);
            }
        }

        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (response.hasFailures()) {
                logger.error("Bulk request had failures: {}", response.buildFailureMessage());
            }
        } catch (IOException e) {
            logger.error("Failed to execute bulk request", e);
        }
    }

    private void startHealthCheck() {
        healthScheduler.scheduleWithFixedDelay(() -> {
            if (!isHealthy()) {
                logger.warn("Elasticsearch connection unhealthy");
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private String resolveIndex() {
        return indexResolver.resolve(properties.getIndex());
    }

    public boolean isHealthy() {
        try {
            return client.ping(RequestOptions.DEFAULT);
        } catch (Exception e) {
            return false;
        }
    }
}

