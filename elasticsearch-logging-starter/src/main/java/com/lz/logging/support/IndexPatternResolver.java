package com.lz.logging.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 索引模式解析器
 *
 * 该类用于解析包含日期格式占位符的索引模式字符串，将其转换为实际的索引名称。
 * 支持多种日期格式，如 %{yyyy-MM-dd}, %{yyyy.MM.dd} 等，便于按日期创建索引。
 *
 * <p>使用示例：</p>
 * <pre>
 * IndexPatternResolver resolver = new IndexPatternResolver();
 *
 * // 解析日期格式的索引名称
 * String index1 = resolver.resolve("app-logs-%{yyyy-MM-dd}"); // 结果: app-logs-2023-12-27
 * String index2 = resolver.resolve("app-logs-%{yyyy.MM.dd}"); // 结果: app-logs-2023.12.27
 * String index3 = resolver.resolve("app-logs-%{yyyy_MM_dd}"); // 结果: app-logs-2023_12_27
 * String index4 = resolver.resolve("app-logs-%{date}");       // 结果: app-logs-2023-12-27
 * String index5 = resolver.resolve("app-logs-%{month}");      // 结果: app-logs-2023-12
 * String index6 = resolver.resolve("app-logs-%{day}");        // 结果: app-logs-27
 * String index7 = resolver.resolve("app-logs-%{hour}");       // 结果: app-logs-14 (假设当前小时是14)
 *
 * // 不包含占位符的模式将直接返回
 * String index8 = resolver.resolve("app-logs");              // 结果: app-logs
 *
 * // 空字符串将返回默认值
 * String index9 = resolver.resolve("");                      // 结果: app-logs
 * </pre>
 *
 * <p>支持的占位符格式：</p>
 * <ul>
 *     <li>%{yyyy-MM-dd} - 日期格式，如 2023-12-27</li>
 *     <li>%{yyyy.MM.dd} - 日期格式，如 2023.12.27</li>
 *     <li>%{yyyy_MM_dd} - 日期格式，如 2023_12_27</li>
 *     <li>%{date} - ISO标准日期格式，如 2023-12-27</li>
 *     <li>%{month} - 月份格式，如 2023-12</li>
 *     <li>%{day} - 日期中的天数，如 27</li>
 *     <li>%{hour} - 小时格式，如 14</li>
 * </ul>
 *
 * @author lingma
 * @version 1.0
 * @since 2023
 */
public class IndexPatternResolver {

    /**
     * 默认日期模式占位符
     */
    private static final String DATE_PATTERN_PLACEHOLDER = "%{yyyy-MM-dd}";
    
    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 用于匹配占位符的正则表达式模式
     * 匹配格式为 %{...} 的占位符
     */
    private static final Pattern PATTERN_PATTERN = Pattern.compile("%\\{([^}]+)\\}");

    /**
     * 解析索引模式字符串，将其中的日期占位符替换为实际的日期值
     *
     * @param pattern 包含占位符的索引模式字符串，例如 "app-logs-%{yyyy-MM-dd}"
     * @return 解析后的实际索引名称，如果输入为null或空字符串则返回默认值 "app-logs"
     */
    public String resolve(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return "app-logs";
        }

        String result = pattern;
        Matcher matcher = PATTERN_PATTERN.matcher(pattern);

        while (matcher.find()) {
            String placeholder = matcher.group(0); // 完整的 %{xxx}
            String key = matcher.group(1);        // xxx

            String replacement = resolvePlaceholder(key);
            if (replacement != null) {
                result = result.replace(placeholder, replacement);
            }
        }

        return result;
    }

    /**
     * 解析特定的占位符并返回对应的日期字符串
     *
     * @param key 占位符的键名（不包含 %{ 和 }），例如 "yyyy-MM-dd"
     * @return 对应的日期字符串，如果键名不支持则返回null
     */
    private String resolvePlaceholder(String key) {
        switch (key.toLowerCase()) {
            case "yyyy.mm.dd":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            case "yyyy-mm-dd":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "yyyy_mm_dd":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
            case "date":
                return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "month":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "day":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd"));
            case "hour":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH"));
            default:
                return null;
        }
    }
}