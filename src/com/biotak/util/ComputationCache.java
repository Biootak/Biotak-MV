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
    
    // Cache size limits - increased for better hit ratios
    private static final int MAX_CACHE_SIZE = 200;
    
    private ComputationCache() {}
    
    /**
     * Get cached timeframe percentage or compute and cache it
     */
    public static Double getCachedPercentage(String barSizeKey) {
        return percentageCache.get(barSizeKey);
    }
    
    /**
     * Cache a timeframe percentage calculation
     */
    public static void cachePercentage(String barSizeKey, double percentage) {
        if (percentageCache.size() < MAX_CACHE_SIZE) {
            percentageCache.put(barSizeKey, percentage);
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
     * Get cache statistics for monitoring
     */
    public static String getCacheStats() {
        return String.format("ComputationCache - Percentage: %d, ATR: %d, Pip: %d", 
                           percentageCache.size(), atrPeriodCache.size(), pipMultiplierCache.size());
    }
}