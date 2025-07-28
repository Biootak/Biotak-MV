package com.biotak.util;

import com.biotak.debug.AdvancedLogger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optimized concurrency utilities for better performance in multi-threaded environments
 */
public final class ConcurrencyOptimizer {
    
    // Optimized thread pool for background tasks
    private static final ThreadPoolExecutor BACKGROUND_EXECUTOR;
    
    // Lock-free counters for performance monitoring
    private static final AtomicLong calculationCount = new AtomicLong(0);
    private static final AtomicLong cacheHitCount = new AtomicLong(0);
    private static final AtomicLong cacheMissCount = new AtomicLong(0);
    
    // Thread-local storage for expensive objects
    private static final ThreadLocal<java.text.DecimalFormat> DECIMAL_FORMAT_1 = 
        ThreadLocal.withInitial(() -> new java.text.DecimalFormat("#.#"));
    
    private static final ThreadLocal<java.text.DecimalFormat> DECIMAL_FORMAT_2 = 
        ThreadLocal.withInitial(() -> new java.text.DecimalFormat("#.##"));
    
    private static final ThreadLocal<java.text.DecimalFormat> DECIMAL_FORMAT_5 = 
        ThreadLocal.withInitial(() -> new java.text.DecimalFormat("#.#####"));
    
    static {
        // Create optimized thread pool
        int corePoolSize = Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
        int maxPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        
        BACKGROUND_EXECUTOR = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "BiotakBackground-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1); // Lower priority for background tasks
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // Fallback to caller thread if queue is full
        );
        
        // Allow core threads to timeout
        BACKGROUND_EXECUTOR.allowCoreThreadTimeOut(true);
    }
    
    private ConcurrencyOptimizer() {}
    
    /**
     * Submit a background task for execution
     */
    public static Future<?> submitBackgroundTask(Runnable task) {
        return BACKGROUND_EXECUTOR.submit(task);
    }
    
    /**
     * Submit a background task with result
     */
    public static <T> Future<T> submitBackgroundTask(Callable<T> task) {
        return BACKGROUND_EXECUTOR.submit(task);
    }
    
    /**
     * Get thread-local decimal formatter
     */
    public static java.text.DecimalFormat getDecimalFormat1() {
        return DECIMAL_FORMAT_1.get();
    }
    
    public static java.text.DecimalFormat getDecimalFormat2() {
        return DECIMAL_FORMAT_2.get();
    }
    
    public static java.text.DecimalFormat getDecimalFormat5() {
        return DECIMAL_FORMAT_5.get();
    }
    
    /**
     * Increment calculation counter (lock-free)
     */
    public static void incrementCalculationCount() {
        calculationCount.incrementAndGet();
    }
    
    /**
     * Increment cache hit counter (lock-free)
     */
    public static void incrementCacheHit() {
        cacheHitCount.incrementAndGet();
    }
    
    /**
     * Increment cache miss counter (lock-free)
     */
    public static void incrementCacheMiss() {
        cacheMissCount.incrementAndGet();
    }
    
    /**
     * Get performance statistics
     */
    public static String getPerformanceStats() {
        long calculations = calculationCount.get();
        long hits = cacheHitCount.get();
        long misses = cacheMissCount.get();
        long total = hits + misses;
        
        double hitRate = total > 0 ? (double) hits / total * 100.0 : 0.0;
        
        return String.format(
            "Calculations: %d, Cache Hit Rate: %.1f%% (%d/%d), Pool: %d/%d threads",
            calculations, hitRate, hits, total,
            BACKGROUND_EXECUTOR.getActiveCount(), BACKGROUND_EXECUTOR.getPoolSize()
        );
    }
    
    /**
     * Shutdown the background executor (for cleanup)
     */
    public static void shutdown() {
        BACKGROUND_EXECUTOR.shutdown();
        try {
            if (!BACKGROUND_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                BACKGROUND_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            BACKGROUND_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Lock-free double-checked locking pattern for lazy initialization
     */
    public static abstract class LazyInitializer<T> {
        private volatile T instance;
        
        protected abstract T create();
        
        public T get() {
            T result = instance;
            if (result == null) {
                synchronized (this) {
                    result = instance;
                    if (result == null) {
                        instance = result = create();
                    }
                }
            }
            return result;
        }
    }
    
    /**
     * Optimized read-write lock for cache operations
     */
    public static class OptimizedReadWriteLock {
        private final java.util.concurrent.locks.ReadWriteLock lock = 
            new java.util.concurrent.locks.ReentrantReadWriteLock(false); // Non-fair for better performance
        
        public void readLock() {
            lock.readLock().lock();
        }
        
        public void readUnlock() {
            lock.readLock().unlock();
        }
        
        public void writeLock() {
            lock.writeLock().lock();
        }
        
        public void writeUnlock() {
            lock.writeLock().unlock();
        }
        
        public <T> T withReadLock(java.util.function.Supplier<T> supplier) {
            readLock();
            try {
                return supplier.get();
            } finally {
                readUnlock();
            }
        }
        
        public void withWriteLock(Runnable runnable) {
            writeLock();
            try {
                runnable.run();
            } finally {
                writeUnlock();
            }
        }
    }
}