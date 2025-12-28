package com.lz.logging.autoconfigure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.lz.logging.logback.ElasticsearchLogAppender;
import com.lz.logging.client.ElasticsearchLogClient;
import com.lz.logging.autoconfigure.ElasticsearchLoggingProperties;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Elasticsearch Logging 自动配置类
 *
 * 该类负责自动配置 Elasticsearch 日志记录功能，包括创建日志客户端、
 * Logback Appender 以及将 Appender 挂载到根日志记录器等操作。
 *
 * 该配置仅在以下条件满足时生效：
 * 1. 类路径下存在 Logger 和 ElasticsearchLogClient 类
 * 2. 配置属性 es.logging.enabled 设置为 true
 *
 * @author lingma
 * @version 1.0
 * @since 2023
 */
@Configuration
@ConditionalOnClass({Logger.class, ElasticsearchLogClient.class})
@EnableConfigurationProperties(ElasticsearchLoggingProperties.class)
@ConditionalOnProperty(
        prefix = "es.logging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ElasticsearchLoggingAutoConfiguration implements DisposableBean {


    /**
     * Elasticsearch Appender 的名称常量
     */
    private static final String APPENDER_NAME = "ELASTICSEARCH";

    /**
     * 注入的配置属性对象
     */
    @Autowired
    private ElasticsearchLoggingProperties properties;



    /**
     * 创建 Elasticsearch 日志客户端 Bean
     *
     * 该方法创建一个 ElasticsearchLogClient 实例，并配置其在 Spring 容器销毁时自动关闭
     *
     * @param properties Elasticsearch 日志配置属性
     * @return 配置好的 ElasticsearchLogClient 实例
     */
    @Bean(destroyMethod = "close")
    public ElasticsearchLogClient elasticsearchLogClient(
            ElasticsearchLoggingProperties properties) {
        return new ElasticsearchLogClient(properties);
    }

    /**
     * 创建 Logback Appender Bean
     *
     * 该方法创建一个 ElasticsearchLogAppender 实例，用于将日志发送到 Elasticsearch
     *
     * @param client Elasticsearch 日志客户端
     * @param properties Elasticsearch 日志配置属性
     * @return 配置好的 ElasticsearchLogAppender 实例
     */
    @Bean
    public ElasticsearchLogAppender elasticsearchLogAppender(
            ElasticsearchLogClient client,
            ElasticsearchLoggingProperties properties) {

        ElasticsearchLogAppender appender = new ElasticsearchLogAppender(client, properties);

        appender.setName(APPENDER_NAME);
        return appender;
    }

    /**
     * 在 Spring 容器启动完成后，将 Appender 挂载到 Root Logger
     *
     * 该方法在 Spring 容器初始化完成后自动执行，将 ElasticsearchLogAppender 添加到
     * 根日志记录器，使所有日志都能通过该 Appender 发送到 Elasticsearch。
     * 同时确保 Appender 已正确启动且避免重复添加。
     */
    @PostConstruct
    public void attachAppender() {

        LoggerContext context =
                (LoggerContext) LoggerFactory.getILoggerFactory();

        // 从 Spring 容器获取 appender 实例
        ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(APPENDER_NAME);
        
        if (appender == null) {
            // 如果 appender 未挂载，创建并挂载
            // 注意：这里直接创建实例而不是通过Spring Bean方法，避免循环依赖
            ElasticsearchLogClient client = new ElasticsearchLogClient(properties);
            ElasticsearchLogAppender esAppender = new ElasticsearchLogAppender(client, properties);
            esAppender.setName(APPENDER_NAME);
            esAppender.setContext(context);

            if (!esAppender.isStarted()) {
                esAppender.start();
            }

            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

            // 避免重复挂载
            if (rootLogger.getAppender(APPENDER_NAME) == null) {
                rootLogger.addAppender(esAppender);
            }
        } else {
            // 如果 appender 已存在但类型是通用 Appender，需要确保其上下文设置正确
            if (appender.getContext() == null) {
                appender.setContext(context);
            }
            if (!appender.isStarted()) {
                appender.start();
            }
        }
    }

    /**
     * 实现 DisposableBean 接口，在 Spring 容器关闭时清理资源
     *
     * 该方法确保在 Spring 应用关闭时，ElasticsearchLogAppender 被正确停止，
     * 避免资源泄漏和日志写入异常。
     *
     * @throws Exception 如果停止 Appender 过程中发生异常
     */
    @Override
    public void destroy() throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender = rootLogger.getAppender(APPENDER_NAME);
        
        if (appender != null && appender.isStarted()) {
            appender.stop();
        }
    }
}
