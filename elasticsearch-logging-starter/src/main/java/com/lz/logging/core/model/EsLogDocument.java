package com.lz.logging.core.model;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;
import org.slf4j.MDC;

public class EsLogDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志时间戳（ISO 8601 格式）
     */
    @JSONField(name = "@timestamp")
    private String timestamp;

    /**
     * 日志级别
     */
    private String level;

    /**
     * 线程名
     */
    private String thread;

    /**
     * Logger 名称
     */
    private String logger;

    /**
     * 日志消息
     */
    private String message;

    /**
     * 跟踪ID（用于分布式追踪）
     */
    private String traceId;

    /**
     * 跨度ID（用于分布式追踪）
     */
    private String spanId;

    /**
     * 应用名称
     */
    private String application;

    /**
     * 环境标识
     */
    private String environment;

    /**
     * 异常信息
     */
    private String exception;

    /**
     * 异常堆栈
     */
    private String stackTrace;

    /**
     * MDC 上下文信息
     */
    private java.util.Map<String, String> mdc;

    /**
     * 日志参数
     */
    private String[] arguments;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 用户标识
     */
    private String userId;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 自定义字段
     */
    private java.util.Map<String, Object> extra;

    // Getters and Setters

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public java.util.Map<String, String> getMdc() {
        return mdc;
    }

    public void setMdc(java.util.Map<String, String> mdc) {
        this.mdc = mdc;
    }

    public String[] getArguments() {
        return arguments;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public java.util.Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(java.util.Map<String, Object> extra) {
        this.extra = extra;
    }
}
