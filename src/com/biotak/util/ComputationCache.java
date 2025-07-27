package com.biotak.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Specialized cache for expensive computations to avoid repeated calculations
 */
public final class ComputationCache {
    
    // Cache for timeframe percentage calculations
    private static final Map<String, Double> percentageCache = new ConcurrentHashMap<>();
    
    // Cache for ATR period calculations
    private static final Map<String, Integer> atrPeriodCache = new ConcurrentHashMap<>();
    
    // Cache for pip multiplier calculations
    private static final Map<String, Double> pipMultiplierCache = new ConcurrentHashMap<>();
    
    // Cache size limits - optimized for better hit ratios
    private static final int MAX_CACHE_SIZE = 500;
    private static final int CLEANUP_THRESHOLD = 400;
    
    // Hit ratio tracking for monitoring
    private static volatile long hits = 0;
    private static volatile long misses = 0;
    
    private ComputationCache() {}
    
    /**
     * Get cached timeframe percentage or compute and cache it
     */
    public static Double getCachedPercentage(String barSizeKey) {
        Double result = percentageCache.get(barSizeKey);
        if (result != null) {
            hits++;
        } else {
            misses++;
        }
        return result;
    }
    
    /**
     * Cache a timeframe percentage calculation
     */
    public static void cachePercentage(String barSizeKey, double percentage) {
        percentageCache.put(barSizeKey, percentage);
        
        if (percentageCache.size() > CLEANUP_THRESHOLD) {
            cleanUp();
        }
    }
    
    /**
     * Get cached ATR period or compute and cache it
     */
    public static Integer getCachedAtrPeriod(String barSizeKey) {
        return atrPeriodCache.get(barSizeKey);
    }
    
    /**
     * Cache an ATR period calculation
     */
    public static void cacheAtrPeriod(String barSizeKey, int period) {
        if (atrPeriodCache.size() < MAX_CACHE_SIZE) {
            atrPeriodCache.put(barSizeKey, period);
        }
    }
    
    /**
     * Get cached pip multiplier or compute and cache it
     */
    public static Double getCachedPipMultiplier(String instrumentKey) {
        return pipMultiplierCache.get(instrumentKey);
    }
    
    /**
     * Cache a pip multiplier calculation
     */
    public static void cachePipMultiplier(String instrumentKey, double multiplier) {
        if (pipMultiplierCache.size() < MAX_CACHE_SIZE) {
            pipMultiplierCache.put(instrumentKey, multiplier);
        }
    }
    
    /**
     * Clear all computation caches
     */
    public static void clearAll() {
        percentageCache.clear();
        atrPeriodCache.clear();
        pipMultiplierCache.clear();
    }
    
    /**
     * Clean up least recently used entries when cache gets too large
     */
    private static void cleanUp() {
        if (percentageCache.size() > MAX_CACHE_SIZE) {
            // Simple cleanup - remove 25% of entries randomly
            int toRemove = percentageCache.size() / 4;
            percentageCache.entrySet().removeIf(entry -> Math.random() < 0.25 && toRemove > 0);
        }
    }
    
    /**
     * Get hit ratio as percentage
     */
    public static double getHitRatio() {
        long totalRequests = hits + misses;
        return totalRequests > 0 ? (double) hits / totalRequests * 100 : 0.0;
    }
    
    /**
     * Reset hit ratio counters
     */
    public static void resetStats() {
        hits = 0;
        misses = 0;
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public static String getCacheStats() {
        double hitRatio = getHitRatio();
        long totalRequests = hits + misses;
        return String.format("ComputationCache - Percentage: %d, ATR: %d, Pip: %d | Hits: %d, Misses: %d, Hit Ratio: %.1f%% (Total: %d)", 
                           percentageCache.size(), atrPeriodCache.size(), pipMultiplierCache.size(),
                           hits, misses, hitRatio, totalRequests);
    }
}
