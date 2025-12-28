package com.lz.logging.support;

import java.lang.management.ManagementFactory;

public class Utils {
    private Utils() {
        // 工具类，私有构造器
    }

    public static String getProcessId() {
        try {
            return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        } catch (Exception e) {
            return String.valueOf(Thread.currentThread().getId());
        }
    }

    public static String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
