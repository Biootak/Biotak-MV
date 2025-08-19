package com.biotak.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.biotak.debug.AdvancedLogger;

/**
 * Smart caching system for BiotakTrigger to prevent redundant calculations
 * and reduce memory pressure
 */
public class SmartCache {
    
    // Cache keys based on critical parameters
    private static class CacheKey {
        private final String timeframe;
        private final double price;
        private final String mode;
        private final long timestamp;
        
        public CacheKey(String timeframe, double price, String mode) {
            this.timeframe = timeframe;
            this.price = Math.round(price * 100000) / 100000.0; // Round to 5 decimals
            this.mode = mode;
            this.timestamp = System.currentTimeMillis() / 60000; // 1-minute buckets
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CacheKey)) return false;
            CacheKey other = (CacheKey) obj;
            return timeframe.equals(other.timeframe) && 
                   Double.compare(price, other.price) == 0 &&
                   mode.equals(other.mode) &&
                   Math.abs(timestamp - other.timestamp) < 2; // Allow 2-minute tolerance
        }
        
        @Override
        public int hashCode() {
            return java.util.Objects.hash(timeframe, price, mode);
        }
    }
    
    // Cached calculation results
    private static class CachedResults {
        final Map<String, Double> mValues;
        final Map<String, Double> atrValues;
        final Map<String, Double> eValues;
        final Map<String, Double> tpValues;
        final Map<String, Double> thValues;
        final Map<String, Double> ssValues;
        final Map<String, Double> lsValues;
        final long createdAt;
        
        public CachedResults(Map<String, Double> mValues, Map<String, Double> atrValues,
                           Map<String, Double> eValues, Map<String, Double> tpValues,
                           Map<String, Double> thValues, Map<String, Double> ssValues,
                           Map<String, Double> lsValues) {
            this.mValues = new ConcurrentHashMap<>(mValues);
            this.atrValues = new ConcurrentHashMap<>(atrValues);
            this.eValues = new ConcurrentHashMap<>(eValues);
            this.tpValues = new ConcurrentHashMap<>(tpValues);
            this.thValues = new ConcurrentHashMap<>(thValues);
            this.ssValues = new ConcurrentHashMap<>(ssValues);
            this.lsValues = new ConcurrentHashMap<>(lsValues);
            this.createdAt = System.currentTimeMillis();
        }
        
        public boolean isValid() {
            return System.currentTimeMillis() - createdAt < 120000; // Valid for 2 minutes
        }
    }
    
    private static final Map<CacheKey, CachedResults> cache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 50;
    
    // Cache hit/miss statistics
    private static long cacheHits = 0;
    private static long cacheMisses = 0;
    
    /**
     * Get cached results or null if not found/expired
     */
    public static CachedResults get(String timeframe, double price, String mode) {
        CacheKey key = new CacheKey(timeframe, price, mode);
        CachedResults result = cache.get(key);
        
        if (result != null && result.isValid()) {
            cacheHits++;
            AdvancedLogger.debug("SmartCache", "get", "Cache HIT for %s|%.5f|%s", timeframe, price, mode);
            return result;
        }
        
        if (result != null) {
            // Remove expired entry
            cache.remove(key);
        }
        
        cacheMisses++;
        AdvancedLogger.debug("SmartCache", "get", "Cache MISS for %s|%.5f|%s", timeframe, price, mode);
        return null;
    }
    
    /**
     * Store results in cache
     */
    public static void put(String timeframe, double price, String mode, 
                          Map<String, Double> mValues, Map<String, Double> atrValues,
                          Map<String, Double> eValues, Map<String, Double> tpValues,
                          Map<String, Double> thValues, Map<String, Double> ssValues,
                          Map<String, Double> lsValues) {
        
        // Clean up old entries if cache is getting large
        if (cache.size() >= MAX_CACHE_SIZE) {
            cleanupOldEntries();
        }
        
        CacheKey key = new CacheKey(timeframe, price, mode);
        CachedResults results = new CachedResults(mValues, atrValues, eValues, tpValues, 
                                                 thValues, ssValues, lsValues);
        cache.put(key, results);
        
        AdvancedLogger.debug("SmartCache", "put", "Cached results for %s|%.5f|%s (size: %d)", 
                           timeframe, price, mode, cache.size());
    }
    
    /**
     * Clean up expired entries
     */
    private static void cleanupOldEntries() {
        cache.entrySet().removeIf(entry -> !entry.getValue().isValid());
        
        // If still too large, remove oldest entries
        if (cache.size() >= MAX_CACHE_SIZE) {
            int toRemove = cache.size() - MAX_CACHE_SIZE + 10;
            cache.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().createdAt, e2.getValue().createdAt))
                .limit(toRemove)
                .forEach(entry -> cache.remove(entry.getKey()));
        }
    }
    
    /**
     * Clear entire cache
     */
    public static void clear() {
        cache.clear();
        AdvancedLogger.info("SmartCache", "clear", "Cache cleared");
    }
    
    /**
     * Get cache statistics
     */
    public static String getStats() {
        double hitRate = cacheHits + cacheMisses > 0 ? 
            (double) cacheHits / (cacheHits + cacheMisses) * 100 : 0;
        
        return String.format("Cache: %d entries, %.1f%% hit rate (%d hits, %d misses)",
                           cache.size(), hitRate, cacheHits, cacheMisses);
    }
    
    /**
     * Reset statistics
     */
    public static void resetStats() {
        cacheHits = 0;
        cacheMisses = 0;
    }
}
