package com.biotak.benchmark;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Advanced Benchmarking System
 * 
 * Features:
 * - Method-level benchmarking
 * - Statistical analysis
 * - Performance regression detection
 * - Multi-threaded benchmark tests
 * - Export results to various formats
 * - Comparison with baseline
 */
public class AdvancedBenchmark {
    
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final Map<String, BenchmarkResult> results = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> measurements = new ConcurrentHashMap<>();
    
    static {
        if (threadBean.isCurrentThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
    }
    
    public static class BenchmarkResult {
        public final String name;
        public final long minTime;
        public final long maxTime;
        public final double avgTime;
        public final double medianTime;
        public final double stdDev;
        public final int iterations;
        public final long totalTime;
        public final double throughput;
        public final LocalDateTime timestamp;
        
        public BenchmarkResult(String name, List<Long> times) {
            this.name = name;
            this.iterations = times.size();
            this.timestamp = LocalDateTime.now();
            
            Collections.sort(times);
            this.minTime = times.get(0);
            this.maxTime = times.get(times.size() - 1);
            this.totalTime = times.stream().mapToLong(Long::longValue).sum();
            this.avgTime = totalTime / (double) iterations;
            
            // Calculate median
            if (iterations % 2 == 0) {
                this.medianTime = (times.get(iterations/2 - 1) + times.get(iterations/2)) / 2.0;
            } else {
                this.medianTime = times.get(iterations/2);
            }
            
            // Calculate standard deviation
            double variance = times.stream()
                .mapToDouble(time -> Math.pow(time - avgTime, 2))
                .average().orElse(0.0);
            this.stdDev = Math.sqrt(variance);
            
            // Calculate throughput (operations per second)
            this.throughput = 1_000_000_000.0 / avgTime; // nanoseconds to ops/sec
        }
        
        @Override
        public String toString() {
            return String.format("%s: %.2f ns (±%.2f), min=%.2f, max=%.2f, median=%.2f, throughput=%.0f ops/sec",
                    name, avgTime, stdDev, (double)minTime, (double)maxTime, medianTime, throughput);
        }
        
        public String toDetailedString() {
            return String.format("""
                Benchmark: %s
                Iterations: %d
                Average Time: %.2f ns
                Standard Deviation: %.2f ns
                Min Time: %d ns
                Max Time: %d ns
                Median Time: %.2f ns
                Total Time: %d ns
                Throughput: %.0f ops/sec
                Timestamp: %s
                """, name, iterations, avgTime, stdDev, minTime, maxTime, 
                medianTime, totalTime, throughput, 
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }
    
    @FunctionalInterface
    public interface BenchmarkTask {
        void run() throws Exception;
    }
    
    /**
     * Benchmark a task with specified iterations
     */
    public BenchmarkResult benchmark(String name, BenchmarkTask task, int iterations) {
        System.out.println("🏃 Running benchmark: " + name + " (" + iterations + " iterations)");
        
        List<Long> times = new ArrayList<>();
        
        // Warm-up phase
        try {
            for (int i = 0; i < Math.min(100, iterations / 10); i++) {
                task.run();
            }
        } catch (Exception e) {
            throw new RuntimeException("Warm-up failed", e);
        }
        
        // Force GC before benchmark
        System.gc();
        
        // Actual benchmark
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            try {
                task.run();
            } catch (Exception e) {
                throw new RuntimeException("Benchmark task failed at iteration " + i, e);
            }
            long endTime = System.nanoTime();
            times.add(endTime - startTime);
        }
        
        BenchmarkResult result = new BenchmarkResult(name, times);
        results.put(name, result);
        measurements.put(name, times);
        
        System.out.println("✅ " + result);
        return result;
    }
    
    /**
     * Benchmark with CPU time measurement
     */
    public BenchmarkResult benchmarkWithCPU(String name, BenchmarkTask task, int iterations) {
        if (!threadBean.isCurrentThreadCpuTimeSupported()) {
            System.out.println("⚠️  CPU time measurement not supported, falling back to wall time");
            return benchmark(name, task, iterations);
        }
        
        System.out.println("🏃 Running CPU benchmark: " + name + " (" + iterations + " iterations)");
        
        List<Long> times = new ArrayList<>();
        long threadId = Thread.currentThread().threadId();
        
        // Warm-up
        try {
            for (int i = 0; i < Math.min(100, iterations / 10); i++) {
                task.run();
            }
        } catch (Exception e) {
            throw new RuntimeException("Warm-up failed", e);
        }
        
        System.gc();
        
        // Actual benchmark with CPU time
        for (int i = 0; i < iterations; i++) {
            long startCpuTime = threadBean.getThreadCpuTime(threadId);
            try {
                task.run();
            } catch (Exception e) {
                throw new RuntimeException("Benchmark task failed at iteration " + i, e);
            }
            long endCpuTime = threadBean.getThreadCpuTime(threadId);
            times.add(endCpuTime - startCpuTime);
        }
        
        BenchmarkResult result = new BenchmarkResult(name + " (CPU)", times);
        results.put(name + "_cpu", result);
        measurements.put(name + "_cpu", times);
        
        System.out.println("✅ " + result);
        return result;
    }
    
    /**
     * Multi-threaded benchmark
     */
    public Map<String, BenchmarkResult> benchmarkMultiThreaded(String name, BenchmarkTask task, 
                                                               int iterations, int threadCount) {
        System.out.println("🏃 Running multi-threaded benchmark: " + name + 
                          " (" + iterations + " iterations, " + threadCount + " threads)");
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Map<String, BenchmarkResult> threadResults = new ConcurrentHashMap<>();
        
        try {
            List<Future<BenchmarkResult>> futures = new ArrayList<>();
            
            for (int t = 0; t < threadCount; t++) {
                final int threadIndex = t;
                futures.add(executor.submit(() -> {
                    String threadName = name + "_thread_" + threadIndex;
                    return benchmark(threadName, task, iterations / threadCount);
                }));
            }
            
            // Wait for all threads to complete
            for (Future<BenchmarkResult> future : futures) {
                try {
                    BenchmarkResult result = future.get();
                    threadResults.put(result.name, result);
                } catch (Exception e) {
                    System.err.println("❌ Thread benchmark failed: " + e.getMessage());
                }
            }
            
            // Calculate aggregate results
            calculateAggregateResults(name, threadResults);
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        return threadResults;
    }
    
    private void calculateAggregateResults(String baseName, Map<String, BenchmarkResult> threadResults) {
        if (threadResults.isEmpty()) return;
        
        double avgThroughput = threadResults.values().stream()
            .mapToDouble(r -> r.throughput)
            .average().orElse(0.0);
        
        double totalThroughput = threadResults.values().stream()
            .mapToDouble(r -> r.throughput)
            .sum();
        
        System.out.println("📊 Aggregate Results for " + baseName + ":");
        System.out.println("  Average per-thread throughput: " + String.format("%.0f ops/sec", avgThroughput));
        System.out.println("  Total throughput: " + String.format("%.0f ops/sec", totalThroughput));
    }
    
    /**
     * Compare two benchmark results
     */
    public void compare(String name1, String name2) {
        BenchmarkResult result1 = results.get(name1);
        BenchmarkResult result2 = results.get(name2);
        
        if (result1 == null || result2 == null) {
            System.out.println("❌ Cannot compare: one or both benchmarks not found");
            return;
        }
        
        System.out.println("\n📊 === BENCHMARK COMPARISON ===");
        System.out.println("Benchmark 1: " + result1);
        System.out.println("Benchmark 2: " + result2);
        
        double speedup = result1.avgTime / result2.avgTime;
        double throughputRatio = result2.throughput / result1.throughput;
        
        System.out.println("\nComparison:");
        if (speedup > 1.0) {
            System.out.printf("🚀 %s is %.2fx faster than %s%n", name2, speedup, name1);
        } else {
            System.out.printf("🐌 %s is %.2fx slower than %s%n", name2, 1.0/speedup, name1);
        }
        
        System.out.printf("Throughput ratio: %.2fx%n", throughputRatio);
        System.out.println("===============================\n");
    }
    
    /**
     * Export results to CSV
     */
    public void exportToCSV(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Name,Iterations,AvgTime_ns,StdDev_ns,MinTime_ns,MaxTime_ns,MedianTime_ns,Throughput_ops_per_sec,Timestamp");
            
            for (BenchmarkResult result : results.values()) {
                writer.printf("%s,%d,%.2f,%.2f,%d,%d,%.2f,%.0f,%s%n",
                    result.name, result.iterations, result.avgTime, result.stdDev,
                    result.minTime, result.maxTime, result.medianTime, result.throughput,
                    result.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            
            System.out.println("📄 Benchmark results exported to: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Error exporting results: " + e.getMessage());
        }
    }
    
    /**
     * Generate performance regression report
     */
    public void generateRegressionReport(Map<String, BenchmarkResult> baseline) {
        System.out.println("\n📈 === PERFORMANCE REGRESSION REPORT ===");
        
        for (String benchmarkName : results.keySet()) {
            BenchmarkResult current = results.get(benchmarkName);
            BenchmarkResult base = baseline.get(benchmarkName);
            
            if (base == null) {
                System.out.println("🆕 New benchmark: " + benchmarkName);
                continue;
            }
            
            double regression = (current.avgTime - base.avgTime) / base.avgTime * 100;
            double throughputChange = (current.throughput - base.throughput) / base.throughput * 100;
            
            System.out.printf("📊 %s:%n", benchmarkName);
            System.out.printf("  Time change: %+.1f%% (%.2f ns → %.2f ns)%n", 
                            regression, base.avgTime, current.avgTime);
            System.out.printf("  Throughput change: %+.1f%% (%.0f → %.0f ops/sec)%n", 
                            throughputChange, base.throughput, current.throughput);
            
            if (regression > 10) {
                System.out.println("  🔴 PERFORMANCE REGRESSION DETECTED!");
            } else if (regression < -10) {
                System.out.println("  🟢 Performance improvement!");
            } else {
                System.out.println("  ⚪ No significant change");
            }
            System.out.println();
        }
        
        System.out.println("=========================================\n");
    }
    
    /**
     * Get all benchmark results
     */
    public Map<String, BenchmarkResult> getAllResults() {
        return new HashMap<>(results);
    }
    
    /**
     * Clear all results
     */
    public void clear() {
        results.clear();
        measurements.clear();
        System.out.println("🗑️  All benchmark results cleared");
    }
    
    /**
     * Print summary of all results
     */
    public void printSummary() {
        if (results.isEmpty()) {
            System.out.println("📋 No benchmark results available");
            return;
        }
        
        System.out.println("\n📊 === BENCHMARK SUMMARY ===");
        results.values().stream()
            .sorted(Comparator.comparing(r -> r.name))
            .forEach(result -> System.out.println("  " + result));
        System.out.println("============================\n");
    }
}
