package com.biotak.test;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Advanced Performance Test with Bug Detection and Diagnostics
 * 
 * Features:
 * - Memory leak detection
 * - Performance regression detection  
 * - Stress testing
 * - Resource leak detection
 * - Thread safety validation
 * - Garbage collection analysis
 * - System bottleneck identification
 */
public class ComprehensivePerformanceTest {

    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    
    // Performance baselines
    private static final Map<String, Double> performanceBaselines = new HashMap<>();
    private static final List<String> detectedIssues = new ArrayList<>();
    private static final List<String> warnings = new ArrayList<>();
    
    public static void main(String[] args) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("=== ADVANCED PERFORMANCE TEST WITH BUG DETECTION ===");
        System.out.println("Start Time: " + timestamp);
        System.out.println("JVM: " + System.getProperty("java.version") + " - " + System.getProperty("java.vendor"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("Cores: " + Runtime.getRuntime().availableProcessors());
        
        // Enable detailed monitoring
        if (threadBean.isCurrentThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
        
        try {
            // Initialize performance baselines
            initializeBaselines();
            
            // System diagnostics
            runSystemDiagnostics();
            
            // JVM warm-up with monitoring
            warmUpJVMAdvanced();
            
            // Core performance tests
            testMemoryLeaksAndUsage();
            testCPUPerformanceAndBottlenecks();
            testConcurrencyAndThreadSafety();
            testIOPerformanceAndResourceLeaks();
            
            // Stress tests
            runStressTests();
            
            // Memory leak detection
            detectMemoryLeaks();
            
            // Performance regression analysis
            analyzePerformanceRegression();
            
            // Final diagnostics
            runFinalDiagnostics();
            
        } catch (Exception e) {
            System.err.println("\n❌ CRITICAL ERROR: " + e.getMessage());
            e.printStackTrace();
            detectedIssues.add("Critical test failure: " + e.getMessage());
        } finally {
            // Generate comprehensive report
            generateDiagnosticReport();
        }
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
    }

    private static void warmUpJVM() {
        System.out.println("\n--- JVM WARM UP ---");
        long startTime = System.currentTimeMillis();

        // More comprehensive warm-up
        for (int i = 0; i < 50000; i++) {
            Math.pow(Math.random() * 100, 2);
            Math.sin(Math.random() * Math.PI);
            String.valueOf(Math.random()).hashCode();
        }

        // Test collection operations
        List<Double> warmupList = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            warmupList.add(Math.random());
        }
        warmupList.sort(Double::compareTo);

        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        long warmUpTime = System.currentTimeMillis() - startTime;
        System.out.printf("JVM warmed up in %d ms%n", warmUpTime);
    }

    private static void testMemoryUsage() {
        System.out.println("\n--- MEMORY USAGE TEST ---");
        Runtime runtime = Runtime.getRuntime();
        
        // Test 1: Array allocation
        long startTime = System.nanoTime();
        long initialUsed = runtime.totalMemory() - runtime.freeMemory();
        
        List<String> data = new java.util.ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            data.add("Memory test data entry number " + i + " with additional text to consume more memory");
        }
        
        long midUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease1 = midUsed - initialUsed;
        
        // Test 2: Large object creation
        List<double[]> largeArrays = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            double[] bigArray = new double[10000];
            for (int j = 0; j < bigArray.length; j++) {
                bigArray[j] = Math.random();
            }
            largeArrays.add(bigArray);
        }
        
        long finalUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease2 = finalUsed - midUsed;
        long endTime = System.nanoTime();
        
        System.out.printf("String data memory: %d KB%n", memoryIncrease1 / 1024);
        System.out.printf("Array data memory: %d KB%n", memoryIncrease2 / 1024);
        System.out.printf("Total memory used: %d KB%n", (memoryIncrease1 + memoryIncrease2) / 1024);
        System.out.printf("Memory allocation time: %.2f ms%n", (endTime - startTime) / 1_000_000.0);
        
        // Force GC and measure cleanup
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        long afterGC = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Memory after GC: %d KB (freed: %d KB)%n", 
                         afterGC / 1024, (finalUsed - afterGC) / 1024);
    }

    private static void testCPUUsage() {
        System.out.println("\n--- CPU USAGE TEST ---");

        if (!threadBean.isCurrentThreadCpuTimeSupported()) {
            System.out.println("CPU time measurement not supported");
            return;
        }

        long threadId = Thread.currentThread().threadId();
        
        // Test 1: Math operations
        long startCpuTime = threadBean.getThreadCpuTime(threadId);
        long startWallTime = System.nanoTime();
        
        double result = 0;
        for (long i = 0; i < 5000000; i++) {
            result += Math.cos(i * 0.001);
            result += Math.sin(i * 0.001);
            result += Math.sqrt(i + 1);
        }
        
        long endCpuTime = threadBean.getThreadCpuTime(threadId);
        long endWallTime = System.nanoTime();
        
        double cpuTime = (endCpuTime - startCpuTime) / 1_000_000.0;
        double wallTime = (endWallTime - startWallTime) / 1_000_000.0;
        double cpuUsage = (cpuTime / wallTime) * 100;
        
        System.out.printf("Math operations result: %.2f%n", result);
        System.out.printf("CPU time: %.2f ms%n", cpuTime);
        System.out.printf("Wall time: %.2f ms%n", wallTime);
        System.out.printf("CPU usage: %.1f%%%n", cpuUsage);
        System.out.printf("Operations per second: %.0f%n", 15000000 / (wallTime / 1000));
        
        // Test 2: String processing
        startCpuTime = threadBean.getThreadCpuTime(threadId);
        startWallTime = System.nanoTime();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb.append("Performance test string ").append(i).append("\n");
            if (i % 1000 == 0) {
                sb.toString().hashCode(); // Force string creation
                sb.setLength(0); // Reset
            }
        }
        
        endCpuTime = threadBean.getThreadCpuTime(threadId);
        endWallTime = System.nanoTime();
        
        cpuTime = (endCpuTime - startCpuTime) / 1_000_000.0;
        wallTime = (endWallTime - startWallTime) / 1_000_000.0;
        cpuUsage = (cpuTime / wallTime) * 100;
        
        System.out.printf("String CPU time: %.2f ms%n", cpuTime);
        System.out.printf("String wall time: %.2f ms%n", wallTime);
        System.out.printf("String CPU usage: %.1f%%%n", cpuUsage);
    }

    private static void testAdvancedOperations() {
        System.out.println("\n--- ADVANCED OPERATIONS TEST ---");
        
        testConcurrency();
        testIOOperations();
    }
    
    private static void testConcurrency() {
        System.out.println("\n[Concurrency Test]");
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        long startTime = System.nanoTime();
        
        List<Future<Double>> futures = new java.util.ArrayList<>();
        
        // Submit CPU-intensive tasks to multiple threads
        for (int t = 0; t < numThreads; t++) {
            final int threadNum = t;
            Future<Double> future = executor.submit(() -> {
                double result = 0;
                for (int i = 0; i < 1000000; i++) {
                    result += Math.sin(i + threadNum * 1000000);
                }
                return result;
            });
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        double totalResult = 0;
        try {
            for (Future<Double> future : futures) {
                totalResult += future.get();
            }
        } catch (Exception e) {
            System.err.println("Concurrency test error: " + e.getMessage());
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Threads used: %d%n", numThreads);
        System.out.printf("Concurrent execution time: %.2f ms%n", executionTime);
        System.out.printf("Total result: %.2f%n", totalResult);
        System.out.printf("Throughput: %.0f operations/second%n", 
                         (numThreads * 1000000) / (executionTime / 1000));
    }
    
    private static void testIOOperations() {
        System.out.println("\n[I/O Test]");
        
        String testFileName = "performance_test.tmp";
        
        try {
            // Test file writing
            long startTime = System.nanoTime();
            
            try (FileWriter writer = new FileWriter(testFileName)) {
                for (int i = 0; i < 10000; i++) {
                    writer.write("Performance test line " + i + 
                               " with some additional content to make it longer\n");
                }
            }
            
            long writeTime = System.nanoTime() - startTime;
            
            // Test file reading
            startTime = System.nanoTime();
            
            List<String> lines = Files.readAllLines(Paths.get(testFileName));
            
            long readTime = System.nanoTime() - startTime;
            
            // Test file size
            long fileSize = Files.size(Paths.get(testFileName));
            
            System.out.printf("File write time: %.2f ms%n", writeTime / 1_000_000.0);
            System.out.printf("File read time: %.2f ms%n", readTime / 1_000_000.0);
            System.out.printf("File size: %d KB%n", fileSize / 1024);
            System.out.printf("Lines read: %d%n", lines.size());
            System.out.printf("Write throughput: %.2f MB/s%n", 
                             (fileSize / 1024.0 / 1024.0) / (writeTime / 1_000_000_000.0));
            System.out.printf("Read throughput: %.2f MB/s%n", 
                             (fileSize / 1024.0 / 1024.0) / (readTime / 1_000_000_000.0));
            
            // Cleanup
            Files.deleteIfExists(Paths.get(testFileName));
            
        } catch (IOException e) {
            System.err.println("I/O test error: " + e.getMessage());
        }
    }
    
    // Advanced diagnostic methods
    private static void initializeBaselines() {
        performanceBaselines.put("max_memory_mb", (double) Runtime.getRuntime().maxMemory() / 1024 / 1024);
        performanceBaselines.put("available_processors", (double) Runtime.getRuntime().availableProcessors());
        performanceBaselines.put("expected_gc_time_ms", 50.0);
        performanceBaselines.put("expected_warmup_time_ms", 200.0);
    }
    
    private static void runSystemDiagnostics() {
        System.out.println("\n🔍 === SYSTEM DIAGNOSTICS ===");
        
        // Check memory configuration
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        
        if (maxMemory < 512 * 1024 * 1024) {
            warnings.add("Low heap size: " + (maxMemory / 1024 / 1024) + "MB. Consider increasing with -Xmx");
        }
        
        // Check GC configuration
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.println("GC: " + gcBean.getName() + " - Collections: " + gcBean.getCollectionCount());
        }
        
        // Check thread configuration
        ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();
        System.out.println("Current threads: " + threadMX.getThreadCount());
        System.out.println("Peak threads: " + threadMX.getPeakThreadCount());
        
        if (threadMX.getThreadCount() > 100) {
            warnings.add("High thread count detected: " + threadMX.getThreadCount());
        }
        
        // Check uptime
        long uptime = runtimeBean.getUptime();
        System.out.println("JVM uptime: " + uptime + "ms");
    }
    
    private static void warmUpJVMAdvanced() {
        System.out.println("\n🔥 === ADVANCED JVM WARM-UP ===");
        long startTime = System.currentTimeMillis();
        
        // Get initial GC stats
        long initialGCTime = getTotalGCTime();
        long initialGCCount = getTotalGCCount();
        
        // Comprehensive warm-up
        for (int i = 0; i < 100000; i++) {
            // Math operations
            Math.pow(Math.random() * 100, 2);
            Math.sin(Math.random() * Math.PI);
            Math.log(Math.random() * 100 + 1);
            
            // String operations
            String.valueOf(Math.random()).intern();
            
            // Collection operations
            if (i % 1000 == 0) {
                List<Integer> tempList = new ArrayList<>();
                for (int j = 0; j < 100; j++) {
                    tempList.add(j);
                }
                tempList.sort(Integer::compareTo);
            }
        }
        
        // Force GC and wait
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        long warmUpTime = System.currentTimeMillis() - startTime;
        long finalGCTime = getTotalGCTime();
        long finalGCCount = getTotalGCCount();
        
        System.out.printf("Warm-up time: %d ms%n", warmUpTime);
        System.out.printf("GC during warm-up: %d collections, %d ms%n", 
                         finalGCCount - initialGCCount, finalGCTime - initialGCTime);
        
        if (warmUpTime > performanceBaselines.get("expected_warmup_time_ms")) {
            warnings.add("Slow warm-up detected: " + warmUpTime + "ms (expected < " + 
                        performanceBaselines.get("expected_warmup_time_ms") + "ms)");
        }
    }
    
    private static void testMemoryLeaksAndUsage() {
        System.out.println("\n💾 === MEMORY LEAK DETECTION ===");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Simulate potential memory leak scenarios
        List<Object> potentialLeaks = new ArrayList<>();
        
        for (int cycle = 0; cycle < 5; cycle++) {
            System.out.printf("Memory test cycle %d...%n", cycle + 1);
            
            // Create objects that might not be properly cleaned
            List<String> data = new ArrayList<>();
            for (int i = 0; i < 50000; i++) {
                data.add("Data " + i + " in cycle " + cycle);
            }
            
            // Intentionally keep some references (potential leak)
            if (cycle % 2 == 0) {
                potentialLeaks.add(data.subList(0, Math.min(1000, data.size())));
            }
            
            // Measure memory after each cycle
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("  Memory used: %d KB%n", currentMemory / 1024);
            
            // Force GC
            System.gc();
            try { Thread.sleep(50); } catch (InterruptedException e) {}
            
            long afterGC = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("  After GC: %d KB%n", afterGC / 1024);
            
            // Check for memory growth pattern
            if (cycle > 0 && (afterGC - initialMemory) > cycle * 5 * 1024 * 1024) {
                warnings.add("Potential memory leak detected in cycle " + cycle);
            }
        }
        
        // Clear potential leaks
        potentialLeaks.clear();
        System.gc();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Final memory usage: %d KB%n", finalMemory / 1024);
        
        if (finalMemory > initialMemory * 2) {
            detectedIssues.add("Memory usage increased significantly: " + 
                             (finalMemory - initialMemory) / 1024 + " KB");
        }
    }
    
    private static void testCPUPerformanceAndBottlenecks() {
        System.out.println("\n⚡ === CPU PERFORMANCE & BOTTLENECK DETECTION ===");
        
        if (!threadBean.isCurrentThreadCpuTimeSupported()) {
            warnings.add("CPU time measurement not supported on this platform");
            return;
        }
        
        long threadId = Thread.currentThread().threadId();
        
        // Test different CPU-intensive operations
        testMathOperationsPerformance(threadId);
        testStringOperationsPerformance(threadId);
        testCollectionOperationsPerformance(threadId);
    }
    
    private static void testMathOperationsPerformance(long threadId) {
        System.out.println("\n📊 Math Operations Performance:");
        
        long startCpuTime = threadBean.getThreadCpuTime(threadId);
        long startWallTime = System.nanoTime();
        
        double result = 0;
        for (int i = 0; i < 1000000; i++) {
            result += Math.sin(i * 0.001);
            result += Math.cos(i * 0.001);
            result += Math.sqrt(i + 1);
            result += Math.log(i + 1);
        }
        
        long endCpuTime = threadBean.getThreadCpuTime(threadId);
        long endWallTime = System.nanoTime();
        
        double cpuTime = (endCpuTime - startCpuTime) / 1_000_000.0;
        double wallTime = (endWallTime - startWallTime) / 1_000_000.0;
        double efficiency = (cpuTime / wallTime) * 100;
        
        System.out.printf("  CPU time: %.2f ms, Wall time: %.2f ms%n", cpuTime, wallTime);
        System.out.printf("  CPU efficiency: %.1f%%, Operations/sec: %.0f%n", 
                         efficiency, 4000000 / (wallTime / 1000));
        
        if (efficiency < 80) {
            warnings.add("Low CPU efficiency in math operations: " + String.format("%.1f%%", efficiency));
        }
        
        // Store baseline for regression testing
        performanceBaselines.put("math_ops_per_second", 4000000 / (wallTime / 1000));
    }
    
    private static void testStringOperationsPerformance(long threadId) {
        System.out.println("\n🔤 String Operations Performance:");
        
        long startCpuTime = threadBean.getThreadCpuTime(threadId);
        long startWallTime = System.nanoTime();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb.append("Test string ").append(i).append(" with data");
            if (i % 5000 == 0) {
                String result = sb.toString();
                sb.setLength(0);
                result.hashCode(); // Use the string
            }
        }
        
        long endCpuTime = threadBean.getThreadCpuTime(threadId);
        long endWallTime = System.nanoTime();
        
        double cpuTime = (endCpuTime - startCpuTime) / 1_000_000.0;
        double wallTime = (endWallTime - startWallTime) / 1_000_000.0;
        
        System.out.printf("  String operations - CPU: %.2f ms, Wall: %.2f ms%n", cpuTime, wallTime);
        
        performanceBaselines.put("string_ops_time_ms", wallTime);
    }
    
    private static void testCollectionOperationsPerformance(long threadId) {
        System.out.println("\n📋 Collection Operations Performance:");
        
        long startCpuTime = threadBean.getThreadCpuTime(threadId);
        long startWallTime = System.nanoTime();
        
        List<Integer> list = new ArrayList<>();
        Set<Integer> set = new HashSet<>();
        Map<Integer, String> map = new HashMap<>();
        
        // Insert operations
        for (int i = 0; i < 100000; i++) {
            list.add(i);
            set.add(i);
            map.put(i, "Value " + i);
        }
        
        // Search operations
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            int key = random.nextInt(100000);
            list.contains(key);
            set.contains(key);
            map.get(key);
        }
        
        long endCpuTime = threadBean.getThreadCpuTime(threadId);
        long endWallTime = System.nanoTime();
        
        double wallTime = (endWallTime - startWallTime) / 1_000_000.0;
        System.out.printf("  Collection operations time: %.2f ms%n", wallTime);
        
        performanceBaselines.put("collection_ops_time_ms", wallTime);
    }
    
    private static void testConcurrencyAndThreadSafety() {
        System.out.println("\n🔄 === CONCURRENCY & THREAD SAFETY TEST ===");
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        // Shared counter to test thread safety issues
        final AtomicInteger safeCounter = new AtomicInteger(0);
        final int[] unsafeCounter = {0};
        
        List<Future<?>> futures = new ArrayList<>();
        long startTime = System.nanoTime();
        
        for (int t = 0; t < numThreads; t++) {
            Future<?> future = executor.submit(() -> {
                for (int i = 0; i < 100000; i++) {
                    safeCounter.incrementAndGet();
                    // Unsafe operation (potential race condition)
                    unsafeCounter[0]++;
                    
                    // CPU work
                    Math.sin(i * 0.001);
                }
            });
            futures.add(future);
        }
        
        // Wait for completion
        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                detectedIssues.add("Concurrency test failure: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        long endTime = System.nanoTime();
        
        double executionTime = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Concurrency test completed in %.2f ms%n", executionTime);
        System.out.printf("Safe counter: %d, Unsafe counter: %d%n", 
                         safeCounter.get(), unsafeCounter[0]);
        
        int expectedCount = numThreads * 100000;
        if (safeCounter.get() != expectedCount) {
            detectedIssues.add("Thread-safe counter mismatch: expected " + expectedCount + 
                             ", got " + safeCounter.get());
        }
        
        if (unsafeCounter[0] != expectedCount) {
            System.out.println("⚠️  Race condition detected in unsafe counter: expected " + 
                             expectedCount + ", got " + unsafeCounter[0]);
        }
    }
    
    private static void testIOPerformanceAndResourceLeaks() {
        System.out.println("\n💿 === I/O PERFORMANCE & RESOURCE LEAK TEST ===");
        
        String testDir = "performance_test_files";
        
        try {
            Files.createDirectories(Paths.get(testDir));
            
            // Test file I/O performance
            testFileIOPerformance(testDir);
            
            // Test resource leak detection
            testResourceLeaks(testDir);
            
        } catch (IOException e) {
            detectedIssues.add("I/O test setup failed: " + e.getMessage());
        } finally {
            // Cleanup
            try {
                Files.walk(Paths.get(testDir))
                     .sorted(Comparator.reverseOrder())
                     .forEach(path -> {
                         try { Files.deleteIfExists(path); } 
                         catch (IOException e) { /* ignore */ }
                     });
            } catch (IOException e) {
                warnings.add("Cleanup failed: " + e.getMessage());
            }
        }
    }
    
    private static void testFileIOPerformance(String testDir) throws IOException {
        String testFile = testDir + "/performance_test.txt";
        
        // Write performance test
        long startTime = System.nanoTime();
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(testFile))) {
            for (int i = 0; i < 50000; i++) {
                writer.write("Performance test line " + i + " with additional content for testing\n");
            }
        }
        long writeTime = System.nanoTime() - startTime;
        
        // Read performance test
        startTime = System.nanoTime();
        List<String> lines = Files.readAllLines(Paths.get(testFile));
        long readTime = System.nanoTime() - startTime;
        
        long fileSize = Files.size(Paths.get(testFile));
        
        System.out.printf("File I/O Performance:%n");
        System.out.printf("  Write time: %.2f ms%n", writeTime / 1_000_000.0);
        System.out.printf("  Read time: %.2f ms%n", readTime / 1_000_000.0);
        System.out.printf("  File size: %d KB%n", fileSize / 1024);
        System.out.printf("  Lines: %d%n", lines.size());
        
        double writeThroughput = (fileSize / 1024.0 / 1024.0) / (writeTime / 1_000_000_000.0);
        double readThroughput = (fileSize / 1024.0 / 1024.0) / (readTime / 1_000_000_000.0);
        
        System.out.printf("  Write throughput: %.2f MB/s%n", writeThroughput);
        System.out.printf("  Read throughput: %.2f MB/s%n", readThroughput);
        
        if (writeThroughput < 10.0) {
            warnings.add("Low write throughput: " + String.format("%.2f MB/s", writeThroughput));
        }
    }
    
    private static void testResourceLeaks(String testDir) {
        System.out.println("\n🔍 Resource Leak Detection:");
        
        // Test file handle leaks
        List<FileInputStream> streams = new ArrayList<>();
        
        try {
            String testFile = testDir + "/leak_test.txt";
            try {
                Files.write(Paths.get(testFile), "Test data".getBytes());
            } catch (IOException e) {
                warnings.add("Failed to create test file: " + e.getMessage());
                return;
            }
            
            // Create multiple streams without closing (potential leak)
            for (int i = 0; i < 10; i++) {
                try {
                    FileInputStream stream = new FileInputStream(testFile);
                    streams.add(stream);
                } catch (IOException e) {
                    warnings.add("File handle limit reached at " + i + " streams");
                    break;
                }
            }
            
            System.out.printf("  Created %d file streams%n", streams.size());
            
        } finally {
            // Proper cleanup
            for (FileInputStream stream : streams) {
                try {
                    stream.close();
                } catch (IOException e) {
                    warnings.add("Error closing stream: " + e.getMessage());
                }
            }
        }
    }
    
    private static void runStressTests() {
        System.out.println("\n🔥 === STRESS TESTS ===");
        
        // Memory stress test
        stressTestMemory();
        
        // CPU stress test
        stressTestCPU();
        
        // Concurrent stress test
        stressTestConcurrency();
    }
    
    private static void stressTestMemory() {
        System.out.println("\n💾 Memory Stress Test:");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        List<byte[]> memoryHog = new ArrayList<>();
        
        try {
            // Allocate memory until we get close to the limit
            for (int i = 0; i < 1000; i++) {
                byte[] chunk = new byte[1024 * 1024]; // 1MB chunks
                Arrays.fill(chunk, (byte) i);
                memoryHog.add(chunk);
                
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                if (currentMemory > runtime.maxMemory() * 0.8) {
                    System.out.printf("  Stopping at %d MB allocated%n", i);
                    break;
                }
            }
            
            System.out.printf("  Allocated %d chunks%n", memoryHog.size());
            
        } catch (OutOfMemoryError e) {
            warnings.add("OutOfMemoryError during stress test at " + memoryHog.size() + " MB");
        } finally {
            memoryHog.clear();
            System.gc();
        }
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("  Memory recovered: %d KB%n", (finalMemory - initialMemory) / 1024);
    }
    
    private static void stressTestCPU() {
        System.out.println("\n⚡ CPU Stress Test:");
        
        long startTime = System.nanoTime();
        
        // Intensive computation for 2 seconds
        long endTime = startTime + 2_000_000_000L; // 2 seconds in nanoseconds
        long operations = 0;
        
        while (System.nanoTime() < endTime) {
            Math.sin(Math.random() * Math.PI);
            Math.cos(Math.random() * Math.PI);
            Math.sqrt(Math.random() * 1000);
            operations += 3;
        }
        
        double actualTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.printf("  Operations in %.2fs: %d (%.0f ops/sec)%n", 
                         actualTime, operations, operations / actualTime);
    }
    
    private static void stressTestConcurrency() {
        System.out.println("\n🔄 Concurrency Stress Test:");
        
        int numThreads = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    // Each thread does intensive work
                    for (int j = 0; j < 500000; j++) {
                        Math.pow(j, 0.5);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            warnings.add("Concurrency stress test interrupted");
        }
        
        executor.shutdown();
        long endTime = System.nanoTime();
        
        double executionTime = (endTime - startTime) / 1_000_000.0;
        System.out.printf("  %d threads completed in %.2f ms%n", numThreads, executionTime);
    }
    
    private static void detectMemoryLeaks() {
        System.out.println("\n🕵️ === MEMORY LEAK DETECTION ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Multiple GC cycles to ensure cleanup
        for (int i = 0; i < 3; i++) {
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        
        long memoryAfterGC = runtime.totalMemory() - runtime.freeMemory();
        
        // Create and destroy objects
        List<String> temp = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            temp.add("Memory leak test " + i);
        }
        
        long memoryWithObjects = runtime.totalMemory() - runtime.freeMemory();
        temp.clear();
        temp = null;
        
        // Force GC again
        for (int i = 0; i < 3; i++) {
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        
        long memoryAfterCleanup = runtime.totalMemory() - runtime.freeMemory();
        
        System.out.printf("Memory states:%n");
        System.out.printf("  After initial GC: %d KB%n", memoryAfterGC / 1024);
        System.out.printf("  With objects: %d KB%n", memoryWithObjects / 1024);
        System.out.printf("  After cleanup: %d KB%n", memoryAfterCleanup / 1024);
        
        long leakedMemory = memoryAfterCleanup - memoryAfterGC;
        if (leakedMemory > 1024 * 1024) { // 1MB threshold
            detectedIssues.add("Potential memory leak detected: " + (leakedMemory / 1024) + " KB not freed");
        }
        
        System.out.printf("  Potential leaked memory: %d KB%n", leakedMemory / 1024);
    }
    
    private static void analyzePerformanceRegression() {
        System.out.println("\n📈 === PERFORMANCE REGRESSION ANALYSIS ===");
        
        // Compare current performance with baselines
        if (performanceBaselines.containsKey("math_ops_per_second")) {
            double mathOpsPerSec = performanceBaselines.get("math_ops_per_second");
            System.out.printf("Math operations performance: %.0f ops/sec%n", mathOpsPerSec);
            
            // Expected baseline (this would normally come from historical data)
            double expectedMathOps = 10000000; // 10M ops/sec baseline
            if (mathOpsPerSec < expectedMathOps * 0.8) {
                detectedIssues.add("Performance regression in math operations: " + 
                                 String.format("%.0f ops/sec (expected > %.0f)", mathOpsPerSec, expectedMathOps * 0.8));
            }
        }
        
        if (performanceBaselines.containsKey("string_ops_time_ms")) {
            double stringOpsTime = performanceBaselines.get("string_ops_time_ms");
            System.out.printf("String operations time: %.2f ms%n", stringOpsTime);
            
            double expectedStringTime = 50.0; // 50ms baseline
            if (stringOpsTime > expectedStringTime * 1.5) {
                detectedIssues.add("Performance regression in string operations: " + 
                                 String.format("%.2f ms (expected < %.2f ms)", stringOpsTime, expectedStringTime * 1.5));
            }
        }
    }
    
    private static void runFinalDiagnostics() {
        System.out.println("\n🔬 === FINAL DIAGNOSTICS ===");
        
        // Final GC analysis
        long totalGCTime = getTotalGCTime();
        long totalGCCount = getTotalGCCount();
        
        System.out.printf("Final GC stats: %d collections, %d ms total%n", totalGCCount, totalGCTime);
        
        if (totalGCTime > performanceBaselines.get("expected_gc_time_ms") * 10) {
            warnings.add("Excessive GC time: " + totalGCTime + "ms");
        }
        
        // Thread analysis
        ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();
        System.out.printf("Final thread count: %d (peak: %d)%n", 
                         threadMX.getThreadCount(), threadMX.getPeakThreadCount());
        
        // Memory analysis
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsagePercent = (double) usedMemory / runtime.totalMemory() * 100;
        
        System.out.printf("Final memory usage: %.1f%% (%d KB)%n", 
                         memoryUsagePercent, usedMemory / 1024);
        
        if (memoryUsagePercent > 80) {
            warnings.add("High memory usage: " + String.format("%.1f%%", memoryUsagePercent));
        }
    }
    
    private static void generateDiagnosticReport() {
        System.out.println("\n📋 === COMPREHENSIVE DIAGNOSTIC REPORT ===");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("Report generated: " + timestamp);
        
        // Summary
        System.out.println("\n📊 SUMMARY:");
        System.out.printf("  Issues detected: %d%n", detectedIssues.size());
        System.out.printf("  Warnings: %d%n", warnings.size());
        
        // Issues
        if (!detectedIssues.isEmpty()) {
            System.out.println("\n❌ CRITICAL ISSUES:");
            for (int i = 0; i < detectedIssues.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, detectedIssues.get(i));
            }
        }
        
        // Warnings
        if (!warnings.isEmpty()) {
            System.out.println("\n⚠️  WARNINGS:");
            for (int i = 0; i < warnings.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, warnings.get(i));
            }
        }
        
        // Performance baselines
        System.out.println("\n📈 PERFORMANCE METRICS:");
        for (Map.Entry<String, Double> entry : performanceBaselines.entrySet()) {
            System.out.printf("  %s: %.2f%n", entry.getKey(), entry.getValue());
        }
        
        // Overall assessment
        System.out.println("\n🎯 OVERALL ASSESSMENT:");
        if (detectedIssues.isEmpty() && warnings.size() <= 2) {
            System.out.println("  ✅ EXCELLENT - System performance is optimal");
        } else if (detectedIssues.isEmpty() && warnings.size() <= 5) {
            System.out.println("  ✅ GOOD - Minor issues detected, system is stable");
        } else if (detectedIssues.size() <= 2) {
            System.out.println("  ⚠️  FAIR - Some issues detected, review recommended");
        } else {
            System.out.println("  ❌ POOR - Multiple issues detected, immediate attention required");
        }
        
        System.out.println("\n=== DIAGNOSTIC TEST COMPLETED ===");
    }
    
    // Helper methods
    private static long getTotalGCTime() {
        return gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
    }
    
    private static long getTotalGCCount() {
        return gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
    }
}

