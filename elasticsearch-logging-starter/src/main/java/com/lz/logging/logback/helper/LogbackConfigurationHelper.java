package com.lz.logging.logback.helper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;

/**
 * Logback 配置助手接口
 */
public interface LogbackConfigurationHelper {

    /**
     * 获取 LoggerContext
     *
     * @return LoggerContext 实例
     */
    LoggerContext getLoggerContext();

    /**
     * 将 Appender 添加到指定的 Logger
     *
     * @param logger    目标 Logger
     * @param appender  要添加的 Appender
     * @param <T>       Appender 类型
     * @return 添加的 Appender 实例
     */
    <T extends Appender> T addAppenderToLogger(Logger logger, T appender);

    /**
     * 将 Appender 添加到根 Logger
     *
     * @param appender  要添加的 Appender
     * @param <T>       Appender 类型
     * @return 添加的 Appender 实例
     */
    <T extends Appender> T addAppenderToRootLogger(T appender);

    /**
     * 检查 Logger 是否已存在指定名称的 Appender
     *
     * @param logger      目标 Logger
     * @param appenderName Appender 名称
     * @return 如果存在返回 true，否则返回 false
     */
    boolean hasAppender(Logger logger, String appenderName);

    /**
     * 从 Logger 移除指定名称的 Appender
     *
     * @param logger      目标 Logger
     * @param appenderName Appender 名称
     */
    void removeAppenderByName(Logger logger, String appenderName);

    /**
     * 启动 Appender（如果尚未启动）
     *
     * @param appender 要启动的 Appender
     */
    void startAppender(Appender appender);

    /**
     * 停止 Appender（如果正在运行）
     *
     * @param appender 要停止的 Appender
     */
    void stopAppender(Appender appender);
}
