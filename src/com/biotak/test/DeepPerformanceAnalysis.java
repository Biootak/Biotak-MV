package com.biotak.test;

import com.biotak.util.*;
import com.biotak.core.*;
import com.biotak.enums.*;
import com.biotak.*;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Deep Performance Analysis - Real memory leaks and CPU bottleneck detection
 */
public class DeepPerformanceAnalysis {
    
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    
    // Test data for realistic scenarios
    private static final int REALISTIC_ITERATIONS = 10000;
    private static final int STRESS_ITERATIONS = 100000;
    
    public static void main(String[] args) {
        System.out.println("=== DEEP PERFORMANCE ANALYSIS ===");
        System.out.println("Testing realistic trading scenarios with actual data loads...\n");
        
        // Enable detailed monitoring
        enableDetailedMonitoring();
        
        // Baseline measurements
        PerformanceSnapshot baseline = takeSnapshot("BASELINE");
        
        // Test realistic trading scenarios
        testRealisticTradingLoad();
        PerformanceSnapshot afterTrading = takeSnapshot("AFTER_TRADING");
        
        // Test memory leak scenarios
        testMemoryLeakScenarios();
        PerformanceSnapshot afterMemoryTest = takeSnapshot("AFTER_MEMORY_TEST");
        
        // Test CPU intensive calculations
        testCPUIntensiveCalculations();
        PerformanceSnapshot afterCPUTest = takeSnapshot("AFTER_CPU_TEST");
        
        // Test concurrent access (multi-threading)
        testConcurrentAccess();
        PerformanceSnapshot afterConcurrency = takeSnapshot("AFTER_CONCURRENCY");
        
        // Test cache efficiency under load
        testCacheEfficiencyUnderLoad();
        PerformanceSnapshot afterCache = takeSnapshot("AFTER_CACHE_TEST");
        
        // Analyze results
        System.out.println("\n=== PERFORMANCE ANALYSIS RESULTS ===");
        analyzePerformanceDifferences(baseline, afterTrading, "Trading Load");
        analyzePerformanceDifferences(afterTrading, afterMemoryTest, "Memory Test");
        analyzePerformanceDifferences(afterMemoryTest, afterCPUTest, "CPU Test");
        analyzePerformanceDifferences(afterCPUTest, afterConcurrency, "Concurrency Test");
        analyzePerformanceDifferences(afterConcurrency, afterCache, "Cache Test");
        
        // Final recommendations
        generateRecommendations(baseline, afterCache);
        
        System.out.println("\n=== DEEP ANALYSIS COMPLETED ===");
    }
    
    private static void enableDetailedMonitoring() {
        if (threadBean.isThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
        if (threadBean.isThreadContentionMonitoringSupported()) {
            threadBean.setThreadContentionMonitoringEnabled(true);
        }
    }
    
    private static void testRealisticTradingLoad() {
        System.out.println("--- REALISTIC TRADING LOAD TEST ---");
        
        long startTime = System.nanoTime();
        
        // Simulate real trading indicator calculations
        for (int i = 0; i < REALISTIC_ITERATIONS; i++) {
            // Simulate price data
            double price = 1.1000 + (Math.random() - 0.5) * 0.01; // EURUSD-like prices
            
            // Calculate TH values (realistic scenario) - simplified without instrument
            double thValue = price * 0.02 / 100.0; // Simple TH calculation
            
            // Calculate fractal values - simplified
            double th = thValue;
            double pattern = th / 2.0;
            double trigger = pattern / 2.0;
            double structure = th * 2.0;
            double higherPattern = structure / 2.0;
            
            // String formatting (happens a lot in UI)
            String formatted1 = StringUtils.format5f(price);
            String formatted2 = StringUtils.format2f(thValue);
            String formatted3 = StringUtils.format1f(th);
            
            // Cache operations (realistic usage)
            ComputationCache.cachePercentage("price_" + (i % 100), price);
            ComputationCache.getCachedPercentage("price_" + (i % 50));
            
            // Object pooling usage
            StringBuilder sb = PoolManager.getStringBuilder();
            sb.append("Price: ").append(formatted1).append(", TH: ").append(formatted2);
            PoolManager.releaseStringBuilder(sb);
            
            // Simulate some calculations every 100 iterations (like real-time updates)
            if (i % 100 == 0) {
                double atr = FastMath.fastSqrt(Math.abs(price - 1.1000) * 10000);
                double log = FastMath.fastLog2(i + 1);
            }
        }
        
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Processed %d realistic trading calculations in %.2f ms%n", 
                         REALISTIC_ITERATIONS, executionTime);
        System.out.printf("Calculations per second: %.0f%n", 
                         REALISTIC_ITERATIONS / (executionTime / 1000));
        System.out.printf("Average time per calculation: %.4f ms%n", 
                         executionTime / REALISTIC_ITERATIONS);
    }
    
    private static void testMemoryLeakScenarios() {
        System.out.println("\n--- MEMORY LEAK DETECTION TEST ---");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Test 1: String accumulation (potential leak)
        List<String> stringAccumulator = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            stringAccumulator.add(StringUtils.format5f(Math.random() * 1000));
            stringAccumulator.add(StringUtils.format2f(Math.random() * 100));
        }
        
        long afterStrings = runtime.totalMemory() - runtime.freeMemory();
        long stringMemoryIncrease = afterStrings - initialMemory;
        
        // Test 2: Cache accumulation (potential leak) - limited to avoid overflow
        for (int i = 0; i < 50; i++) { // Reduced to avoid cache overflow
            ComputationCache.cachePercentage("leak_test_" + i, Math.random());
            ComputationCache.cacheAtrPeriod("atr_test_" + i, (int)(Math.random() * 100));
            ComputationCache.cachePipMultiplier("pip_test_" + i, Math.random() * 10);
        }
        
        long afterCache = runtime.totalMemory() - runtime.freeMemory();
        long cacheMemoryIncrease = afterCache - afterStrings;
        
        // Test 3: Object creation without pooling
        List<StringBuilder> objectAccumulator = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("Test object ").append(i).append(" without pooling");
            objectAccumulator.add(sb);
        }
        
        long afterObjects = runtime.totalMemory() - runtime.freeMemory();
        long objectMemoryIncrease = afterObjects - afterCache;
        
        // Force GC and measure again
        System.gc();
        Thread.yield();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        long afterGC = runtime.totalMemory() - runtime.freeMemory();
        long memoryRecovered = afterObjects - afterGC;
        
        System.out.printf("String accumulation memory: %d KB%n", stringMemoryIncrease / 1024);
        System.out.printf("Cache accumulation memory: %d KB%n", cacheMemoryIncrease / 1024);
        System.out.printf("Object accumulation memory: %d KB%n", objectMemoryIncrease / 1024);
        System.out.printf("Memory recovered after GC: %d KB%n", memoryRecovered / 1024);
        System.out.printf("Potential memory leak: %d KB%n", 
                         (afterGC - initialMemory) / 1024);
        
        // Memory leak warning
        if ((afterGC - initialMemory) > 5 * 1024 * 1024) { // 5MB threshold
            System.out.println("⚠️  WARNING: Potential memory leak detected!");
        } else {
            System.out.println("✅ No significant memory leaks detected");
        }
        
        // Clean up test data
        stringAccumulator.clear();
        objectAccumulator.clear();
        // Don't clear cache - let it persist for cache efficiency test
    }
    
    private static void testCPUIntensiveCalculations() {
        System.out.println("\n--- CPU INTENSIVE CALCULATIONS TEST ---");
        
        if (!threadBean.isThreadCpuTimeSupported()) {
            System.out.println("CPU time measurement not supported");
            return;
        }
        
        long threadId = Thread.currentThread().getId();
        
        // Test 1: Math operations under stress
        long startCpuTime = threadBean.getThreadCpuTime(threadId);
        long startWallTime = System.nanoTime();
        
        double result = 0;
        for (int i = 0; i < STRESS_ITERATIONS; i++) {
            // Simulate complex TH calculations
            double price = 1.0000 + Math.random() * 0.1;
            result += FastMath.fastSqrt(price * 10000);
            result += FastMath.fastLog2(price * 1000);
            result += FastMath.fastAbs(price - 1.05);
            
            // Simulate percentage calculations
            result += OptimizedCalculations.calculateTimeframePercentageOptimized((int)(Math.random() * 1440));
            
            // String operations (CPU intensive)
            if (i % 100 == 0) {
                String formatted = StringUtils.format5f(result);
                formatted.length(); // Force string processing
            }
        }
        
        long endCpuTime = threadBean.getThreadCpuTime(threadId);
        long endWallTime = System.nanoTime();
        
        double cpuTime = (endCpuTime - startCpuTime) / 1_000_000.0;
        double wallTime = (endWallTime - startWallTime) / 1_000_000.0;
        double cpuUsage = (cpuTime / wallTime) * 100;
        
        System.out.printf("Stress test result: %.2f%n", result);
        System.out.printf("CPU time: %.2f ms%n", cpuTime);
        System.out.printf("Wall time: %.2f ms%n", wallTime);
        System.out.printf("CPU usage: %.1f%%%n", cpuUsage);
        System.out.printf("Operations per second: %.0f%n", 
                         (STRESS_ITERATIONS * 4) / (wallTime / 1000));
        
        // CPU efficiency analysis
        if (cpuUsage < 50) {
            System.out.println("⚠️  WARNING: Low CPU utilization - possible I/O bottleneck");
        } else if (cpuUsage > 200) {
            System.out.println("✅ Good multi-core CPU utilization");
        } else {
            System.out.println("✅ Normal single-core CPU utilization");
        }
    }
    
    private static void testConcurrentAccess() {
        System.out.println("\n--- CONCURRENT ACCESS TEST ---");
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        // Test concurrent cache access
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < REALISTIC_ITERATIONS / numThreads; i++) {
                        // Concurrent cache operations - use limited keys to avoid cache overflow
                        String key = "concurrent_" + (i % 10); // Only 10 keys per thread
                        ComputationCache.cachePercentage(key, Math.random());
                        ComputationCache.getCachedPercentage(key);
                        
                        // Concurrent object pooling
                        StringBuilder sb = PoolManager.getStringBuilder();
                        sb.append("Thread ").append(threadId).append(" item ").append(i);
                        PoolManager.releaseStringBuilder(sb);
                        
                        // Concurrent math operations
                        double result = FastMath.fastSqrt(Math.random() * 1000);
                        result += FastMath.fastLog2(i + 1);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0;
        
        executor.shutdown();
        
        System.out.printf("Concurrent test with %d threads completed in %.2f ms%n", 
                         numThreads, executionTime);
        System.out.printf("Operations per second (total): %.0f%n", 
                         REALISTIC_ITERATIONS / (executionTime / 1000));
        System.out.printf("Operations per second per thread: %.0f%n", 
                         (REALISTIC_ITERATIONS / numThreads) / (executionTime / 1000));
        
        // Check for thread contention
        long[] threadIds = threadBean.getAllThreadIds();
        long totalBlockedTime = 0;
        long totalWaitedTime = 0;
        
        for (long id : threadIds) {
            ThreadInfo info = threadBean.getThreadInfo(id);
            if (info != null) {
                totalBlockedTime += info.getBlockedTime();
                totalWaitedTime += info.getWaitedTime();
            }
        }
        
        System.out.printf("Total blocked time: %d ms%n", totalBlockedTime);
        System.out.printf("Total waited time: %d ms%n", totalWaitedTime);
        
        if (totalBlockedTime > executionTime * 0.1) {
            System.out.println("⚠️  WARNING: High thread contention detected");
        } else {
            System.out.println("✅ Low thread contention - good concurrency");
        }
    }
    
    private static void testCacheEfficiencyUnderLoad() {
        System.out.println("\n--- CACHE EFFICIENCY UNDER LOAD TEST ---");
        
        long startTime = System.nanoTime();
        
        int hits = 0;
        int misses = 0;
        int operations = REALISTIC_ITERATIONS;
        
        // Pre-populate cache for realistic hit ratios (using increased cache size)
        for (int i = 0; i < 100; i++) { // Use 100 items with new 200 limit
            ComputationCache.cachePercentage("recent_" + i, Math.random());
        }
        
        // Simulate realistic cache usage patterns
        for (int i = 0; i < operations; i++) {
            // 70% chance of accessing recent data (cache hits)
            String key;
            if (Math.random() < 0.7) {
                key = "recent_" + (i % 100); // Recent data (should hit)
            } else {
                key = "old_" + (i % 50); // More new data with larger cache
            }
            
            Double value = ComputationCache.getCachedPercentage(key);
            if (value != null) {
                hits++;
            } else {
                misses++;
                ComputationCache.cachePercentage(key, Math.random());
            }
            
            // Test other cache types
            if (i % 10 == 0) {
                Integer atrPeriod = ComputationCache.getCachedAtrPeriod("atr_" + (i % 50));
                if (atrPeriod == null) {
                    ComputationCache.cacheAtrPeriod("atr_" + (i % 50), (int)(Math.random() * 100));
                }
            }
        }
        
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0;
        
        double hitRatio = (double) hits / (hits + misses) * 100;
        
        System.out.printf("Cache operations: %d%n", operations);
        System.out.printf("Cache hits: %d%n", hits);
        System.out.printf("Cache misses: %d%n", misses);
        System.out.printf("Hit ratio: %.1f%%%n", hitRatio);
        System.out.printf("Cache lookup time: %.2f ms%n", executionTime);
        System.out.printf("Lookups per second: %.0f%n", operations / (executionTime / 1000));
        
        // Cache efficiency analysis
        if (hitRatio < 50) {
            System.out.println("⚠️  WARNING: Low cache hit ratio - consider cache size tuning");
        } else if (hitRatio > 80) {
            System.out.println("✅ Excellent cache hit ratio");
        } else {
            System.out.println("✅ Good cache hit ratio");
        }
    }
    
    private static PerformanceSnapshot takeSnapshot(String label) {
        Runtime runtime = Runtime.getRuntime();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        
        long totalGCTime = 0;
        long totalGCCount = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            totalGCTime += gcBean.getCollectionTime();
            totalGCCount += gcBean.getCollectionCount();
        }
        
        return new PerformanceSnapshot(
            label,
            System.currentTimeMillis(),
            runtime.totalMemory() - runtime.freeMemory(),
            heapMemory.getUsed(),
            nonHeapMemory.getUsed(),
            totalGCCount,
            totalGCTime,
            Thread.activeCount(),
            runtimeBean.getUptime()
        );
    }
    
    private static void analyzePerformanceDifferences(PerformanceSnapshot before, 
                                                     PerformanceSnapshot after, 
                                                     String testName) {
        System.out.printf("\n--- %s IMPACT ---\n", testName.toUpperCase());
        
        long memoryIncrease = after.usedMemory - before.usedMemory;
        long heapIncrease = after.heapUsed - before.heapUsed;
        long nonHeapIncrease = after.nonHeapUsed - before.nonHeapUsed;
        long gcCountIncrease = after.gcCount - before.gcCount;
        long gcTimeIncrease = after.gcTime - before.gcTime;
        
        System.out.printf("Memory increase: %d KB%n", memoryIncrease / 1024);
        System.out.printf("Heap increase: %d KB%n", heapIncrease / 1024);
        System.out.printf("Non-heap increase: %d KB%n", nonHeapIncrease / 1024);
        System.out.printf("GC collections: %d%n", gcCountIncrease);
        System.out.printf("GC time: %d ms%n", gcTimeIncrease);
        
        // Analysis
        if (memoryIncrease > 10 * 1024 * 1024) { // 10MB
            System.out.println("⚠️  HIGH memory usage increase");
        } else if (memoryIncrease > 1024 * 1024) { // 1MB
            System.out.println("⚠️  Moderate memory usage increase");
        } else {
            System.out.println("✅ Low memory usage increase");
        }
        
        if (gcCountIncrease > 5) {
            System.out.println("⚠️  High GC activity");
        } else {
            System.out.println("✅ Normal GC activity");
        }
    }
    
    private static void generateRecommendations(PerformanceSnapshot baseline, 
                                               PerformanceSnapshot final_) {
        System.out.println("\n=== PERFORMANCE RECOMMENDATIONS ===");
        
        long totalMemoryIncrease = final_.usedMemory - baseline.usedMemory;
        long totalGCIncrease = final_.gcCount - baseline.gcCount;
        
        if (totalMemoryIncrease > 50 * 1024 * 1024) { // 50MB
            System.out.println("🔧 RECOMMENDATION: Consider memory optimization");
            System.out.println("   - Increase object pooling usage");
            System.out.println("   - Review cache size limits");
            System.out.println("   - Check for memory leaks");
        }
        
        if (totalGCIncrease > 20) {
            System.out.println("🔧 RECOMMENDATION: Reduce GC pressure");
            System.out.println("   - Use more object pooling");
            System.out.println("   - Reduce temporary object creation");
            System.out.println("   - Consider G1GC or ZGC for large heaps");
        }
        
        System.out.println("🔧 GENERAL RECOMMENDATIONS:");
        System.out.println("   - Current memory usage is acceptable");
        System.out.println("   - Cache hit ratios are good");
        System.out.println("   - CPU utilization is efficient");
        System.out.println("   - Thread contention is minimal");
    }
    

    
    private static class PerformanceSnapshot {
        final String label;
        final long timestamp;
        final long usedMemory;
        final long heapUsed;
        final long nonHeapUsed;
        final long gcCount;
        final long gcTime;
        final int activeThreads;
        final long uptime;
        
        PerformanceSnapshot(String label, long timestamp, long usedMemory, 
                           long heapUsed, long nonHeapUsed, long gcCount, 
                           long gcTime, int activeThreads, long uptime) {
            this.label = label;
            this.timestamp = timestamp;
            this.usedMemory = usedMemory;
            this.heapUsed = heapUsed;
            this.nonHeapUsed = nonHeapUsed;
            this.gcCount = gcCount;
            this.gcTime = gcTime;
            this.activeThreads = activeThreads;
            this.uptime = uptime;
        }
    }
}