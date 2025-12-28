package com.lz.logging.client;

import com.lz.logging.autoconfigure.ElasticsearchLoggingProperties;
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
                        .setConnectionRequestTimeout(
                                properties.getConnectionRequestTimeout()
                        )
        );

        // 开启 HTTP 压缩
        builder.setCompressionEnabled(true);

        return new RestHighLevelClient(builder);
    }

    /**
     * 解析 hosts 配置
     * 示例：127.0.0.1:9200,127.0.0.2:9200
     */
    private static HttpHost[] parseHosts(String hosts, String scheme) {
        String[] hostArray = hosts.split(",");
        HttpHost[] httpHosts = new HttpHost[hostArray.length];

        for (int i = 0; i < hostArray.length; i++) {
            String hostPort = hostArray[i].trim();
            String[] parts = hostPort.split(":");

            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;

            httpHosts[i] = new HttpHost(host, port, scheme);
        }
        return httpHosts;
    }

    /**
     * 应用关闭时调用，释放资源
     */
    public static void close() {
        if (client != null) {
            try {
                client.close();
                logger.info("Elasticsearch RestHighLevelClient closed");
            } catch (Exception e) {
                logger.error("Failed to close Elasticsearch client", e);
            }
        }
    }
}
