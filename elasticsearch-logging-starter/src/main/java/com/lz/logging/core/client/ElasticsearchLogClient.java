package com.lz.logging.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz.logging.config.ElasticsearchLoggingProperties;
import com.lz.logging.core.model.EsLogDocument;
import com.lz.logging.core.util.IndexPatternResolver;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elasticsearch 日志客户端（ES 7.17.x）
 * <p>
 * 优化：使用官方 BulkProcessor 实现高性能异步批量发送
 */
public class ElasticsearchLogClient {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchLogClient.class);

    private final RestHighLevelClient client;
    private final ElasticsearchLoggingProperties properties;
    private final IndexPatternResolver indexResolver;
    private final ObjectMapper objectMapper;

    private BulkProcessor bulkProcessor;
    private final ScheduledExecutorService healthScheduler;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public ElasticsearchLogClient(ElasticsearchLoggingProperties properties) {
        this(properties, null);
    }

    public ElasticsearchLogClient(ElasticsearchLoggingProperties properties,
                                  ObjectMapper objectMapper) {

        this.properties = properties;
        this.indexResolver = new IndexPatternResolver();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        this.client = RestClientFactory.createElasticsearchClient(properties);

        this.healthScheduler = Executors.newSingleThreadScheduledExecutor(r ->
                new Thread(r, "es-log-health-check"));

        logger.info("ElasticsearchLogClient initialized, hosts={}", properties.getHosts());
    }

    @PostConstruct
    public void start() {
        if (properties.isBulkEnabled()) {
            initBulkProcessor();
        }
        startHealthCheck();
    }

    private void initBulkProcessor() {
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                // 可选：记录 debug 日志
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                if (response.hasFailures()) {
                    logger.warn("Bulk execution completed with failures: {}", response.buildFailureMessage());
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                logger.error("Failed to execute bulk", failure);
            }
        };

        BulkProcessor.Builder builder = BulkProcessor.builder(
                (request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                listener);

        // 设置刷新条件
        builder.setBulkActions(properties.getBulkSize());
        // 默认 5MB
        builder.setBulkSize(new ByteSizeValue(5, ByteSizeUnit.MB));
        builder.setFlushInterval(TimeValue.timeValueMillis(properties.getBulkInterval()));
        builder.setConcurrentRequests(properties.getConcurrentRequests());
        // 指数退避重试
        builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), properties.getMaxRetries()));

        this.bulkProcessor = builder.build();
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);

        if (bulkProcessor != null) {
            try {
                // 等待所有任务完成，缩短等待时间以避免阻塞应用关闭
                boolean terminated = bulkProcessor.awaitClose(3, TimeUnit.SECONDS);
                if (!terminated) {
                    logger.warn("BulkProcessor did not terminate in 3s");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        healthScheduler.shutdownNow(); // 直接停止健康检查，无需等待
        try {
            if (!healthScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                 logger.warn("Health scheduler did not terminate in 1s");
            }
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
        if (!properties.isAsync() || bulkProcessor == null) {
            sendSync(document);
            return;
        }

        try {
            String index = resolveIndex();
            IndexRequest request = new IndexRequest(index)
                    .source(objectMapper.writeValueAsBytes(document), XContentType.JSON);
            bulkProcessor.add(request);
        } catch (Exception e) {
            logger.error("Failed to add log to bulk processor", e);
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

    private void startHealthCheck() {
        healthScheduler.scheduleWithFixedDelay(() -> {
            try {
                boolean healthy = client.ping(RequestOptions.DEFAULT);
                if (!healthy) {
                    logger.warn("Elasticsearch connection unhealthy");
                }
            } catch (Exception e) {
                logger.warn("Elasticsearch health check failed", e);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private String resolveIndex() {
        return indexResolver.resolve(properties.getIndex());
    }
}
