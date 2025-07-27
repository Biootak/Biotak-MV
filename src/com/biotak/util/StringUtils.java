package com.biotak.util;

/**
 * Optimized string utilities to reduce string allocation overhead
 */
public final class StringUtils {
    
    // Thread-local DecimalFormat instances for better performance
    private static final ThreadLocal<java.text.DecimalFormat> FORMAT_1F_TL = 
        ThreadLocal.withInitial(() -> new java.text.DecimalFormat("#.#"));
    private static final ThreadLocal<java.text.DecimalFormat> FORMAT_2F_TL = 
        ThreadLocal.withInitial(() -> new java.text.DecimalFormat("#.##"));
    private static final ThreadLocal<java.text.DecimalFormat> FORMAT_5F_TL = 
        ThreadLocal.withInitial(() -> new java.text.DecimalFormat("#.#####"));
    
    // Thread-local StringBuilder for thread-safe operations - optimized size
    private static final ThreadLocal<StringBuilder> THREAD_LOCAL_SB = 
        ThreadLocal.withInitial(() -> new StringBuilder(32)); // Reduced initial capacity
    
    private StringUtils() {}
    
    /**
     * Format a double to 1 decimal place efficiently
     */
    public static String format1f(double value) {
        return FORMAT_1F_TL.get().format(value);
    }
    
    /**
     * Format a double to 2 decimal places efficiently
     */
    public static String format2f(double value) {
        return FORMAT_2F_TL.get().format(value);
    }
    
    /**
     * Format a double to 5 decimal places efficiently
     */
    public static String format5f(double value) {
        return FORMAT_5F_TL.get().format(value);
    }
    
    /**
     * Get a thread-local StringBuilder for efficient string building
     */
    public static StringBuilder getThreadLocalStringBuilder() {
        StringBuilder sb = THREAD_LOCAL_SB.get();
        sb.setLength(0); // Clear existing content
        return sb;
    }
    
    /**
     * Build a string efficiently using thread-local StringBuilder
     */
    public static String buildString(String... parts) {
        StringBuilder sb = getThreadLocalStringBuilder();
        for (String part : parts) {
            if (part != null) {
                sb.append(part);
            }
        }
        return sb.toString();
    }
    
    /**
     * Check if a string is null or empty efficiently
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * Check if a string is not null and not empty efficiently
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }
}