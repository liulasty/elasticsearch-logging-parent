package com.lz.logging.client;

import com.lz.logging.autoconfigure.ElasticsearchLoggingProperties;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Elasticsearch 客户端工厂
 * 使用新的 Java API Client
 * @author Administrator
 */

public class RestClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(RestClientFactory.class);
    private static volatile RestClient restClient;
    private static volatile ElasticsearchClient elasticsearchClient;

    /**
     * 创建 Elasticsearch 客户端（单例模式）
     */
    public static ElasticsearchClient createElasticsearchClient(ElasticsearchLoggingProperties properties) {
        if (elasticsearchClient == null) {
            synchronized (RestClientFactory.class) {
                if (elasticsearchClient == null) {
                    restClient = createRestClientWithPool(properties);
                    RestClientTransport transport = new RestClientTransport(
                            restClient,
                            new JacksonJsonpMapper()
                    );
                    elasticsearchClient = new ElasticsearchClient(transport);
                }
            }
        }
        return elasticsearchClient;
    }

    /**
     * 创建带连接池的 RestClient
     */
    private static RestClient createRestClientWithPool(ElasticsearchLoggingProperties properties) {
        // 解析 hosts
        String[] hostArray = properties.getHosts().split(",");
        HttpHost[] httpHosts = new HttpHost[hostArray.length];

        for (int i = 0; i < hostArray.length; i++) {
            String[] hostPort = hostArray[i].trim().split(":");
            String host = hostPort[0];
            int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 9200;
            httpHosts[i] = new HttpHost(host, port, properties.getScheme());
        }

        // 构建 RestClientBuilder
        RestClientBuilder builder = RestClient.builder(httpHosts);

        // 设置认证
        if (properties.getUsername() != null && properties.getPassword() != null) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword())
            );

            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                // 设置连接池配置
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

                // 连接池配置
                httpClientBuilder.setMaxConnTotal(properties.getMaxConnTotal()); // 最大连接数
                httpClientBuilder.setMaxConnPerRoute(properties.getMaxConnPerRoute()); // 每路由最大连接数

                // IO 线程配置
                httpClientBuilder.setDefaultIOReactorConfig(
                        IOReactorConfig.custom()
                                .setIoThreadCount(Runtime.getRuntime().availableProcessors())
                                .setSoKeepAlive(true)
                                .build()
                );

                return httpClientBuilder;
            });
        } else {
            // 无认证时的连接池配置
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder.setMaxConnTotal(properties.getMaxConnTotal());
                httpClientBuilder.setMaxConnPerRoute(properties.getMaxConnPerRoute());

                httpClientBuilder.setDefaultIOReactorConfig(
                        IOReactorConfig.custom()
                                .setIoThreadCount(Runtime.getRuntime().availableProcessors())
                                .setSoKeepAlive(true)
                                .build()
                );

                return httpClientBuilder;
            });
        }

        // 设置连接超时和重试
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(properties.getConnectTimeout())
                        .setSocketTimeout(properties.getSocketTimeout())
                        .setConnectionRequestTimeout(properties.getConnectionRequestTimeout())
        );


        // 开启压缩
        builder.setCompressionEnabled(true);

        restClient = builder.build();
        return restClient;
    }


}