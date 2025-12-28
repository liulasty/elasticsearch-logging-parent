package com.lz.logging.support;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程本地 MDC（Mapped Diagnostic Context）工具类
 *
 * 该类提供线程安全的 MDC 操作功能，用于在日志记录中添加上下文信息，
 * 如请求ID、用户ID等，便于日志追踪和分析。
 *
 * MDC（Mapped Diagnostic Context）是 logback 提供的一种诊断上下文映射机制，
 * 可以在多线程环境中为每个线程维护独立的上下文信息。
 *
 * @author lingma
 * @version 1.0
 * @since 2023
 */
public class ThreadLocalMDC {

    /**
     * 线程本地存储的 MDC 映射
     */
    private static final ThreadLocal<Map<String, String>> MDC_HOLDER =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * 将键值对放入当前线程的 MDC 映射中
     *
     * @param key MDC 键，不能为 null
     * @param value MDC 值，可以为 null
     * @throws IllegalArgumentException 如果 key 为 null
     */
    public static void put(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        MDC_HOLDER.get().put(key, value);
    }

    /**
     * 从当前线程的 MDC 映射中获取指定键的值
     *
     * @param key 要获取的键
     * @return 对应的值，如果键不存在则返回 null
     */
    public static String get(String key) {
        return MDC_HOLDER.get().get(key);
    }

    /**
     * 从当前线程的 MDC 映射中移除指定键的值
     *
     * @param key 要移除的键
     * @return 被移除的值，如果键不存在则返回 null
     */
    public static String remove(String key) {
        return MDC_HOLDER.get().remove(key);
    }

    /**
     * 清空当前线程的 MDC 映射
     */
    public static void clear() {
        MDC_HOLDER.get().clear();
    }

    /**
     * 获取当前线程 MDC 映射的副本
     *
     * @return 当前线程 MDC 映射的不可变副本
     */
    public static Map<String, String> getCopyOfContextMap() {
        return new HashMap<>(MDC_HOLDER.get());
    }

    /**
     * 设置当前线程的 MDC 映射
     *
     * @param contextMap 新的 MDC 映射，如果为 null 则清空当前映射
     */
    public static void setContextMap(Map<String, String> contextMap) {
        if (contextMap == null) {
            MDC_HOLDER.remove();
        } else {
            MDC_HOLDER.set(new HashMap<>(contextMap));
        }
    }

    /**
     * 检查当前线程的 MDC 映射中是否包含指定键
     *
     * @param key 要检查的键
     * @return 如果包含该键返回 true，否则返回 false
     */
    public static boolean containsKey(String key) {
        return MDC_HOLDER.get().containsKey(key);
    }

    /**
     * 获取当前线程 MDC 映射中键值对的数量
     *
     * @return MDC 映射中的键值对数量
     */
    public static int size() {
        return MDC_HOLDER.get().size();
    }

    /**
     * 检查当前线程的 MDC 映射是否为空
     *
     * @return 如果映射为空返回 true，否则返回 false
     */
    public static boolean isEmpty() {
        return MDC_HOLDER.get().isEmpty();
    }
}
