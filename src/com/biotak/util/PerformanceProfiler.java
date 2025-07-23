package com.biotak.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced performance profiler for detailed monitoring
 */
public final class PerformanceProfiler {
    
    // Performance counters
    private static final AtomicLong methodCallCount = new AtomicLong(0);
    private static final AtomicLong totalExecutionTime = new AtomicLong(0);
    private static final AtomicLong cacheOperations = new AtomicLong(0);
    private static final AtomicLong memoryAllocations = new AtomicLong(0);
    
    // Method-specific profiling
    private static final ConcurrentHashMap<String, MethodProfile> methodProfiles = 
        new ConcurrentHashMap<>();
    
    // System monitoring
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    
    // Profiling state
    private static volatile boolean profilingEnabled = false;
    private static volatile long profilingStartTime = 0;
    
    private PerformanceProfiler() {}
    
    /**
     * Method profile data
     */
    public static class MethodProfile {
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTime = new AtomicLong(0);
        
        public void recordCall(long executionTime) {
            callCount.incrementAndGet();
            totalTime.addAndGet(executionTime);
            
            // Update min/max times
            long currentMin = minTime.get();
            while (executionTime < currentMin && !minTime.compareAndSet(currentMin, executionTime)) {
                currentMin = minTime.get();
            }
            
            long currentMax = maxTime.get();
            while (executionTime > currentMax && !maxTime.compareAndSet(currentMax, executionTime)) {
                currentMax = maxTime.get();
            }
        }
        
        public long getCallCount() { return callCount.get(); }
        public long getTotalTime() { return totalTime.get(); }
        public long getMinTime() { return minTime.get() == Long.MAX_VALUE ? 0 : minTime.get(); }
        public long getMaxTime() { return maxTime.get(); }
        public double getAverageTime() { 
            long calls = callCount.get();
            return calls > 0 ? (double) totalTime.get() / calls : 0.0;
        }
    }
    
    /**
     * Start profiling
     */
    public static void startProfiling() {
        profilingEnabled = true;
        profilingStartTime = System.currentTimeMillis();
        Logger.info("Performance profiling started");
    }
    
    /**
     * Stop profiling
     */
    public static void stopProfiling() {
        profilingEnabled = false;
        Logger.info("Performance profiling stopped");
    }
    
    /**
     * Check if profiling is enabled
     */
    public static boolean isProfilingEnabled() {
        return profilingEnabled;
    }
    
    /**
     * Profile a method execution
     */
    public static <T> T profileMethod(String methodName, java.util.function.Supplier<T> method) {
        if (!profilingEnabled) {
            return method.get();
        }
        
        long startTime = System.nanoTime();
        try {
            T result = method.get();
            return result;
        } finally {
            long executionTime = System.nanoTime() - startTime;
            recordMethodCall(methodName, executionTime);
        }
    }
    
    /**
     * Profile a void method execution
     */
    public static void profileMethod(String methodName, Runnable method) {
        if (!profilingEnabled) {
            method.run();
            return;
        }
        
        long startTime = System.nanoTime();
        try {
            method.run();
        } finally {
            long executionTime = System.nanoTime() - startTime;
            recordMethodCall(methodName, executionTime);
        }
    }
    
    /**
     * Record a method call
     */
    private static void recordMethodCall(String methodName, long executionTime) {
        methodCallCount.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        
        methodProfiles.computeIfAbsent(methodName, k -> new MethodProfile())
                     .recordCall(executionTime);
    }
    
    /**
     * Record cache operation
     */
    public static void recordCacheOperation() {
        cacheOperations.incrementAndGet();
    }
    
    /**
     * Record memory allocation
     */
    public static void recordMemoryAllocation(long bytes) {
        memoryAllocations.addAndGet(bytes);
    }
    
    /**
     * Get comprehensive performance report
     */
    public static String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        
        // Header
        report.append("\n=== BIOTAK PERFORMANCE REPORT ===\n");
        
        // Profiling duration
        if (profilingStartTime > 0) {
            long duration = System.currentTimeMillis() - profilingStartTime;
            report.append(String.format("Profiling Duration: %d ms\n", duration));
        }
        
        // Overall statistics
        report.append("\n--- Overall Statistics ---\n");
        report.append(String.format("Total Method Calls: %d\n", methodCallCount.get()));
        report.append(String.format("Total Execution Time: %.2f ms\n", totalExecutionTime.get() / 1_000_000.0));
        report.append(String.format("Cache Operations: %d\n", cacheOperations.get()));
        report.append(String.format("Memory Allocations: %d bytes\n", memoryAllocations.get()));
        
        // Method-specific statistics
        report.append("\n--- Method Performance ---\n");
        report.append(String.format("%-30s %8s %12s %12s %12s %12s\n", 
                     "Method", "Calls", "Total(ms)", "Avg(ms)", "Min(ms)", "Max(ms)"));
        report.append("--------------------------------------------------------------------------------\n");
        
        methodProfiles.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().getTotalTime(), e1.getValue().getTotalTime()))
            .limit(20) // Top 20 methods
            .forEach(entry -> {
                String method = entry.getKey();
                MethodProfile profile = entry.getValue();
                
                report.append(String.format("%-30s %8d %12.2f %12.2f %12.2f %12.2f\n",
                    method.length() > 30 ? method.substring(0, 27) + "..." : method,
                    profile.getCallCount(),
                    profile.getTotalTime() / 1_000_000.0,
                    profile.getAverageTime() / 1_000_000.0,
                    profile.getMinTime() / 1_000_000.0,
                    profile.getMaxTime() / 1_000_000.0
                ));
            });
        
        // Memory statistics
        report.append("\n--- Memory Statistics ---\n");
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        report.append(String.format("Heap Memory: %d MB / %d MB (%.1f%%)\n",
                     heapUsed / (1024 * 1024),
                     heapMax / (1024 * 1024),
                     (double) heapUsed / heapMax * 100.0));
        report.append(String.format("Non-Heap Memory: %d MB\n", nonHeapUsed / (1024 * 1024)));
        
        // Thread statistics
        report.append("\n--- Thread Statistics ---\n");
        report.append(String.format("Active Threads: %d\n", threadBean.getThreadCount()));
        report.append(String.format("Peak Threads: %d\n", threadBean.getPeakThreadCount()));
        report.append(String.format("Total Started Threads: %d\n", threadBean.getTotalStartedThreadCount()));
        
        // Cache statistics
        report.append("\n--- Cache Statistics ---\n");
        report.append(ComputationCache.getCacheStats() + "\n");
        report.append(ConcurrencyOptimizer.getPerformanceStats() + "\n");
        report.append(AdvancedMemoryManager.getMemoryStats() + "\n");
        report.append(UIOptimizer.getUIStats() + "\n");
        report.append(IOOptimizer.getIOStats() + "\n");
        
        report.append("\n=== END REPORT ===\n");
        
        return report.toString();
    }
    
    /**
     * Get quick performance summary
     */
    public static String getQuickSummary() {
        long calls = methodCallCount.get();
        double totalTimeMs = totalExecutionTime.get() / 1_000_000.0;
        double avgTimeMs = calls > 0 ? totalTimeMs / calls : 0.0;
        
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double heapPercent = (double) heapUsed / heapMax * 100.0;
        
        return String.format(
            "Calls: %d, Avg: %.3fms, Heap: %.1f%%, Cache Ops: %d",
            calls, avgTimeMs, heapPercent, cacheOperations.get()
        );
    }
    
    /**
     * Reset all profiling data
     */
    public static void reset() {
        methodCallCount.set(0);
        totalExecutionTime.set(0);
        cacheOperations.set(0);
        memoryAllocations.set(0);
        methodProfiles.clear();
        profilingStartTime = System.currentTimeMillis();
    }
    
    /**
     * Auto-profiling wrapper for critical methods
     */
    public static class AutoProfiler implements AutoCloseable {
        private final String methodName;
        private final long startTime;
        
        public AutoProfiler(String methodName) {
            this.methodName = methodName;
            this.startTime = profilingEnabled ? System.nanoTime() : 0;
        }
        
        @Override
        public void close() {
            if (profilingEnabled && startTime > 0) {
                long executionTime = System.nanoTime() - startTime;
                recordMethodCall(methodName, executionTime);
            }
        }
    }
}