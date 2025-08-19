package com.biotak.util;

import java.util.concurrent.*;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import com.biotak.debug.AdvancedLogger;

/**
 * High-performance async logging system to prevent thread blocking
 * مشکل فعلی: synchronized در writeToFile باعث کندی می‌شود
 */
public class AsyncLogger {
    
    private static final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>(10000);
    private static final ExecutorService loggerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AsyncLogger");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    
    // Ring buffer for high-frequency logging
    private static final RingBuffer<String> recentLogs = new RingBuffer<>(1000);
    
    private static volatile boolean shutdown = false;
    private static long droppedMessages = 0;
    
    static {
        // Start async processing thread
        loggerExecutor.submit(AsyncLogger::processLogEntries);
        
        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
            try {
                if (!loggerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    loggerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                loggerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }
    
    /**
     * Log entry container
     */
    private static class LogEntry {
        final String filename;
        final String message;
        final long timestamp;
        
        LogEntry(String filename, String message) {
            this.filename = filename;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Simple ring buffer for recent logs
     */
    private static class RingBuffer<T> {
        private final T[] buffer;
        private int head = 0;
        private final int capacity;
        
        @SuppressWarnings("unchecked")
        RingBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = (T[]) new Object[capacity];
        }
        
        synchronized void add(T item) {
            buffer[head] = item;
            head = (head + 1) % capacity;
        }
        
        synchronized T[] getRecent(int count) {
            count = Math.min(count, capacity);
            @SuppressWarnings("unchecked")
            T[] result = (T[]) new Object[count];
            for (int i = 0; i < count; i++) {
                int index = (head - count + i + capacity) % capacity;
                result[i] = buffer[index];
            }
            return result;
        }
    }
    
    /**
     * Async log method - non-blocking
     */
    public static void logAsync(String filename, String message) {
        if (shutdown) return;
        
        LogEntry entry = new LogEntry(filename, message);
        
        // Try to add to queue without blocking
        if (!logQueue.offer(entry)) {
            droppedMessages++;
            // Store in ring buffer as fallback
            recentLogs.add(message);
            
            // Log drop warning occasionally
            if (droppedMessages % 1000 == 0) {
                System.err.println("AsyncLogger: Dropped " + droppedMessages + " messages due to queue overflow");
            }
        }
    }
    
    /**
     * Process log entries in background thread
     */
    private static void processLogEntries() {
        while (!shutdown || !logQueue.isEmpty()) {
            try {
                LogEntry entry = logQueue.poll(1, TimeUnit.SECONDS);
                if (entry != null) {
                    writeToFileDirectly(entry.filename, entry.message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("AsyncLogger error: " + e.getMessage());
                // Continue processing other entries
            }
        }
    }
    
    /**
     * Direct file writing without synchronization bottleneck
     */
    private static void writeToFileDirectly(String filename, String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
            writer.println(message);
        } catch (IOException e) {
            System.err.println("Failed to write async log to " + filename + ": " + e.getMessage());
            // Store failed message in ring buffer
            recentLogs.add(message);
        }
    }
    
    /**
     * Force flush all pending logs (blocking)
     */
    public static void flush() {
        // Add a special flush marker
        logQueue.offer(new LogEntry("FLUSH", ""));
        
        // Wait for queue to be processed
        while (!logQueue.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Get recent logs from ring buffer
     */
    public static String[] getRecentLogs(int count) {
        return recentLogs.getRecent(count);
    }
    
    /**
     * Get statistics
     */
    public static String getStats() {
        return String.format("AsyncLogger: Queue size: %d, Dropped: %d", 
                           logQueue.size(), droppedMessages);
    }
    
    /**
     * Shutdown async logger
     */
    public static void shutdown() {
        shutdown = true;
        flush();
    }
    
    /**
     * Check if queue is getting full
     */
    public static boolean isQueueFull() {
        return logQueue.remainingCapacity() < 1000; // Less than 1000 slots remaining
    }
}
