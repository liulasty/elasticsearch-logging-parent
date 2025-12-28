package com.lz.logging.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import org.slf4j.LoggerFactory;

/**
 * Logback 配置助手实现类
 */
public class LogbackConfigurationHelperImpl implements LogbackConfigurationHelper {

    /**
     * 获取 LoggerContext
     *
     * @return LoggerContext 实例
     */
    @Override
    public LoggerContext getLoggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    /**
     * 将 Appender 添加到指定的 Logger
     *
     * @param logger    目标 Logger
     * @param appender  要添加的 Appender
     * @param <T>       Appender 类型
     * @return 添加的 Appender 实例
     */
    @Override
    public <T extends Appender> T addAppenderToLogger(Logger logger, T appender) {
        // 设置上下文
        appender.setContext(logger.getLoggerContext());

        // 启动 Appender（如果尚未启动）
        startAppender(appender);

        // 添加到 Logger
        logger.addAppender(appender);

        return appender;
    }

    /**
     * 将 Appender 添加到根 Logger
     *
     * @param appender  要添加的 Appender
     * @param <T>       Appender 类型
     * @return 添加的 Appender 实例
     */
    @Override
    public <T extends Appender> T addAppenderToRootLogger(T appender) {
        LoggerContext context = getLoggerContext();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        return addAppenderToLogger(rootLogger, appender);
    }

    /**
     * 检查 Logger 是否已存在指定名称的 Appender
     *
     * @param logger      目标 Logger
     * @param appenderName Appender 名称
     * @return 如果存在返回 true，否则返回 false
     */
    @Override
    public boolean hasAppender(Logger logger, String appenderName) {
        return logger.getAppender(appenderName) != null;
    }

    /**
     * 从 Logger 移除指定名称的 Appender
     *
     * @param logger      目标 Logger
     * @param appenderName Appender 名称
     */
    @Override
    public void removeAppenderByName(Logger logger, String appenderName) {
        Appender<?> appender = logger.getAppender(appenderName);
        if (appender != null) {
            logger.detachAppender(appenderName);
            stopAppender(appender);
        }
    }

    /**
     * 启动 Appender（如果尚未启动）
     *
     * @param appender 要启动的 Appender
     */
    @Override
    public void startAppender(Appender appender) {
        if (!appender.isStarted()) {
            appender.start();
        }
    }

    /**
     * 停止 Appender（如果正在运行）
     *
     * @param appender 要停止的 Appender
     */
    @Override
    public void stopAppender(Appender appender) {
        if (appender.isStarted()) {
            appender.stop();
        }
    }
}
