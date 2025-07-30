package com.biotak.util;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Instrument;

/**
 * Optimized calculation methods to reduce computational overhead
 */
public final class OptimizedCalculations {
    
    // Cache for frequently used mathematical operations
    private static final double[] SQRT_CACHE = new double[1000];
    private static final boolean[] SQRT_CACHE_VALID = new boolean[1000];
    
    static {
        // Pre-calculate common square roots
        for (int i = 1; i < 100; i++) {
            SQRT_CACHE[i] = Math.sqrt(i);
            SQRT_CACHE_VALID[i] = true;
        }
    }
    
    private OptimizedCalculations() {}
    
    /**
     * Optimized square root calculation with caching for small integers
     */
    public static double fastSqrt(double value) {
        if (value >= 0 && value < SQRT_CACHE.length && value == (int)value) {
            int intValue = (int)value;
            if (SQRT_CACHE_VALID[intValue]) {
                return SQRT_CACHE[intValue];
            }
        }
        return Math.sqrt(value);
    }
    
    /**
     * Fast ATR calculation with optimized loop
     */
    public static double calculateATROptimized(DataSeries series, int period) {
        int size = series.size();
        if (size <= period) {
            return 0.0;
        }
        
        // Use incremental calculation instead of full recalculation
        double sumTR = 0;
        int startIndex = size - period;
        
        for (int i = startIndex; i < size; i++) {
            double high = series.getHigh(i);
            double low = series.getLow(i);
            double prevClose = (i > 0) ? series.getClose(i-1) : series.getOpen(i);
            
            // Optimized True Range calculation
            double hl = high - low;
            double hc = Math.abs(high - prevClose);
            double lc = Math.abs(low - prevClose);
            
            // Use conditional assignment instead of Math.max for better performance
            double tr = hl;
            if (hc > tr) tr = hc;
            if (lc > tr) tr = lc;
            
            sumTR += tr;
        }
        
        return sumTR / period;
    }
    
    /**
     * Optimized pip conversion with pre-calculated multipliers
     */
    public static double convertToPipsOptimized(double priceValue, Instrument instrument) {
        // Use cached pip multiplier
        double multiplier = UnitConverter.getPipMultiplier(instrument);
        return priceValue * multiplier;
    }
    
    /**
     * Fast percentage calculation for timeframes using improved logarithmic interpolation
     */
    public static double calculateTimeframePercentageOptimized(int totalMinutes) {
        if (totalMinutes <= 0) totalMinutes = 1;
        
        // Use the improved TimeframeUtil method which handles logarithmic interpolation
        return TimeframeUtil.getTimeframePercentageFromMinutes(totalMinutes);
    }
    
    /**
     * Calculates the step size in points for the TH levels.
     * This is the main TH calculation method used throughout the application.
     */
    public static double calculateTHPoints(Instrument instrument, double price, double percentage) {
        return calculateTHOptimized(instrument, price, percentage);
    }
    
    /**
     * Optimized TH calculation with reduced precision for performance
     */
    public static double calculateTHOptimized(Instrument instrument, double basePrice, double percentage) {
        if (basePrice <= 0 || percentage <= 0) return 0;
        
        double tickSize = instrument.getTickSize();
        if (tickSize <= 0) return 0;
        
        // Simplified calculation without complex digit handling
        double thStepPriceUnits = (basePrice * percentage) / 100.0;
        return thStepPriceUnits / tickSize;
    }
    
    /**
     * Batch calculation for multiple levels to reduce overhead
     */
    public static double[] calculateMultipleLevels(double basePrice, double stepSize, int count) {
        double[] levels = PoolManager.getDoubleArray(count);
        
        try {
            for (int i = 0; i < count; i++) {
                levels[i] = basePrice + (stepSize * (i + 1));
            }
            
            // Return a copy to avoid pool interference
            double[] result = new double[count];
            System.arraycopy(levels, 0, result, 0, count);
            return result;
        } finally {
            PoolManager.releaseDoubleArray(levels);
        }
    }
    
    /**
     * Fast min/max calculation for price ranges
     */
    public static double[] findMinMaxOptimized(DataSeries series, int startIndex, int endIndex) {
        if (startIndex >= endIndex || startIndex < 0 || endIndex > series.size()) {
            return new double[]{0, 0};
        }
        
        double min = series.getLow(startIndex);
        double max = series.getHigh(startIndex);
        
        // Unroll loop for better performance
        int i = startIndex + 1;
        for (; i + 3 < endIndex; i += 4) {
            // Process 4 elements at once
            double low1 = series.getLow(i);
            double high1 = series.getHigh(i);
            double low2 = series.getLow(i + 1);
            double high2 = series.getHigh(i + 1);
            double low3 = series.getLow(i + 2);
            double high3 = series.getHigh(i + 2);
            double low4 = series.getLow(i + 3);
            double high4 = series.getHigh(i + 3);
            
            if (low1 < min) min = low1;
            if (high1 > max) max = high1;
            if (low2 < min) min = low2;
            if (high2 > max) max = high2;
            if (low3 < min) min = low3;
            if (high3 > max) max = high3;
            if (low4 < min) min = low4;
            if (high4 > max) max = high4;
        }
        
        // Handle remaining elements
        for (; i < endIndex; i++) {
            double low = series.getLow(i);
            double high = series.getHigh(i);
            if (low < min) min = low;
            if (high > max) max = high;
        }
        
        return new double[]{max, min};
    }
}