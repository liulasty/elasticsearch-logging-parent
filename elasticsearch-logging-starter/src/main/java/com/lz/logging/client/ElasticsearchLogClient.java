package com.lz.logging.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz.logging.autoconfigure.ElasticsearchLoggingProperties;
import com.lz.logging.model.EsLogDocument;
import com.lz.logging.support.IndexPatternResolver;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elasticsearch 日志客户端
 * 使用新的 Java API Client
 * @author Administrator
 */
public class ElasticsearchLogClient implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchLogClient.class);

    private final ElasticsearchClient client;
    private final ElasticsearchLoggingProperties properties;
    private final IndexPatternResolver indexPatternResolver;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // 批量处理相关
    private final BlockingQueue<EsLogDocument> queue;
    private final List<EsLogDocument> batchBuffer;

    public ElasticsearchLogClient(ElasticsearchLoggingProperties properties) {
        this.properties = properties;
        this.indexPatternResolver = new IndexPatternResolver();
        this.objectMapper = new ObjectMapper();

        // 创建 ES 客户端
        this.client = RestClientFactory.createElasticsearchClient(properties);

        // 初始化队列和缓冲区
        this.queue = new LinkedBlockingQueue<>(properties.getQueueSize());
        this.batchBuffer = new ArrayList<>(properties.getBulkSize());

        // 初始化调度器
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "es-log-bulk-consumer");
            thread.setDaemon(true);
            return thread;
        });

        // 启动批量处理器
        startBulkProcessor();

        // 启动健康检查
        startHealthCheck();

        logger.info("ElasticsearchLogClient initialized with hosts: {}", properties.getHosts());
    }

    /**
     * 发送单条日志（异步）
     */
    public void sendAsync(EsLogDocument document) {
        if (!properties.isAsync()) {
            sendSync(document);
            return;
        }

        try {
            // 尝试放入队列，如果队列满了则丢弃
            if (!queue.offer(document, 100, TimeUnit.MILLISECONDS)) {
                logger.warn("ES log queue is full, discard log: {}", document.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while offering log to queue");
        }
    }

    /**
     * 发送单条日志（同步）
     */
    public void sendSync(EsLogDocument document) {
        try {
            String index = resolveIndexName();

            IndexRequest<EsLogDocument> request = IndexRequest.of(idx -> idx
                    .index(index)
                    .document(document)
            );

            client.index(request);

            logger.debug("Successfully sent log to Elasticsearch index: {}", index);
        } catch (IOException e) {
            logger.error("Failed to send log to Elasticsearch", e);
        }
    }

    /**
     * 批量发送日志
     */
    public void sendBulk(List<EsLogDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        try {
            String index = resolveIndexName();
            List<BulkOperation> operations = new ArrayList<>(documents.size());

            for (EsLogDocument doc : documents) {
                operations.add(BulkOperation.of(b -> b
                        .index(IndexOperation.of(i -> i
                                .index(index)
                                .document(doc)
                        ))
                ));
            }

            // 构建批量请求 - 使用正确的方法
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            bulkBuilder.operations(operations);
            bulkBuilder.timeout(t -> t.time(properties.getTimeout() + "ms"));

            // 设置刷新策略
            if (properties.isRefreshAfterWrite()) {
                bulkBuilder.refresh(Refresh.WaitFor);
            } else {
                bulkBuilder.refresh(Refresh.False);
            }

            BulkRequest bulkRequest = bulkBuilder.build();

            // 执行批量请求
            BulkResponse response = client.bulk(bulkRequest);

            // 检查失败项 - 使用正确的 API
            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        logger.error("Bulk request failed for item: {}", item.error().reason());
                    }
                    if (item.status() >= 400) {
                        logger.warn("Bulk request failed for item: {}", item.status());
                    }
                }
            }

            logger.debug("Bulk request completed with {} items, took {} ms",
                    documents.size(), response.took());

        } catch (Exception e) {
            logger.error("Failed to send bulk logs to Elasticsearch", e);
        }
    }

    /**
     * 检查 ES 连接状态
     */
    public boolean isHealthy() {
        try {
            return client.ping().value();
        } catch (Exception e) {
            logger.warn("Elasticsearch health check failed", e);
            return false;
        }
    }



    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return queue.size();
    }

    // 私有方法

    private void startBulkProcessor() {
        if (!properties.isBulkEnabled()) {
            return;
        }

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                consumeBatch();
            } catch (Exception e) {
                logger.error("Error in bulk processor", e);
            }
        }, properties.getBulkInterval(), properties.getBulkInterval(), TimeUnit.MILLISECONDS);
    }

    private void consumeBatch() {
        try {
            // 从队列中取出消息
            int count = queue.drainTo(batchBuffer, properties.getBulkSize());

            if (count > 0) {
                logger.debug("Processing batch of {} logs", count);
                sendBulk(batchBuffer);
                batchBuffer.clear();
            }
        } catch (Exception e) {
            logger.error("Failed to consume batch", e);
        }
    }

    private void flushRemainingLogs() {
        try {
            // 取出队列中所有剩余消息
            List<EsLogDocument> remaining = new ArrayList<>();
            queue.drainTo(remaining);

            if (!remaining.isEmpty()) {
                logger.info("Flushing {} remaining logs to Elasticsearch", remaining.size());
                sendBulk(remaining);
            }
        } catch (Exception e) {
            logger.error("Failed to flush remaining logs", e);
        }
    }

    private void startHealthCheck() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (!isHealthy()) {
                    logger.warn("Elasticsearch connection is unhealthy");
                }
            } catch (Exception e) {
                // 忽略健康检查异常
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private String resolveIndexName() {
        return indexPatternResolver.resolve(properties.getIndex());
    }

    // 索引管理相关方法

    /**
     * 创建索引（如果不存在）
     */
    public void createIndexIfNotExists(String indexName) throws IOException {
        boolean exists = client.indices()
                .exists(e -> e.index(indexName))
                .value();

        if (!exists) {
            client.indices().create(c -> c.index(indexName));
            logger.info("Created index: {}", indexName);
        }
    }

    /**
     * 创建索引并指定映射
     */
    public void createIndexWithMapping(String indexName, String mappingJson) throws IOException {
        boolean exists = client.indices()
                .exists(e -> e.index(indexName))
                .value();

        if (!exists) {
            client.indices().create(c -> c
                    .index(indexName)
                    .withJson(new java.io.StringReader(mappingJson))
            );
            logger.info("Created index with mapping: {}", indexName);
        }
    }

    @Override
    public void destroy() {
    }

    public void close() {
        scheduler.shutdown();
    }
}