package com.lz.logging.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch 日志记录配置属性类
 * <p>该类用于配置与 Elasticsearch 日志记录相关的各项参数，包括连接信息、索引设置、日志级别等。</p>
 *
 * @author Administrator
 */
@Component
@ConfigurationProperties(prefix = "es.logging")
public class ElasticsearchLoggingProperties {

    /**
     * 是否启用 ES 日志收集
     */
    private boolean enabled = false;

    private String scheme = "http";

    /**
     * ES 连接地址，支持多个：host1:9200,host2:9200
     */
    private String hosts = "localhost:9200";

    /**
     * 索引名称，支持日期格式：app-logs-%{yyyy.MM.dd}
     */
    private String index = "app-logs";

    /**
     * 应用名称，自动获取 spring.application.name
     */
    private String applicationName;

    /**
     * 环境标识，自动获取 spring.profiles.active
     */
    private String environment = "dev";

    /**
     * 最低日志级别：TRACE, DEBUG, INFO, WARN, ERROR
     */
    private String minLevel = "INFO";

    /**
     * ES 连接超时（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * ES 读写超时（毫秒）
     */
    private int socketTimeout = 30000;

    /**
     * ES 认证用户名
     */
    private String username;

    /**
     * ES 认证密码
     */
    private String password;

    /**
     * 是否启用异步发送
     */
    private boolean async = true;

    /**
     * 异步队列大小
     */
    private int queueSize = 10000;

    /**
     * 是否启用批量发送
     */
    private boolean bulkEnabled = true;

    /**
     * 批量大小
     */
    private int bulkSize = 1000;

    /**
     * 批量发送间隔（毫秒）
     */
    private int bulkInterval = 5000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 自定义字段映射
     */
    private Map<String, String> customFields = new HashMap<>();

    /**
     * 要排除的 logger 名称
     */
    private String[] excludeLoggers = {
            "org.springframework",
            "org.apache",
            "com.zaxxer.hikari"
    };

    // 连接配置
    private int connectionRequestTimeout = 1000;
    private int maxConnTotal = 30;
    private int maxConnPerRoute = 10;




    // 发送配置
    private boolean refreshAfterWrite = false;
    private int timeout = 60000;  // 60秒


    // 连接保持配置
    private boolean keepAlive = true;
    private int keepAliveTimeout = 30000;    // 30秒

    // 重试配置
    private int maxRetryTimeout = 30000;     // 最大重试超时
    private int retryCount = 3;              // 重试次数


    // Getters and Setters

    /**
     * 获取是否启用 ES 日志收集
     *
     * @return 如果启用 ES 日志收集返回 true，否则返回 false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 ES 日志收集
     *
     * @param enabled 是否启用 ES 日志收集
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取 ES 连接地址列表
     * <p>支持多个地址，格式为：host1:9200,host2:9200</p>
     *
     * @return ES 连接地址列表
     */
    public String getHosts() {
        return hosts;
    }

    /**
     * 设置 ES 连接地址列表
     * <p>支持多个地址，格式为：host1:9200,host2:9200</p>
     *
     * @param hosts ES 连接地址列表
     */
    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    /**
     * 获取索引名称
     * <p>支持日期格式：app-logs-%{yyyy.MM.dd}</p>
     *
     * @return 索引名称
     */
    public String getIndex() {
        return index;
    }

    /**
     * 设置索引名称
     * <p>支持日期格式：app-logs-%{yyyy.MM.dd}</p>
     *
     * @param index 索引名称
     */
    public void setIndex(String index) {
        this.index = index;
    }

    /**
     * 获取应用名称
     * <p>自动获取 spring.application.name</p>
     *
     * @return 应用名称
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * 设置应用名称
     * <p>自动获取 spring.application.name</p>
     *
     * @param applicationName 应用名称
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * 获取环境标识
     * <p>自动获取 spring.profiles.active</p>
     *
     * @return 环境标识
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * 设置环境标识
     * <p>自动获取 spring.profiles.active</p>
     *
     * @param environment 环境标识
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * 获取最低日志级别
     * <p>可选值：TRACE, DEBUG, INFO, WARN, ERROR</p>
     *
     * @return 最低日志级别
     */
    public String getMinLevel() {
        return minLevel;
    }

    /**
     * 设置最低日志级别
     * <p>可选值：TRACE, DEBUG, INFO, WARN, ERROR</p>
     *
     * @param minLevel 最低日志级别
     */
    public void setMinLevel(String minLevel) {
        this.minLevel = minLevel;
    }

    /**
     * 获取 ES 连接超时时间（毫秒）
     *
     * @return 连接超时时间（毫秒）
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置 ES 连接超时时间（毫秒）
     *
     * @param connectTimeout 连接超时时间（毫秒）
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * 获取 ES 读写超时时间（毫秒）
     *
     * @return 读写超时时间（毫秒）
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * 设置 ES 读写超时时间（毫秒）
     *
     * @param socketTimeout 读写超时时间（毫秒）
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * 获取 ES 认证用户名
     *
     * @return ES 认证用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置 ES 认证用户名
     *
     * @param username ES 认证用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取 ES 认证密码
     *
     * @return ES 认证密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置 ES 认证密码
     *
     * @param password ES 认证密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取是否启用异步发送
     *
     * @return 如果启用异步发送返回 true，否则返回 false
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * 设置是否启用异步发送
     *
     * @param async 是否启用异步发送
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * 获取异步队列大小
     *
     * @return 异步队列大小
     */
    public int getQueueSize() {
        return queueSize;
    }

    /**
     * 设置异步队列大小
     *
     * @param queueSize 异步队列大小
     */
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    /**
     * 获取是否启用批量发送
     *
     * @return 如果启用批量发送返回 true，否则返回 false
     */
    public boolean isBulkEnabled() {
        return bulkEnabled;
    }

    /**
     * 设置是否启用批量发送
     *
     * @param bulkEnabled 是否启用批量发送
     */
    public void setBulkEnabled(boolean bulkEnabled) {
        this.bulkEnabled = bulkEnabled;
    }

    /**
     * 获取批量大小
     *
     * @return 批量大小
     */
    public int getBulkSize() {
        return bulkSize;
    }

    /**
     * 设置批量大小
     *
     * @param bulkSize 批量大小
     */
    public void setBulkSize(int bulkSize) {
        this.bulkSize = bulkSize;
    }

    /**
     * 获取批量发送间隔（毫秒）
     *
     * @return 批量发送间隔（毫秒）
     */
    public int getBulkInterval() {
        return bulkInterval;
    }

    /**
     * 设置批量发送间隔（毫秒）
     *
     * @param bulkInterval 批量发送间隔（毫秒）
     */
    public void setBulkInterval(int bulkInterval) {
        this.bulkInterval = bulkInterval;
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 设置最大重试次数
     *
     * @param maxRetries 最大重试次数
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * 获取自定义字段映射
     *
     * @return 自定义字段映射
     */
    public Map<String, String> getCustomFields() {
        return customFields;
    }

    /**
     * 设置自定义字段映射
     *
     * @param customFields 自定义字段映射
     */
    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields;
    }

    /**
     * 获取要排除的 logger 名称数组
     *
     * @return 要排除的 logger 名称数组
     */
    public String[] getExcludeLoggers() {
        return excludeLoggers;
    }

    /**
     * 设置要排除的 logger 名称数组
     *
     * @param excludeLoggers 要排除的 logger 名称数组
     */
    public void setExcludeLoggers(String[] excludeLoggers) {
        this.excludeLoggers = excludeLoggers;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public int getMaxConnTotal() {
        return maxConnTotal;
    }

    public void setMaxConnTotal(int maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
    }

    public int getMaxConnPerRoute() {
        return maxConnPerRoute;
    }

    public void setMaxConnPerRoute(int maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
    }

    public boolean isRefreshAfterWrite() {
        return refreshAfterWrite;
    }

    public void setRefreshAfterWrite(boolean refreshAfterWrite) {
        this.refreshAfterWrite = refreshAfterWrite;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public int getMaxRetryTimeout() {
        return maxRetryTimeout;
    }

    public void setMaxRetryTimeout(int maxRetryTimeout) {
        this.maxRetryTimeout = maxRetryTimeout;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}