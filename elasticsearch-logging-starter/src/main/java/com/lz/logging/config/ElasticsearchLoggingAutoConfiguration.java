package com.lz.logging.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.lz.logging.core.client.ElasticsearchLogClient;
import com.lz.logging.logback.ElasticsearchLogAppender;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch Logging 自动配置类
 *
 * 职责说明：
 * 1. 创建 ElasticsearchLogClient
 * 2. 创建并配置 ElasticsearchLogAppender
 * 3. 在 Spring Boot 启动完成后，将 Appender 挂载到 Root Logger
 * 4. 在容器关闭时，安全停止 Appender
 *
 * 设计原则：
 * - Appender 不作为 @Component
 * - 不使用 @PostConstruct
 * - 启动逻辑通过 ApplicationRunner 执行
 * - 销毁逻辑通过 @PreDestroy 执行
 *
 * 适配版本：
 * - Spring Boot 2.7.x
 * - Logback Classic
 */
@Configuration
@ConditionalOnClass({ Logger.class, ElasticsearchLogClient.class })
@EnableConfigurationProperties(ElasticsearchLoggingProperties.class)
@ConditionalOnProperty(
        prefix = "es.logging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ElasticsearchLoggingAutoConfiguration {

    /**
     * Appender 在 Logback 中的唯一名称
     */
    private static final String APPENDER_NAME = "ELASTICSEARCH";

    /**
     * 创建 Elasticsearch 日志客户端
     */
    @Bean
    public ElasticsearchLogClient elasticsearchLogClient(
            ElasticsearchLoggingProperties properties) {
        return new ElasticsearchLogClient(properties);
    }

    /**
     * 创建 Elasticsearch Logback Appender
     *
     * 注意：
     * - 这里只做对象构造和基础属性设置
     * - 不在此处 start()
     */
    @Bean
    public ElasticsearchLogAppender elasticsearchLogAppender(
            ElasticsearchLogClient client,
            ElasticsearchLoggingProperties properties) {

        LoggerContext context =
                (LoggerContext) LoggerFactory.getILoggerFactory();

        ElasticsearchLogAppender appender =
                new ElasticsearchLogAppender(client, properties);

        appender.setName(APPENDER_NAME);
        appender.setContext(context);

        return appender;
    }

    /**
     * 在 Spring Boot 启动完成后，将 Appender 挂载到 Root Logger
     *
     * 使用 ApplicationRunner 的原因：
     * - 保证 Spring 容器、环境、日志系统全部初始化完成
     * - 避免生命周期冲突
     */
    @Bean
    public ApplicationRunner elasticsearchAppenderInitializer(
            ElasticsearchLogAppender appender) {

        return args -> {
            LoggerContext context =
                    (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger =
                    context.getLogger(Logger.ROOT_LOGGER_NAME);

            // 防止重复添加
            if (rootLogger.getAppender(APPENDER_NAME) == null) {
                appender.start();
                rootLogger.addAppender(appender);
            }
        };
    }
}
