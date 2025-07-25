package com.biotak.test;

import com.biotak.util.*;
import com.biotak.core.*;
import com.biotak.enums.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;

/**
 * Accurate Performance Test with real memory and CPU measurements
 */
public class AccuratePerformanceTest {
    
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    
    public static void main(String[] args) {
        System.out.println("=== ACCURATE PERFORMANCE TEST ===");
        
        // Enable CPU time measurement
        if (threadBean.isCurrentThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
        
        // Initial system state
        printSystemInfo();
        
        // Warm up JVM
        warmUpJVM();
        
        // Run actual performance tests
        testMemoryUsage();
        testCPUUsage();
        testCachePerformance();
        testStringOperations();
        testMathOperations();
        testObjectPooling();
        
        // Final system state
        System.out.println("\n=== FINAL SYSTEM STATE ===");
        printSystemInfo();
        
        System.out.println("\n=== ACCURATE PERFORMANCE TEST COMPLETED ===");
    }
    
    private static void printSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        System.out.println("\n--- SYSTEM INFO ---");
        System.out.printf("Max Memory:   %d MB%n", maxMemory / 1024 / 1024);
        System.out.printf("Total Memory: %d MB%n", totalMemory / 1024 / 1024);
        System.out.printf("Used Memory:  %d MB%n", usedMemory / 1024 / 1024);
        System.out.printf("Free Memory:  %d MB%n", freeMemory / 1024 / 1024);
        System.out.printf("Memory Usage: %.1f%%%n", (double) usedMemory / totalMemory * 100);
        
        // Heap memory details
        var heapMemory = memoryBean.getHeapMemoryUsage();
        System.out.printf("Heap Used:    %d MB%n", heapMemory.getUsed() / 1024 / 1024);
        System.out.printf("Heap Max:     %d MB%n", heapMemory.getMax() / 1024 / 1024);
        
        // Non-heap memory details
        var nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        System.out.printf("Non-Heap Used: %d MB%n", nonHeapMemory.getUsed() / 1024 / 1024);
        
        // Thread info
        System.out.printf("Active Threads: %d%n", Thread.activeCount());
        
        // GC info
        long totalGCTime = 0;
        long totalGCCount = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            totalGCTime += gcBean.getCollectionTime();
            totalGCCount += gcBean.getCollectionCount();
        }
        System.out.printf("GC Count: %d, GC Time: %d ms%n", totalGCCount, totalGCTime);
    }
    
    private static void warmUpJVM() {
        System.out.println("\n--- JVM WARM UP ---");
        long startTime = System.currentTimeMillis();
        
        // Warm up common operations
        for (int i = 0; i < 10000; i++) {
            StringUtils.format1f(Math.random() * 100);
            FastMath.fastSqrt(Math.random() * 100);
            ComputationCache.cachePercentage("warmup" + i, Math.random());
        }
        
        // Force GC to clean up warm-up objects
        System.gc();
        
        long warmUpTime = System.currentTimeMillis() - startTime;
        System.out.printf("JVM warmed up in %d ms%n", warmUpTime);
    }
    
    private static void testMemoryUsage() {
        System.out.println("\n--- MEMORY USAGE TEST ---");
        
        // Get initial memory state
        Runtime runtime = Runtime.getRuntime();
        long initialUsed = runtime.totalMemory() - runtime.freeMemory();
        
        // Create objects to test memory usage
        java.util.List<String> testObjects = new java.util.ArrayList<>();
        
        long startTime = System.nanoTime();
        
        // Test 1: String operations
        for (int i = 0; i < 1000; i++) {
            testObjects.add(StringUtils.format1f(Math.random() * 1000));
            testObjects.add(StringUtils.format2f(Math.random() * 1000));
            testObjects.add(StringUtils.format5f(Math.random() * 1000));
        }
        
        long midUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease1 = midUsed - initialUsed;
        
        // Test 2: Object pooling vs regular allocation
        for (int i = 0; i < 1000; i++) {
            StringBuilder sb = PoolManager.getStringBuilder();
            sb.append("Test ").append(i);
            PoolManager.releaseStringBuilder(sb);
        }
        
        long finalUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease2 = finalUsed - midUsed;
        
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0; // Convert to ms
        
        System.out.printf("String operations memory increase: %d KB%n", memoryIncrease1 / 1024);
        System.out.printf("Object pooling memory increase: %d KB%n", memoryIncrease2 / 1024);
        System.out.printf("Total execution time: %.2f ms%n", executionTime);
        System.out.printf("Memory efficiency: %.1f KB/ms%n", (memoryIncrease1 + memoryIncrease2) / 1024.0 / executionTime);
        
        // Clean up
        testObjects.clear();
        System.gc();
    }
    
    private static void testCPUUsage() {
        System.out.println("\n--- CPU USAGE TEST ---");
        
        if (!threadBean.isCurrentThreadCpuTimeSupported()) {
            System.out.println("CPU time measurement not supported");
            return;
        }
        
        long threadId = Thread.currentThread().getId();
        
        // Test 1: Math operations
        long startCpuTime = threadBean.getThreadCpuTime(threadId);
        long startWallTime = System.nanoTime();
        
        double result = 0;
        for (int i = 0; i < 100000; i++) {
            result += FastMath.fastSqrt(i);
            result += FastMath.fastLog2(i + 1);
            result += FastMath.fastAbs(i - 50000);
        }
        
        long endCpuTime = threadBean.getThreadCpuTime(threadId);
        long endWallTime = System.nanoTime();
        
        double cpuTime = (endCpuTime - startCpuTime) / 1_000_000.0; // Convert to ms
        double wallTime = (endWallTime - startWallTime) / 1_000_000.0; // Convert to ms
        double cpuUsage = (cpuTime / wallTime) * 100;
        
        System.out.printf("Math operations result: %.2f%n", result);
        System.out.printf("CPU time: %.2f ms%n", cpuTime);
        System.out.printf("Wall time: %.2f ms%n", wallTime);
        System.out.printf("CPU usage: %.1f%%%n", cpuUsage);
        System.out.printf("Operations per second: %.0f%n", 300000 / (wallTime / 1000));
        
        // Test 2: Cache operations
        startCpuTime = threadBean.getThreadCpuTime(threadId);
        startWallTime = System.nanoTime();
        
        for (int i = 0; i < 10000; i++) {
            ComputationCache.cachePercentage("test" + i, Math.random());
            ComputationCache.getCachedPercentage("test" + (i / 2));
        }
        
        endCpuTime = threadBean.getThreadCpuTime(threadId);
        endWallTime = System.nanoTime();
        
        cpuTime = (endCpuTime - startCpuTime) / 1_000_000.0;
        wallTime = (endWallTime - startWallTime) / 1_000_000.0;
        cpuUsage = (cpuTime / wallTime) * 100;
        
        System.out.printf("Cache CPU time: %.2f ms%n", cpuTime);
        System.out.printf("Cache wall time: %.2f ms%n", wallTime);
        System.out.printf("Cache CPU usage: %.1f%%%n", cpuUsage);
        System.out.printf("Cache ops per second: %.0f%n", 20000 / (wallTime / 1000));
    }
    
    private static void testCachePerformance() {
        System.out.println("\n--- CACHE PERFORMANCE TEST ---");
        
        long startTime = System.nanoTime();
        
        // Test cache hit/miss ratios
        int hits = 0;
        int misses = 0;
        
        // Fill cache
        for (int i = 0; i < 100; i++) {
            ComputationCache.cachePercentage("key" + i, Math.random());
        }
        
        // Test cache performance
        for (int i = 0; i < 1000; i++) {
            String key = "key" + (i % 150); // 100 hits, 50 misses expected
            Double value = ComputationCache.getCachedPercentage(key);
            if (value != null) {
                hits++;
            } else {
                misses++;
                ComputationCache.cachePercentage(key, Math.random());
            }
        }
        
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Cache hits: %d%n", hits);
        System.out.printf("Cache misses: %d%n", misses);
        System.out.printf("Hit ratio: %.1f%%%n", (double) hits / (hits + misses) * 100);
        System.out.printf("Cache lookup time: %.2f ms%n", executionTime);
        System.out.printf("Lookups per second: %.0f%n", 1000 / (executionTime / 1000));
    }
    
    private static void testStringOperations() {
        System.out.println("\n--- STRING OPERATIONS TEST ---");
        
        long startTime = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Test string formatting performance
        for (int i = 0; i < 10000; i++) {
            StringUtils.format1f(Math.random() * 1000);
            StringUtils.format2f(Math.random() * 1000);
            StringUtils.format5f(Math.random() * 1000);
        }
        
        long endTime = System.nanoTime();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        double executionTime = (endTime - startTime) / 1_000_000.0;
        long memoryUsed = finalMemory - initialMemory;
        
        System.out.printf("String operations time: %.2f ms%n", executionTime);
        System.out.printf("Memory used: %d KB%n", memoryUsed / 1024);
        System.out.printf("Operations per second: %.0f%n", 30000 / (executionTime / 1000));
        System.out.printf("Memory per operation: %.2f bytes%n", (double) memoryUsed / 30000);
    }
    
    private static void testMathOperations() {
        System.out.println("\n--- MATH OPERATIONS TEST ---");
        
        long startTime = System.nanoTime();
        
        double result = 0;
        for (int i = 0; i < 50000; i++) {
            result += FastMath.fastSqrt(i + 1);
            result += FastMath.fastLog2(i + 1);
            result += FastMath.fastAbs(i - 25000);
            result += FastMath.fastMin(i, 25000);
            result += FastMath.fastMax(i, 25000);
        }
        
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Math result: %.2f%n", result);
        System.out.printf("Math operations time: %.2f ms%n", executionTime);
        System.out.printf("Operations per second: %.0f%n", 250000 / (executionTime / 1000));
        System.out.printf("Nanoseconds per operation: %.2f%n", (endTime - startTime) / 250000.0);
    }
    
    private static void testObjectPooling() {
        System.out.println("\n--- OBJECT POOLING TEST ---");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Test without pooling
        long startTime = System.nanoTime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        for (int i = 0; i < 5000; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("Test ").append(i).append(" without pooling");
            // No reuse - new object each time
        }
        
        long midTime = System.nanoTime();
        long midMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Test with pooling
        for (int i = 0; i < 5000; i++) {
            StringBuilder sb = PoolManager.getStringBuilder();
            sb.append("Test ").append(i).append(" with pooling");
            PoolManager.releaseStringBuilder(sb);
        }
        
        long endTime = System.nanoTime();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        double timeWithoutPooling = (midTime - startTime) / 1_000_000.0;
        double timeWithPooling = (endTime - midTime) / 1_000_000.0;
        long memoryWithoutPooling = midMemory - initialMemory;
        long memoryWithPooling = finalMemory - midMemory;
        
        System.out.printf("Without pooling - Time: %.2f ms, Memory: %d KB%n", 
                         timeWithoutPooling, memoryWithoutPooling / 1024);
        System.out.printf("With pooling - Time: %.2f ms, Memory: %d KB%n", 
                         timeWithPooling, memoryWithPooling / 1024);
        System.out.printf("Time improvement: %.1f%%%n", 
                         (timeWithoutPooling - timeWithPooling) / timeWithoutPooling * 100);
        System.out.printf("Memory improvement: %.1f%%%n", 
                         (double)(memoryWithoutPooling - memoryWithPooling) / memoryWithoutPooling * 100);
    }
}