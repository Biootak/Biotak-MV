package com.biotak.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.biotak.debug.AdvancedLogger;

/**
 * High-performance settings cache to reduce I/O operations
 * در هر drawFigures، getSettings() صدها بار فراخوانی می‌شود
 */
public class SettingsCache {
    
    private static final Map<String, Object> cache = new ConcurrentHashMap<>();
    private static final Map<String, Long> timestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 30000; // 30 seconds
    
    // Statistics
    private static long hits = 0;
    private static long misses = 0;
    
    /**
     * Get cached setting value
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Object settings, java.util.function.Supplier<T> fetcher) {
        String fullKey = settings.hashCode() + ":" + key;
        Long timestamp = timestamps.get(fullKey);
        
        // Check if cached value is still valid
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
            Object cached = cache.get(fullKey);
            if (cached != null) {
                hits++;
                return (T) cached;
            }
        }
        
        // Cache miss - fetch new value
        misses++;
        T value = fetcher.get();
        
        // Store in cache
        cache.put(fullKey, value);
        timestamps.put(fullKey, System.currentTimeMillis());
        
        // Clean up old entries periodically
        if ((hits + misses) % 100 == 0) {
            cleanupExpired();
        }
        
        return value;
    }
    
    /**
     * Convenience method for boolean settings
     */
    public static boolean getBoolean(Object settings, String key, boolean defaultValue, 
                                   java.util.function.Function<String, Boolean> fetcher) {
        return get(key + ":bool", settings, () -> fetcher.apply(key));
    }
    
    /**
     * Convenience method for double settings
     */
    public static double getDouble(Object settings, String key, double defaultValue,
                                 java.util.function.Function<String, Double> fetcher) {
        return get(key + ":double", settings, () -> fetcher.apply(key));
    }
    
    /**
     * Convenience method for string settings
     */
    public static String getString(Object settings, String key, String defaultValue,
                                 java.util.function.Function<String, String> fetcher) {
        return get(key + ":string", settings, () -> fetcher.apply(key));
    }
    
    /**
     * Invalidate cache entry
     */
    public static void invalidate(Object settings, String key) {
        String fullKey = settings.hashCode() + ":" + key;
        cache.remove(fullKey);
        timestamps.remove(fullKey);
    }
    
    /**
     * Clear entire cache
     */
    public static void clear() {
        cache.clear();
        timestamps.clear();
        AdvancedLogger.debug("SettingsCache", "clear", "Settings cache cleared");
    }
    
    /**
     * Clean up expired entries
     */
    private static void cleanupExpired() {
        long now = System.currentTimeMillis();
        timestamps.entrySet().removeIf(entry -> {
            boolean expired = now - entry.getValue() > CACHE_DURATION_MS;
            if (expired) {
                cache.remove(entry.getKey());
            }
            return expired;
        });
    }
    
    /**
     * Get cache statistics
     */
    public static String getStats() {
        double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;
        return String.format("Settings Cache: %d entries, %.1f%% hit rate", 
                           cache.size(), hitRate);
    }
    
    /**
     * Reset statistics
     */
    public static void resetStats() {
        hits = 0;
        misses = 0;
    }
}
