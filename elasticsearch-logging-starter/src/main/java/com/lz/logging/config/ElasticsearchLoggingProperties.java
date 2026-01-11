package com.lz.logging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
     * 最大连接数
     */
    private int maxConnTotal = 30;

    /**
     * 每个路由的最大连接数
     */
    private int maxConnPerRoute = 10;

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
     * 时区 ID，例如 "Asia/Shanghai"，默认使用系统默认时区
     */
    private String zoneId = java.time.ZoneId.systemDefault().getId();

    /**
     * 并发请求数，0 表示仅允许一个请求执行（同步），1 表示允许一个并发请求
     */
    private int concurrentRequests = 1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(String minLevel) {
        this.minLevel = minLevel;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public boolean isBulkEnabled() {
        return bulkEnabled;
    }

    public void setBulkEnabled(boolean bulkEnabled) {
        this.bulkEnabled = bulkEnabled;
    }

    public int getBulkSize() {
        return bulkSize;
    }

    public void setBulkSize(int bulkSize) {
        this.bulkSize = bulkSize;
    }

    public int getBulkInterval() {
        return bulkInterval;
    }

    public void setBulkInterval(int bulkInterval) {
        this.bulkInterval = bulkInterval;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public int getConcurrentRequests() {
        return concurrentRequests;
    }

    public void setConcurrentRequests(int concurrentRequests) {
        this.concurrentRequests = concurrentRequests;
    }
}
