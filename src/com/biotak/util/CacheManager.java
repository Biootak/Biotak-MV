package com.biotak.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.biotak.debug.AdvancedLogger;

/**
 * Centralized cache management to prevent memory leaks and improve performance
 */
public final class CacheManager {
    
    private static final Map<String, Map<String, Object>> caches = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Long>> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long DEFAULT_EXPIRY_MS = 60000; // 1 minute
    private static final int MAX_CACHE_SIZE = 1000; // افزایش برای hit ratio بهتر
    
    // Cleanup scheduler برای پاکسازی خودکار
    private static final java.util.concurrent.ScheduledExecutorService cleanupExecutor = 
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CacheCleanup");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY); // کم‌ترین اولویت
            return t;
        });
    
    static {
        // پاکسازی خودکار هر 90 ثانیه (بهینه‌تر)
        cleanupExecutor.scheduleAtFixedRate(
            CacheManager::cleanupAll, 90, 90, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    private CacheManager() {}
    
    /**
     * Get a cache by name, creating it if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> getCache(String cacheName) {
        return (Map<String, T>) caches.computeIfAbsent(cacheName, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * Get cache timestamps for a specific cache
     */
    private static Map<String, Long> getCacheTimestamps(String cacheName) {
        return cacheTimestamps.computeIfAbsent(cacheName, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * Put a value in cache with default expiry
     */
    public static <T> void put(String cacheName, String key, T value) {
        put(cacheName, key, value, DEFAULT_EXPIRY_MS);
    }
    
    /**
     * Put a value in cache with custom expiry
     */
    public static <T> void put(String cacheName, String key, T value, long expiryMs) {
        Map<String, T> cache = getCache(cacheName);
        Map<String, Long> timestamps = getCacheTimestamps(cacheName);
        
        cache.put(key, value);
        timestamps.put(key, System.currentTimeMillis() + expiryMs);
        
        // Clean up if cache is getting too large
        if (cache.size() > MAX_CACHE_SIZE) {
            cleanup(cacheName);
        }
    }
    
    /**
     * Get a value from cache, returning null if expired or not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String cacheName, String key) {
        Map<String, Object> cache = caches.get(cacheName);
        Map<String, Long> timestamps = cacheTimestamps.get(cacheName);
        
        if (cache == null || timestamps == null) {
            return null;
        }
        
        Long expiry = timestamps.get(key);
        if (expiry == null || System.currentTimeMillis() > expiry) {
            // Expired or not found
            cache.remove(key);
            timestamps.remove(key);
            return null;
        }
        
        return (T) cache.get(key);
    }
    
    /**
     * Check if a key exists and is not expired
     */
    public static boolean contains(String cacheName, String key) {
        return get(cacheName, key) != null;
    }
    
    /**
     * Remove expired entries from a specific cache
     */
    public static void cleanup(String cacheName) {
        Map<String, Object> cache = caches.get(cacheName);
        Map<String, Long> timestamps = cacheTimestamps.get(cacheName);
        
        if (cache == null || timestamps == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        timestamps.entrySet().removeIf(entry -> {
            boolean expired = currentTime > entry.getValue();
            if (expired) {
                cache.remove(entry.getKey());
            }
            return expired;
        });
    }
    
    /**
     * Clean up all caches
     */
    public static void cleanupAll() {
        for (String cacheName : caches.keySet()) {
            cleanup(cacheName);
        }
    }
    
    /**
     * Clear a specific cache
     */
    public static void clear(String cacheName) {
        Map<String, Object> cache = caches.get(cacheName);
        Map<String, Long> timestamps = cacheTimestamps.get(cacheName);
        
        if (cache != null) cache.clear();
        if (timestamps != null) timestamps.clear();
    }
    
    /**
     * Clear all caches
     */
    public static void clearAll() {
        caches.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * Get cache statistics
     */
    public static void logCacheStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Cache Statistics ===\n");
        
        for (Map.Entry<String, Map<String, Object>> entry : caches.entrySet()) {
            String cacheName = entry.getKey();
            int size = entry.getValue().size();
            sb.append(String.format("%-20s: %d entries\n", cacheName, size));
        }
        
        AdvancedLogger.debug("CacheManager", "logCacheStats", sb.toString());
    }
}