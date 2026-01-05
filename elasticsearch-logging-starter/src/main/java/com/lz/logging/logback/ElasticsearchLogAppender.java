package com.lz.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.lz.logging.autoconfigure.ElasticsearchLoggingProperties;
import com.lz.logging.client.ElasticsearchLogClient;
import com.lz.logging.model.EsLogDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch Logback Appender - 将日志发送到 Elasticsearch 的自定义 Appender
 *
 * 此 Appender 继承自 Logback 的 AppenderBase，用于将应用程序日志发送到 Elasticsearch 存储。
 * 支持同步和异步两种发送模式，通过配置可以控制日志发送的方式。
 *
 * @author Administrator
 * @since 1.0.0
 */

public class ElasticsearchLogAppender extends AppenderBase<ILoggingEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchLogAppender.class);

    /**
     * Elasticsearch 日志客户端，用于与 Elasticsearch 服务进行通信
     */
    private ElasticsearchLogClient elasticsearchLogClient;

    /**
     * 日志事件转换器，用于将 Logback 的 ILoggingEvent 转换为 Elasticsearch 可存储的文档格式
     */
    private final LogEventConverter logEventConverter;

    /**
     * 是否启用异步发送模式
     * 异步模式下，日志会通过异步方式发送到 Elasticsearch，提高性能
     * 同步模式下，日志会同步发送到 Elasticsearch，保证日志发送的实时性
     */
    private boolean async = true;

    /**
     * 无参构造函数，初始化日志事件转换器
     * 注意：使用此构造函数时，需要通过 setter 方法设置 ElasticsearchLogClient
     */
    public ElasticsearchLogAppender() {
        this.logEventConverter = new LogEventConverter();
    }

    /**
     * 构造函数，使用指定的客户端和配置属性初始化 Appender
     *
     * @param elasticsearchLogClient Elasticsearch日志客户端，用于发送日志数据
     * @param properties 配置属性对象，用于设置异步模式等参数
     * @throws IllegalArgumentException 如果参数为 null，则抛出此异常
     */
    public ElasticsearchLogAppender(ElasticsearchLogClient elasticsearchLogClient,
                                    ElasticsearchLoggingProperties properties) {
        this.elasticsearchLogClient = elasticsearchLogClient;
        this.logEventConverter = new LogEventConverter();
        // 根据配置设置异步模式
        this.async = properties.isAsync();
    }

    /**
     * 追加日志事件到 Elasticsearch
     *
     * 此方法是 Appender 的核心方法，当有日志事件产生时会被调用。
     * 将 Logback 的 ILoggingEvent 转换为 Elasticsearch 文档格式，
     * 然后根据配置的异步/同步模式发送到 Elasticsearch。
     *
     * @param eventObject Logback 日志事件对象，包含日志的所有信息
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        try {
            // 将 Logback 事件转换为 ES 文档
            EsLogDocument document = logEventConverter.convert(eventObject);

            // 发送日志到 Elasticsearch
            if (async) {
                elasticsearchLogClient.sendAsync(document);
            } else {
                elasticsearchLogClient.sendSync(document);
            }
        } catch (Exception e) {
            addError("Failed to send log to Elasticsearch", e);
        }
    }

    /**
     * 启动 Appender
     *
     * 在 Appender 启动时验证必要的依赖是否已正确注入，
     * 如果 ElasticsearchLogClient 为 null，则记录错误并阻止 Appender 启动。
     */
    @Override
    public void start() {
        super.start();
        if (elasticsearchLogClient == null) {
            addError("ElasticsearchLogClient is null, appender will not start");
        }
    }

    /**
     * 停止 Appender
     *
     * 在 Appender 停止时，关闭 Elasticsearch 客户端连接，
     * 释放相关资源，并记录停止日志。
     */
    @Override
    public void stop() {
        super.stop();
        if (elasticsearchLogClient != null) {
            elasticsearchLogClient.shutdown();
        }
        logger.info("ElasticsearchLogAppender stopped");
    }

    // Getter 和 Setter 方法
    /**
     * 设置 Elasticsearch 日志客户端
     *
     * @param elasticsearchLogClient Elasticsearch 日志客户端实例
     */
    public void setElasticsearchLogClient(ElasticsearchLogClient elasticsearchLogClient) {
        this.elasticsearchLogClient = elasticsearchLogClient;
    }

    /**
     * 获取 Elasticsearch 日志客户端
     *
     * @return 当前使用的 Elasticsearch 日志客户端实例
     */
    public ElasticsearchLogClient getElasticsearchLogClient() {
        return elasticsearchLogClient;
    }

    /**
     * 设置异步发送模式
     *
     * @param async true 表示启用异步发送，false 表示使用同步发送
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * 获取异步发送模式状态
     *
     * @return true 表示当前为异步发送模式，false 表示为同步发送模式
     */
    public boolean isAsync() {
        return async;
    }
}
