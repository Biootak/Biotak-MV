package com.biotak.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Performance monitoring utility to track method execution times and memory usage
 */
public final class PerformanceMonitor {
    
    private static final Map<String, Long> methodStartTimes = new ConcurrentHashMap<>();
    private static final Map<String, Long> methodTotalTimes = new ConcurrentHashMap<>();
    private static final Map<String, Integer> methodCallCounts = new ConcurrentHashMap<>();
    private static final boolean MONITORING_ENABLED = false; // Disable in production
    
    // محدودیت اندازه برای جلوگیری از نشت حافظه
    private static final int MAX_METHODS_TRACKED = 100;
    
    // Cleanup scheduler برای پاکسازی خودکار
    private static final java.util.concurrent.ScheduledExecutorService cleanupExecutor = 
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PerformanceCleanup");
            t.setDaemon(true);
            return t;
        });
    
    static {
        // پاکسازی خودکار هر 5 دقیقه
        cleanupExecutor.scheduleAtFixedRate(
            PerformanceMonitor::clear, 300, 300, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    private PerformanceMonitor() {}
    
    /**
     * Start timing a method
     */
    public static void startTiming(String methodName) {
        if (!MONITORING_ENABLED) return;
        
        String key = Thread.currentThread().getName() + ":" + methodName;
        methodStartTimes.put(key, System.nanoTime());
    }
    
    /**
     * End timing a method and record the duration
     */
    public static void endTiming(String methodName) {
        if (!MONITORING_ENABLED) return;
        
        String key = Thread.currentThread().getName() + ":" + methodName;
        Long startTime = methodStartTimes.remove(key);
        
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            
            // محدود کردن تعداد methods tracked
            if (methodCallCounts.size() < MAX_METHODS_TRACKED) {
                methodTotalTimes.merge(methodName, duration, Long::sum);
                methodCallCounts.merge(methodName, 1, Integer::sum);
            }
            
            // Log slow methods (> 10ms)
            if (duration > 10_000_000) { // 10ms in nanoseconds
                Logger.warn("Slow method detected: " + methodName + " took " + 
                           (duration / 1_000_000) + "ms");
            }
        }
    }
    
    /**
     * Get performance statistics
     */
    public static void logPerformanceStats() {
        if (!MONITORING_ENABLED || methodCallCounts.isEmpty()) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Performance Statistics ===\n");
        
        for (Map.Entry<String, Integer> entry : methodCallCounts.entrySet()) {
            String method = entry.getKey();
            int calls = entry.getValue();
            long totalTime = methodTotalTimes.getOrDefault(method, 0L);
            long avgTime = calls > 0 ? totalTime / calls : 0;
            
            sb.append(String.format("%-30s: %6d calls, %8.2fms total, %8.2fms avg\n",
                    method, calls, totalTime / 1_000_000.0, avgTime / 1_000_000.0));
        }
        
        Logger.info(sb.toString());
    }
    
    /**
     * Clear all performance data
     */
    public static void clear() {
        methodStartTimes.clear();
        methodTotalTimes.clear();
        methodCallCounts.clear();
    }
    
    /**
     * Get current memory usage in MB
     */
    public static double getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024.0 * 1024.0);
    }
    
    /**
     * Log memory usage if it's high
     */
    public static void checkMemoryUsage() {
        double memoryMB = getMemoryUsageMB();
        if (memoryMB > 100) { // Log if using more than 100MB
            Logger.warn("High memory usage detected: " + String.format("%.2f MB", memoryMB));
        }
    }
}