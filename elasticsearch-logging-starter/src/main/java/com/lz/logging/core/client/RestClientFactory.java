package com.lz.logging.core.client;

import com.lz.logging.config.ElasticsearchLoggingProperties;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * Elasticsearch RestHighLevelClient 工厂
 *
 * 适用版本：
 * - Java 8
 * - Spring Boot 2.7.x
 * - Elasticsearch 7.17.x
 *
 * 特点：
 * - 线程安全单例
 * - 支持认证
 * - 支持连接池
 * - 无 jakarta 依赖
 *
 * @author Administrator
 */
public class RestClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(RestClientFactory.class);

    private static volatile RestHighLevelClient client;

    /**
     * 获取 Elasticsearch 客户端（单例）
     */
    public static RestHighLevelClient createElasticsearchClient(
            ElasticsearchLoggingProperties properties) {

        if (client == null) {
            synchronized (RestClientFactory.class) {
                if (client == null) {
                    client = buildClient(properties);
                    logger.info("Elasticsearch RestHighLevelClient initialized");
                }
            }
        }
        return client;
    }

    /**
     * 构建 RestHighLevelClient
     */
    private static RestHighLevelClient buildClient(ElasticsearchLoggingProperties properties) {

        HttpHost[] httpHosts = parseHosts(properties.getHosts(), properties.getScheme());

        RestClientBuilder builder = RestClient.builder(httpHosts);

        // HTTP Client 配置
        builder.setHttpClientConfigCallback(httpClientBuilder -> {

            // 认证（可选）
            if (properties.getUsername() != null && properties.getPassword() != null) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(
                                properties.getUsername(),
                                properties.getPassword()
                        )
                );
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            // 连接池配置
            httpClientBuilder
                    .setMaxConnTotal(properties.getMaxConnTotal())
                    .setMaxConnPerRoute(properties.getMaxConnPerRoute())
                    .setDefaultIOReactorConfig(
                            IOReactorConfig.custom()
                                    .setIoThreadCount(
                                            Runtime.getRuntime().availableProcessors()
                                    )
                                    .setSoKeepAlive(true)
                                    .build()
                    );

            return httpClientBuilder;
        });

        // 请求超时配置
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(properties.getConnectTimeout())
                        .setSocketTimeout(properties.getSocketTimeout())
        );

        return new RestHighLevelClient(builder);
    }

    private static HttpHost[] parseHosts(String hosts, String scheme) {
        return Arrays.stream(hosts.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    String[] parts = s.split(":");
                    String host = parts[0];
                    int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
                    return new HttpHost(host, port, scheme);
                })
                .toArray(HttpHost[]::new);
    }
}
