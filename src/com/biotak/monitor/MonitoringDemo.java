package com.biotak.monitor;

import java.util.concurrent.TimeUnit;

/**
 * Demo class to showcase monitoring capabilities
 */
public class MonitoringDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting Performance Monitoring Demo");
        
        PerformanceMonitor monitor = new PerformanceMonitor();
        
        // Lower thresholds for demonstration
        monitor.setThreshold("memory_usage", 15.0);
        monitor.setThreshold("thread_count", 10.0);
        
        // Start monitoring every 2 seconds
        monitor.startMonitoring(2);
        
        try {
            // Simulate some workload
            simulateWorkload();
            
            // Wait a bit to collect data
            Thread.sleep(20000);
            
            // Generate report
            monitor.printPerformanceReport();
            
            // Export data
            monitor.exportData("demo_performance.csv");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            monitor.stopMonitoring();
        }
        
        System.out.println("📊 Demo completed!");
    }
    
    private static void simulateWorkload() {
        System.out.println("💼 Simulating workload...");
        
        // Create multiple threads to increase thread count
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    // CPU intensive work
                    for (int j = 0; j < 1000000; j++) {
                        Math.sin(j * 0.001);
                        Math.cos(j * 0.001);
                    }
                    
                    // Memory intensive work
                    java.util.List<String> data = new java.util.ArrayList<>();
                    for (int j = 0; j < 50000; j++) {
                        data.add("Thread " + threadId + " data " + j);
                    }
                    
                    Thread.sleep(5000);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "WorkerThread-" + i).start();
        }
        
        // Main thread work
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
