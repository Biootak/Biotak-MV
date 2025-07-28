package com.biotak.monitor;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;
import com.biotak.debug.AdvancedLogger;

/**
 * Real-time Performance Monitor
 * 
 * Features:
 * - Continuous monitoring of CPU, Memory, GC, Threads
 * - Real-time alerts for performance issues
 * - Performance logging and history
 * - Configurable thresholds
 * - Export monitoring data
 */
public class PerformanceMonitor {
    
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final List<PerformanceSnapshot> history = new ArrayList<>();
    private final Map<String, Double> thresholds = new HashMap<>();
    private final List<AlertListener> alertListeners = new ArrayList<>();
    
    private volatile boolean isMonitoring = false;
    private String logFilePath = "performance_monitor.log";
    private int historyLimit = 1000; // Keep last 1000 snapshots
    
    // Performance thresholds (configurable)
    public static class Thresholds {
        public static final double HIGH_MEMORY_USAGE = 80.0; // 80%
        public static final double HIGH_CPU_USAGE = 85.0; // 85%
        public static final long HIGH_GC_TIME = 100; // 100ms
        public static final int HIGH_THREAD_COUNT = 50;
        public static final double LOW_CPU_EFFICIENCY = 60.0; // 60%
    }
    
    public static class PerformanceSnapshot {
        public final LocalDateTime timestamp;
        public final double memoryUsage;
        public final double cpuUsage;
        public final long gcTime;
        public final long gcCount;
        public final int threadCount;
        public final long heapUsed;
        public final long heapMax;
        public final long uptime;
        
        public PerformanceSnapshot() {
            this.timestamp = LocalDateTime.now();
            
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            this.memoryUsage = (double) usedMemory / totalMemory * 100;
            this.cpuUsage = getCPUUsage();
            this.gcTime = getTotalGCTime();
            this.gcCount = getTotalGCCount();
            this.threadCount = threadBean.getThreadCount();
            
            var heapMemory = memoryBean.getHeapMemoryUsage();
            this.heapUsed = heapMemory.getUsed();
            this.heapMax = heapMemory.getMax();
            this.uptime = runtimeBean.getUptime();
        }
        
        private static double getCPUUsage() {
            // Simplified CPU usage calculation
            // In production, you might want to use com.sun.management.OperatingSystemMXBean
            try {
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
                }
            } catch (Exception e) {
                // Fallback calculation
            }
            return -1; // Unknown
        }
        
        private static long getTotalGCTime() {
            return gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        }
        
        private static long getTotalGCCount() {
            return gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        }
        
        @Override
        public String toString() {
            return String.format("%s | Memory: %.1f%% | CPU: %.1f%% | GC: %dms (%d) | Threads: %d",
                    timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    memoryUsage, cpuUsage, gcTime, gcCount, threadCount);
        }
    }
    
    public interface AlertListener {
        void onAlert(AlertType type, String message, PerformanceSnapshot snapshot);
    }
    
    public enum AlertType {
        HIGH_MEMORY, HIGH_CPU, EXCESSIVE_GC, HIGH_THREAD_COUNT, LOW_EFFICIENCY, MEMORY_LEAK
    }
    
    public PerformanceMonitor() {
        initializeThresholds();
        setupDefaultAlertListener();
    }
    
    private void initializeThresholds() {
        thresholds.put("memory_usage", Thresholds.HIGH_MEMORY_USAGE);
        thresholds.put("cpu_usage", Thresholds.HIGH_CPU_USAGE);
        thresholds.put("gc_time", (double) Thresholds.HIGH_GC_TIME);
        thresholds.put("thread_count", (double) Thresholds.HIGH_THREAD_COUNT);
        thresholds.put("cpu_efficiency", Thresholds.LOW_CPU_EFFICIENCY);
    }
    
    private void setupDefaultAlertListener() {
        addAlertListener((type, message, snapshot) -> {
            String alertMsg = String.format("[%s] %s ALERT: %s", 
                    snapshot.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    type, message);
            System.err.println("🚨 " + alertMsg);
            logToFile("ALERT", alertMsg);
        });
    }
    
    public void startMonitoring(int intervalSeconds) {
        if (isMonitoring) {
            System.out.println("⚠️  Monitoring is already running!");
            return;
        }
        
        isMonitoring = true;
        AdvancedLogger.info("PerformanceMonitor", "startMonitoring", "Starting performance monitoring with interval: %d seconds", intervalSeconds);
        System.out.println("🔍 Starting performance monitoring (interval: " + intervalSeconds + "s)");
        
        // Performance monitoring task
        scheduler.scheduleAtFixedRate(() -> {
            try {
                PerformanceSnapshot snapshot = new PerformanceSnapshot();
                addSnapshot(snapshot);
                checkAlerts(snapshot);
                
                if (history.size() % 10 == 0) { // Print every 10th snapshot
                    System.out.println("📊 " + snapshot);
                }
                
            } catch (Exception e) {
                System.err.println("❌ Error in monitoring: " + e.getMessage());
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
        
        // Periodic report task
        scheduler.scheduleAtFixedRate(() -> {
            printPerformanceReport();
        }, 60, 60, TimeUnit.SECONDS); // Every 60 seconds
    }
    
    public void stopMonitoring() {
        if (!isMonitoring) {
            System.out.println("⚠️  Monitoring is not running!");
            return;
        }
        
        isMonitoring = false;
        scheduler.shutdown();
        AdvancedLogger.info("PerformanceMonitor", "stopMonitoring", "Performance monitoring stopped");
        System.out.println("🛑 Performance monitoring stopped");
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void addSnapshot(PerformanceSnapshot snapshot) {
        synchronized (history) {
            history.add(snapshot);
            if (history.size() > historyLimit) {
                history.remove(0); // Remove oldest
            }
        }
        logToFile("SNAPSHOT", snapshot.toString());
    }
    
    private void checkAlerts(PerformanceSnapshot snapshot) {
        // Memory usage alert
        if (snapshot.memoryUsage > thresholds.get("memory_usage")) {
            triggerAlert(AlertType.HIGH_MEMORY, 
                    String.format("Memory usage: %.1f%% (threshold: %.1f%%)", 
                            snapshot.memoryUsage, thresholds.get("memory_usage")), snapshot);
        }
        
        // CPU usage alert
        if (snapshot.cpuUsage > 0 && snapshot.cpuUsage > thresholds.get("cpu_usage")) {
            triggerAlert(AlertType.HIGH_CPU, 
                    String.format("CPU usage: %.1f%% (threshold: %.1f%%)", 
                            snapshot.cpuUsage, thresholds.get("cpu_usage")), snapshot);
        }
        
        // GC time alert
        if (history.size() > 1) {
            PerformanceSnapshot prev = history.get(history.size() - 2);
            long gcTimeDiff = snapshot.gcTime - prev.gcTime;
            if (gcTimeDiff > thresholds.get("gc_time")) {
                triggerAlert(AlertType.EXCESSIVE_GC, 
                        String.format("GC took %dms (threshold: %.0fms)", 
                                gcTimeDiff, thresholds.get("gc_time")), snapshot);
            }
        }
        
        // Thread count alert
        if (snapshot.threadCount > thresholds.get("thread_count")) {
            triggerAlert(AlertType.HIGH_THREAD_COUNT, 
                    String.format("Thread count: %d (threshold: %.0f)", 
                            snapshot.threadCount, thresholds.get("thread_count")), snapshot);
        }
        
        // Memory leak detection (simplified)
        if (history.size() >= 10) {
            checkMemoryLeak();
        }
    }
    
    private void checkMemoryLeak() {
        int size = history.size();
        if (size < 10) return;
        
        // Check if memory usage is consistently increasing
        double avgRecent = 0, avgOld = 0;
        for (int i = size - 5; i < size; i++) {
            avgRecent += history.get(i).memoryUsage;
        }
        for (int i = size - 10; i < size - 5; i++) {
            avgOld += history.get(i).memoryUsage;
        }
        
        avgRecent /= 5;
        avgOld /= 5;
        
        if (avgRecent > avgOld + 10) { // 10% increase
            triggerAlert(AlertType.MEMORY_LEAK, 
                    String.format("Potential memory leak detected. Memory usage increased from %.1f%% to %.1f%%", 
                            avgOld, avgRecent), history.get(size - 1));
        }
    }
    
    private void triggerAlert(AlertType type, String message, PerformanceSnapshot snapshot) {
        for (AlertListener listener : alertListeners) {
            try {
                listener.onAlert(type, message, snapshot);
            } catch (Exception e) {
                System.err.println("❌ Error in alert listener: " + e.getMessage());
            }
        }
    }
    
    public void addAlertListener(AlertListener listener) {
        alertListeners.add(listener);
    }
    
    public void setThreshold(String metric, double value) {
        thresholds.put(metric, value);
        System.out.println("⚙️  Threshold updated: " + metric + " = " + value);
    }
    
    public void printPerformanceReport() {
        if (history.isEmpty()) return;
        
        synchronized (history) {
            System.out.println("\n📈 === PERFORMANCE REPORT ===");
            System.out.println("Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println("Monitoring duration: " + (history.size() * 5) + " seconds");
            
            // Calculate averages
            double avgMemory = history.stream().mapToDouble(s -> s.memoryUsage).average().orElse(0);
            double avgCPU = history.stream().filter(s -> s.cpuUsage > 0).mapToDouble(s -> s.cpuUsage).average().orElse(0);
            double avgThreads = history.stream().mapToDouble(s -> s.threadCount).average().orElse(0);
            
            // Find peaks
            PerformanceSnapshot maxMemory = history.stream().max(Comparator.comparing(s -> s.memoryUsage)).orElse(null);
            PerformanceSnapshot maxCPU = history.stream().filter(s -> s.cpuUsage > 0).max(Comparator.comparing(s -> s.cpuUsage)).orElse(null);
            
            System.out.printf("Average Memory Usage: %.1f%%\n", avgMemory);
            if (maxMemory != null) {
                System.out.printf("Peak Memory Usage: %.1f%% at %s\n", 
                        maxMemory.memoryUsage, maxMemory.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
            
            if (avgCPU > 0) {
                System.out.printf("Average CPU Usage: %.1f%%\n", avgCPU);
            }
            if (maxCPU != null) {
                System.out.printf("Peak CPU Usage: %.1f%% at %s\n", 
                        maxCPU.cpuUsage, maxCPU.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
            
            System.out.printf("Average Thread Count: %.1f\n", avgThreads);
            
            // GC stats
            if (history.size() > 1) {
                PerformanceSnapshot latest = history.get(history.size() - 1);
                PerformanceSnapshot first = history.get(0);
                long totalGC = latest.gcTime - first.gcTime;
                long totalGCCount = latest.gcCount - first.gcCount;
                System.out.printf("Total GC Time: %dms (%d collections)\n", totalGC, totalGCCount);
            }
            
            System.out.println("================================\n");
        }
    }
    
    public void exportData(String filename) {
        try {
            Path path = Paths.get(filename);
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
                writer.println("Timestamp,MemoryUsage,CPUUsage,GCTime,GCCount,ThreadCount,HeapUsed,HeapMax");
                
                synchronized (history) {
                    for (PerformanceSnapshot snapshot : history) {
                        writer.printf("%s,%.2f,%.2f,%d,%d,%d,%d,%d%n",
                                snapshot.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                snapshot.memoryUsage, snapshot.cpuUsage, snapshot.gcTime,
                                snapshot.gcCount, snapshot.threadCount, snapshot.heapUsed, snapshot.heapMax);
                    }
                }
            }
            System.out.println("📄 Performance data exported to: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Failed to export data: " + e.getMessage());
        }
    }
    
    private void logToFile(String level, String message) {
        try {
            String logEntry = String.format("[%s] %s: %s%n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    level, message);
            
            Files.write(Paths.get(logFilePath), logEntry.getBytes(), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Fail silently to avoid spam
        }
    }
    
    public List<PerformanceSnapshot> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }
    
    public void clearHistory() {
        synchronized (history) {
            history.clear();
        }
        System.out.println("🗑️  Performance history cleared");
    }
    
    public boolean isMonitoring() {
        return isMonitoring;
    }
    
    // Main method for standalone monitoring
    public static void main(String[] args) {
        PerformanceMonitor monitor = new PerformanceMonitor();
        
        // Custom alert listener example
        monitor.addAlertListener((type, message, snapshot) -> {
            if (type == AlertType.HIGH_MEMORY) {
                // Could send email, SMS, or push notification
                System.out.println("🔔 Sending alert notification: " + message);
            }
        });
        
        // Set custom thresholds
        monitor.setThreshold("memory_usage", 70.0); // Lower threshold for testing
        
        // Start monitoring every 5 seconds
        monitor.startMonitoring(5);
        
        // Set up shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down monitor...");
            monitor.stopMonitoring();
            monitor.exportData("performance_data.csv");
        }));
        
        // Keep the program running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            monitor.stopMonitoring();
        }
    }
}
