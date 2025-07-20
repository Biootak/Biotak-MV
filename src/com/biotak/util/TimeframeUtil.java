package com.biotak.util;

import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.Enums;

import static com.biotak.util.Constants.FRACTAL_PERCENTAGES;
import java.util.Map;
import java.util.TreeMap;
import com.biotak.util.Logger;
import java.util.HashMap;

/**
 * Utility class for timeframe-related calculations.
 * Handles both standard fractal timeframes (powers of 2) and powers of 3 timeframes.
 */
public final class TimeframeUtil {

    // Store seconds-based fractal timeframes for special cases
    private static final TreeMap<Integer, String> FRACTAL_SECONDS_MAP = new TreeMap<>();
    static {
        FRACTAL_SECONDS_MAP.put(1, "S1");     // 1 second
        FRACTAL_SECONDS_MAP.put(4, "S4");     // 4 seconds
        FRACTAL_SECONDS_MAP.put(8, "S8");     // 8 seconds - for 2min trigger
        FRACTAL_SECONDS_MAP.put(11, "S11");   // 11 seconds - for 3min trigger
        FRACTAL_SECONDS_MAP.put(15, "S15");   // 15 seconds - for 4min trigger
        FRACTAL_SECONDS_MAP.put(16, "S16");   // 16 seconds - special fractal timeframe (1%)
        FRACTAL_SECONDS_MAP.put(30, "S30");   // 30 seconds - for 2min pattern
        FRACTAL_SECONDS_MAP.put(45, "S45");   // 45 seconds - for 3min pattern
    }
    
    // Store fractal timeframes in minutes for interpolation (powers of 2)
    private static final TreeMap<Integer, String> FRACTAL_MINUTES_MAP = new TreeMap<>();
    static {
        FRACTAL_MINUTES_MAP.put(1, "M1");           // 2^0 = 1 minute
        FRACTAL_MINUTES_MAP.put(4, "M4");           // 2^2 = 4 minutes
        FRACTAL_MINUTES_MAP.put(16, "M16");         // 2^4 = 16 minutes
        FRACTAL_MINUTES_MAP.put(64, "H1+M4");       // 2^6 = ~1 hour
        FRACTAL_MINUTES_MAP.put(256, "H4+M16");     // 2^8 = ~4 hours
        FRACTAL_MINUTES_MAP.put(1024, "H17+M4");    // 2^10 = ~17 hours
        FRACTAL_MINUTES_MAP.put(4096, "D2+H20+M16");// 2^12 = ~2.8 days
        FRACTAL_MINUTES_MAP.put(16384, "D11+H9+M4");// 2^14 = ~11.4 days
        FRACTAL_MINUTES_MAP.put(65536, "D45+H12+M16");// 2^16 = ~45.5 days
        FRACTAL_MINUTES_MAP.put(262144, "D182+H2+M4");// 2^18 = ~182 days
        FRACTAL_MINUTES_MAP.put(1048576, "D365+H1");// 2^20 = ~365 days
        FRACTAL_MINUTES_MAP.put(4194304, "D730+H2");// 2^22 = ~730 days
    }
    
    // Store powers of 3 timeframes in minutes
    private static final TreeMap<Integer, String> POWER3_MINUTES_MAP = new TreeMap<>();
    static {
        POWER3_MINUTES_MAP.put(1, "M1");            // 3^0 = 1 minute
        POWER3_MINUTES_MAP.put(3, "M3");            // 3^1 = 3 minutes
        POWER3_MINUTES_MAP.put(9, "M9");            // 3^2 = 9 minutes
        POWER3_MINUTES_MAP.put(27, "M27");          // 3^3 = 27 minutes
        POWER3_MINUTES_MAP.put(81, "H1+M21");       // 3^4 = 81 minutes
        POWER3_MINUTES_MAP.put(243, "H4+M3");       // 3^5 = 243 minutes
        POWER3_MINUTES_MAP.put(729, "H12+M9");      // 3^6 = 729 minutes
        POWER3_MINUTES_MAP.put(2187, "D1+H12+M27"); // 3^7 = 2187 minutes
        POWER3_MINUTES_MAP.put(6561, "D4+H13+M21"); // 3^8 = 6561 minutes
    }

    // Enhanced standard timeframe map - ordered by minutes for easy lookup
    private static final TreeMap<Integer, String> STANDARD_TIMEFRAMES_MAP = new TreeMap<>();
    static {
        STANDARD_TIMEFRAMES_MAP.put(1, "M1");       // 1 minute
        STANDARD_TIMEFRAMES_MAP.put(5, "M5");       // 5 minutes
        STANDARD_TIMEFRAMES_MAP.put(15, "M15");     // 15 minutes
        STANDARD_TIMEFRAMES_MAP.put(30, "M30");     // 30 minutes
        STANDARD_TIMEFRAMES_MAP.put(60, "H1");      // 1 hour
        STANDARD_TIMEFRAMES_MAP.put(240, "H4");     // 4 hours
        STANDARD_TIMEFRAMES_MAP.put(1440, "D1");    // 1 day
        STANDARD_TIMEFRAMES_MAP.put(10080, "W1");   // 1 week
        STANDARD_TIMEFRAMES_MAP.put(43200, "MN");   // 1 month (approx)
    }

    // Store standard ATR periods for common timeframes
    private static final Map<String, Integer> STANDARD_ATR_PERIODS = new HashMap<>();
    static {
        STANDARD_ATR_PERIODS.put("S1", 8);     // 1 second
        STANDARD_ATR_PERIODS.put("S4", 10);    // 4 seconds
        STANDARD_ATR_PERIODS.put("S16", 12);   // 16 seconds
        STANDARD_ATR_PERIODS.put("S30", 16);   // 30 seconds
        STANDARD_ATR_PERIODS.put("S45", 18);   // 45 seconds
        STANDARD_ATR_PERIODS.put("M1", 24);    // 1 minute
        STANDARD_ATR_PERIODS.put("M3", 24);    // 3 minutes
        STANDARD_ATR_PERIODS.put("M5", 24);    // 5 minutes
        STANDARD_ATR_PERIODS.put("M15", 24);   // 15 minutes
        STANDARD_ATR_PERIODS.put("M30", 24);   // 30 minutes
        STANDARD_ATR_PERIODS.put("H1", 24);    // 1 hour
        STANDARD_ATR_PERIODS.put("H4", 30);    // 4 hours
        STANDARD_ATR_PERIODS.put("D1", 22);    // 1 day
        STANDARD_ATR_PERIODS.put("W1", 52);    // 1 week
        STANDARD_ATR_PERIODS.put("MN", 12);    // 1 month
    }

    // Store exact mappings between standard timeframes for pattern and trigger levels
    // We'll use these only as fallbacks when mathematical calculation doesn't work
    private static final Map<String, String> PATTERN_TIMEFRAME_MAP = Map.of(
        "MN", "D1",   // Monthly -> Daily
        "W1", "H4",   // Weekly -> 4 Hour
        "D1", "H1",   // Daily -> 1 Hour
        "H4", "H1",   // 4 Hour -> 1 Hour
        "H1", "M15",  // 1 Hour -> 15 Min
        "M30", "M5",  // 30 Min -> 5 Min
        "M15", "M4",  // 15 Min -> 4 Min (more mathematically accurate)
        "M5", "M1",   // 5 Min -> 1 Min
        "M1", "S15"   // 1 Min -> 15 seconds
    );

    private static final Map<String, String> TRIGGER_TIMEFRAME_MAP = Map.of(
        "MN", "H4",    // Monthly -> 4 Hour
        "W1", "H1",    // Weekly -> 1 Hour
        "D1", "M15",   // Daily -> 15 Min
        "H4", "M15",   // 4 Hour -> 15 Min
        "H1", "M5",    // 1 Hour -> 5 Min
        "M30", "M1",   // 30 Min -> 1 Min
        "M15", "M1",   // 15 Min -> 1 Min
        "M5", "M1",    // 5 Min -> 1 Min
        "M1", "S4"     // 1 Min -> 4 seconds
    );

    // Fallback pattern mappings for minutes to seconds when under 4 minutes
    private static final Map<Integer, Integer> MINUTES_TO_SECONDS_PATTERN_MAP = Map.of(
        1, 15,  // 1 minute -> 15 seconds
        2, 30,  // 2 minutes -> 30 seconds
        3, 45,  // 3 minutes -> 45 seconds
        4, 60   // 4 minutes -> 60 seconds
    );

    // Fallback trigger mappings for minutes to seconds when under 16 minutes
    private static final Map<Integer, Integer> MINUTES_TO_SECONDS_TRIGGER_MAP = Map.of(
        1, 4,   // 1 minute -> 4 seconds
        2, 7,   // 2 minutes -> 7 seconds (approximately 1/16)
        3, 11,  // 3 minutes -> 11 seconds (approximately 1/16)
        4, 15,  // 4 minutes -> 15 seconds
        5, 20,  // 5 minutes -> 20 seconds (approximately 1/16)
        10, 40  // 10 minutes -> 40 seconds (approximately 1/16)
    );

    private TimeframeUtil() {
        // Private constructor to prevent instantiation
    }

    /* ------------------------------------------------------------------
     *  PUBLIC HELPERS (exposed for Ruler matching and other utilities)
     * ------------------------------------------------------------------ */

    /**
     * Unmodifiable view of the underlying power-of-2 fractal minute map.
     * Key = minutes, Value = formatted timeframe label (e.g. "M4", "H4+M16").
     */
    public static Map<Integer, String> getFractalMinutesMap() {
        return java.util.Collections.unmodifiableMap(FRACTAL_MINUTES_MAP);
    }

    /**
     * Unmodifiable view of the underlying power-of-3 fractal minute map.
     * Key = minutes, Value = formatted timeframe label (e.g. "M9", "H1+M21").
     */
    public static Map<Integer, String> getPower3MinutesMap() {
        return java.util.Collections.unmodifiableMap(POWER3_MINUTES_MAP);
    }

    /**
     * Convenience wrapper to obtain the timeframe percentage directly from a raw minute value.
     * Internally constructs a minute-based BarSize and delegates to {@link #getTimeframePercentage(BarSize)}.
     *
     * @param minutes total minutes (must be >0)
     * @return computed percentage for the given timeframe length
     */
    public static double getTimeframePercentage(int minutes) {
        if (minutes <= 0) minutes = 1;
        com.motivewave.platform.sdk.common.BarSize bs = com.motivewave.platform.sdk.common.BarSize.getBarSize(minutes);
        return getTimeframePercentage(bs);
    }

    /**
     * Gets the corresponding percentage for a given bar size based on a predefined fractal mapping.
     * For non-fractal timeframes, it interpolates between the closest fractal timeframes.
     * This works for both powers of 2 and powers of 3 timeframes using logarithmic interpolation.
     * 
     * @param barSize The bar size from the data series.
     * @return The percentage value for the timeframe.
     */
    public static double getTimeframePercentage(BarSize barSize) {
        // Special case for seconds-based timeframes
        if (barSize.getIntervalType() == Enums.IntervalType.SECOND) {
            int seconds = barSize.getInterval();
            
            // Special case for S16 only
            if (seconds == 16) {
                return FRACTAL_PERCENTAGES.getOrDefault("S16", 0.01);
            }
            
            // For other seconds-based timeframes, convert to minutes and use the standard calculation
            double minutesEquivalent = seconds / 60.0;
            // Use the same logarithmic calculation as for minutes
            double log2Minutes = Math.log(minutesEquivalent) / Math.log(2);
            return 0.02 * Math.pow(2, log2Minutes/2);
        }
        
        int totalMinutes = getTotalMinutes(barSize);
        
        // Handle special seconds-based timeframes (marked with negative values)
        if (totalMinutes < 0) {
            int seconds = Math.abs(totalMinutes);
            
            // Special case for S16 only
            if (seconds == 16) {
                return FRACTAL_PERCENTAGES.getOrDefault("S16", 0.01);
            }
            
            // For other seconds-based timeframes, convert to minutes and use the standard calculation
            double minutesEquivalent = seconds / 60.0;
            // Use the same logarithmic calculation as for minutes
            double log2Minutes = Math.log(minutesEquivalent) / Math.log(2);
            return 0.02 * Math.pow(2, log2Minutes/2);
        }
        
        // First check if this is an exact power of 2 fractal timeframe
        if (FRACTAL_MINUTES_MAP.containsKey(totalMinutes)) {
            String fractalTf = FRACTAL_MINUTES_MAP.get(totalMinutes);
            return FRACTAL_PERCENTAGES.getOrDefault(fractalTf, 0.32);
        }
        
        // Check if it's a power of 3 timeframe
        if (POWER3_MINUTES_MAP.containsKey(totalMinutes)) {
            // For powers of 3, we use the mathematical relationship:
            // Each 3x increase in time corresponds to sqrt(2) increase in percentage
            // log₃(minutes) gives us the power, which we multiply by log₂(sqrt(2)) to get the equivalent power of 2
            double power3 = Math.log(totalMinutes) / Math.log(3);
            double equivalentPower2 = power3 * (Math.log(Math.sqrt(2)) / Math.log(2));
            return 0.02 * Math.pow(2, equivalentPower2); // 0.02 is base percentage for M1
        }
        
        // For all other non-standard timeframes, find the closest fractal timeframes
        // First try with powers of 2
        Map.Entry<Integer, String> lowerEntry = FRACTAL_MINUTES_MAP.floorEntry(totalMinutes);
        Map.Entry<Integer, String> higherEntry = FRACTAL_MINUTES_MAP.ceilingEntry(totalMinutes);
        
        // If we have both bounds, use logarithmic interpolation
        if (lowerEntry != null && higherEntry != null) {
            double lowerPercentage = FRACTAL_PERCENTAGES.get(lowerEntry.getValue());
            double higherPercentage = FRACTAL_PERCENTAGES.get(higherEntry.getValue());
            
            // Use logarithmic interpolation since fractal timeframes follow a power pattern
            double logLower = Math.log(lowerEntry.getKey());
            double logHigher = Math.log(higherEntry.getKey());
            double logCurrent = Math.log(totalMinutes);
            
            double factor = (logCurrent - logLower) / (logHigher - logLower);
            double logLowerPercentage = Math.log(lowerPercentage);
            double logHigherPercentage = Math.log(higherPercentage);
            double interpolatedLogPercentage = logLowerPercentage + factor * (logHigherPercentage - logLowerPercentage);
            
            return Math.exp(interpolatedLogPercentage);
        }
        
        // If we don't have both bounds, use direct calculation based on the mathematical relationship:
        // percentage = base_percentage * 2^(log₂(minutes)/2)
        // This follows the pattern that 4x time = 2x percentage (square root relationship)
        double log2Minutes = Math.log(totalMinutes) / Math.log(2);
        return 0.02 * Math.pow(2, log2Minutes/2); // 0.02 is base percentage for M1
    }

    /**
     * Gets the appropriate ATR period for a given bar size based on the fractal relationship.
     * ATR periods also follow a pattern where 4x time approximately equals 2x ATR period.
     * 
     * @param barSize The bar size from the data series.
     * @return The appropriate ATR period for the timeframe.
     */
    public static int getAtrPeriod(BarSize barSize) {
        // Get standard string representation like "M1", "H4"
        String standardFormat = getStandardTimeframeString(barSize);
        
        // If it's a standard timeframe, return the predefined period
        if (STANDARD_ATR_PERIODS.containsKey(standardFormat)) {
            return STANDARD_ATR_PERIODS.get(standardFormat);
        }
        
        // For non-standard timeframes, use the fractal relationship
        int totalMinutes = getTotalMinutes(barSize);
        
        // Use the mathematical relationship: ATR period ≈ base_period * log₂(minutes)^(1/3)
        // This provides a smoother curve than direct doubling, which matches empirical observations
        double log2Minutes = Math.log(totalMinutes) / Math.log(2);
        int baseAtrPeriod = 24; // Base period for M1
        int calculatedPeriod = (int)(baseAtrPeriod * Math.pow(log2Minutes, 1/3.0));
        
        // Ensure reasonable bounds
        return Math.max(12, Math.min(52, calculatedPeriod));
    }

    /**
     * Converts a BarSize to a standard timeframe string representation (e.g., "M1", "H4", "D1").
     * For non-standard timeframes, it returns the exact specification (e.g., "M3", "M17").
     * For seconds-based timeframes, it returns "S" followed by the number of seconds.
     * 
     * @param barSize The bar size to convert
     * @return The string representation of the timeframe
     */
    public static String getStandardTimeframeString(BarSize barSize) {
        int interval = barSize.getInterval();
        Enums.IntervalType type = barSize.getIntervalType();
        
        // Handle seconds specially to ensure we always use our standard "S" format
        if (type == Enums.IntervalType.SECOND) {
            return "S" + interval;
        }
        
        switch (type) {
            case TICK:
                return "TICK";
            case MINUTE:
                return "M" + interval;
            case HOUR:
                return "H" + interval;
            case DAY:
                return "D" + interval;
            case WEEK:
                return "W" + interval;
            case MONTH:
                return "MN";
            case YEAR:
                return "Y" + interval;
            default:
                return barSize.toString();
        }
    }

    /**
     * Gets the pattern timeframe string representation (one fractal level down)
     * for the given timeframe, using precise fractal relationships.
     * 
     * For any timeframe (fractal or non-fractal), the pattern timeframe is 
     * exactly 1/4 of the current timeframe. This follows the principle that 
     * quadrupling the timeframe doubles the level in fractal structure.
     * 
     * @param barSize The original bar size
     * @return A string representation of the pattern timeframe
     */
    public static String getPatternTimeframeString(BarSize barSize) {
        // Special case for seconds-based timeframes
        if (barSize.getIntervalType() == Enums.IntervalType.SECOND) {
            int seconds = barSize.getInterval();
            // Calculate pattern timeframe (1/4 of current seconds)
            int patternSeconds = Math.max(1, seconds / 4);
            // If pattern seconds would be less than 1, use 1 second
            if (patternSeconds < 1) patternSeconds = 1;
            return "S" + patternSeconds;
        }
        
        String standardFormat = getStandardTimeframeString(barSize);
        
        // Check if we have a direct mapping for this standard timeframe
        if (PATTERN_TIMEFRAME_MAP.containsKey(standardFormat)) {
            return PATTERN_TIMEFRAME_MAP.get(standardFormat);
        }
        
        // For all timeframes (standard, fractal powers of 2, powers of 3, and others),
        // the pattern timeframe is exactly 1/4 of the current timeframe
        int totalMinutes = getTotalMinutes(barSize);
        
        // Handle special seconds-based timeframes (marked with negative values)
        if (totalMinutes < 0) {
            // Convert negative value back to seconds
            int seconds = Math.abs(totalMinutes);
            // Calculate pattern timeframe (1/4 of current seconds)
            int patternSeconds = Math.max(1, seconds / 4);
            return "S" + patternSeconds;
        }
        
        // Pattern is at the timeframe that is exactly 1/4 of the current timeframe
        // For example: M20 -> M5, M3 -> 45 seconds
        if (totalMinutes < 4) {
            // For timeframes smaller than 4 minutes, we need to calculate seconds
            int totalSeconds = totalMinutes * 60;
            int patternSeconds = totalSeconds / 4;
            
            // Return seconds-based timeframe for small timeframes
            if (patternSeconds < 60) {
                return "S" + patternSeconds;
            } else {
                return "M" + (patternSeconds / 60);
            }
        } else {
            int patternMinutes = Math.max(1, totalMinutes / 4);
            
            // Try to find an exact match in our known timeframes first
            if (STANDARD_TIMEFRAMES_MAP.containsKey(patternMinutes)) {
                return STANDARD_TIMEFRAMES_MAP.get(patternMinutes);
            }

            if (FRACTAL_MINUTES_MAP.containsKey(patternMinutes)) {
                return FRACTAL_MINUTES_MAP.get(patternMinutes);
            }
            
            if (POWER3_MINUTES_MAP.containsKey(patternMinutes)) {
                return POWER3_MINUTES_MAP.get(patternMinutes);
            }
            
            // For non-standard timeframes, return the exact value like "M5" or "M11"
            if (patternMinutes < 60) {
                return "M" + patternMinutes;
            } else {
                // Convert directly to timeframe string (e.g., H1+M15)
                return getTimeframeString(patternMinutes);
            }
        }
    }
    
    /**
     * Gets the trigger timeframe string representation (two fractal levels down)
     * for the given timeframe, using precise fractal relationships.
     * 
     * For any timeframe (fractal or non-fractal), the trigger timeframe is 
     * exactly 1/16 of the current timeframe. This follows the principle that 
     * each fractal level is 1/4 of the previous level, and trigger is two levels down.
     * 
     * @param barSize The original bar size
     * @return A string representation of the trigger timeframe
     */
    public static String getTriggerTimeframeString(BarSize barSize) {
        // Special case for seconds-based timeframes
        if (barSize.getIntervalType() == Enums.IntervalType.SECOND) {
            int seconds = barSize.getInterval();
            // Calculate trigger timeframe (1/16 of current seconds)
            int triggerSeconds = Math.max(1, seconds / 16);
            // If trigger seconds would be less than 1, use 1 second
            if (triggerSeconds < 1) triggerSeconds = 1;
            return "S" + triggerSeconds;
        }
        
        String standardFormat = getStandardTimeframeString(barSize);
        
        // Check if we have a direct mapping for standard timeframes
        if (TRIGGER_TIMEFRAME_MAP.containsKey(standardFormat)) {
            return TRIGGER_TIMEFRAME_MAP.get(standardFormat);
        }
        
        // For all timeframes (standard, fractal powers of 2, powers of 3, and others),
        // the trigger timeframe is exactly 1/16 of the current timeframe (two fractal levels down)
        int totalMinutes = getTotalMinutes(barSize);
        
        // Handle special seconds-based timeframes (marked with negative values)
        if (totalMinutes < 0) {
            // Convert negative value back to seconds
            int seconds = Math.abs(totalMinutes);
            // Calculate trigger timeframe (1/16 of current seconds)
            int triggerSeconds = Math.max(1, seconds / 16);
            return "S" + triggerSeconds;
        }
        
        // For timeframes smaller than 16 minutes, we need to calculate seconds to be more precise
        if (totalMinutes < 16) {
            int totalSeconds = totalMinutes * 60;
            int triggerSeconds = Math.max(1, totalSeconds / 16);
            
            // Return seconds-based timeframe for small timeframes
            if (triggerSeconds < 60) {
                return "S" + triggerSeconds;
            } else {
                return "M" + (triggerSeconds / 60);
            }
        } else {
            int triggerMinutes = Math.max(1, totalMinutes / 16);
            
            // Try to find an exact match in our known timeframes first
            if (STANDARD_TIMEFRAMES_MAP.containsKey(triggerMinutes)) {
                return STANDARD_TIMEFRAMES_MAP.get(triggerMinutes);
            }
            
            if (FRACTAL_MINUTES_MAP.containsKey(triggerMinutes)) {
                return FRACTAL_MINUTES_MAP.get(triggerMinutes);
            }
            
            if (POWER3_MINUTES_MAP.containsKey(triggerMinutes)) {
                return POWER3_MINUTES_MAP.get(triggerMinutes);
            }
            
            // For non-standard timeframes, return the exact value like "M3" or "M7"
            if (triggerMinutes < 60) {
                return "M" + triggerMinutes;
            } else {
                // Convert directly to timeframe string (e.g., H1+M15)
                return getTimeframeString(triggerMinutes);
            }
        }
    }
    
    /**
     * Gets the relative position of a non-fractal timeframe between its neighboring fractal timeframes.
     * This helps understand where a timeframe like M20 sits between M16 and M64.
     * 
     * @param barSize The bar size to analyze
     * @return A value between 0.0 and 1.0 indicating the position between lower and higher fractal timeframes
     */
    public static double getNonFractalPosition(BarSize barSize) {
        int totalMinutes = getTotalMinutes(barSize);
        
        // If it's already a fractal timeframe, return 0.0 (exact match with lower)
        if (FRACTAL_MINUTES_MAP.containsKey(totalMinutes) || POWER3_MINUTES_MAP.containsKey(totalMinutes)) {
            return 0.0;
        }
        
        // Find neighboring fractal timeframes (powers of 2)
        Map.Entry<Integer, String> lowerEntry = FRACTAL_MINUTES_MAP.floorEntry(totalMinutes);
        Map.Entry<Integer, String> higherEntry = FRACTAL_MINUTES_MAP.ceilingEntry(totalMinutes);
        
        if (lowerEntry != null && higherEntry != null) {
            // Calculate position using logarithmic scale (more appropriate for fractal timeframes)
            double logLower = Math.log(lowerEntry.getKey());
            double logHigher = Math.log(higherEntry.getKey());
            double logCurrent = Math.log(totalMinutes);
            
            return (logCurrent - logLower) / (logHigher - logLower);
        }
        
        // If we don't have both bounds, check if it's close to powers of 3
        Map.Entry<Integer, String> lowerEntry3 = POWER3_MINUTES_MAP.floorEntry(totalMinutes);
        Map.Entry<Integer, String> higherEntry3 = POWER3_MINUTES_MAP.ceilingEntry(totalMinutes);
        
        if (lowerEntry3 != null && higherEntry3 != null) {
            // Calculate position using logarithmic scale
            double logLower = Math.log(lowerEntry3.getKey());
            double logHigher = Math.log(higherEntry3.getKey());
            double logCurrent = Math.log(totalMinutes);
            
            return (logCurrent - logLower) / (logHigher - logLower);
        }
        
        // If all else fails, return 0.5 (middle position)
        return 0.5;
    }
    
    /**
     * Checks if a number is a power of 2 or very close to it
     */
    private static boolean isPowerOf2(int number) {
        double log2 = Math.log(number) / Math.log(2);
        double roundedLog2 = Math.round(log2);
        
        // Check if log2 is very close to an integer
        return Math.abs(log2 - roundedLog2) < 0.05;
    }
    
    /**
     * Checks if a number is a power of 3 or very close to it
     */
    private static boolean isPowerOf3(int number) {
        double log3 = Math.log(number) / Math.log(3);
        double roundedLog3 = Math.round(log3);
        
        // Check if log3 is very close to an integer
        return Math.abs(log3 - roundedLog3) < 0.05;
    }
    
    /**
     * Checks if a number is close to any exact timeframe (standard, power of 2, or power of 3)
     */
    private static boolean isCloseToExactTimeframe(int minutes) {
        // Check standard timeframes
        for (int standardMinutes : STANDARD_TIMEFRAMES_MAP.keySet()) {
            if (Math.abs(minutes - standardMinutes) <= 0.1 * standardMinutes) {
                return true;
            }
        }
        
        // Check power of 2 timeframes
        for (int powerMinutes : FRACTAL_MINUTES_MAP.keySet()) {
            if (Math.abs(minutes - powerMinutes) <= 0.1 * powerMinutes) {
                return true;
            }
        }
        
        // Check power of 3 timeframes
        for (int powerMinutes : POWER3_MINUTES_MAP.keySet()) {
            if (Math.abs(minutes - powerMinutes) <= 0.1 * powerMinutes) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Finds the closest timeframe (from standard, power of 2, or power of 3) to the given minutes
     */
    private static Map.Entry<Integer, String> findClosestTimeframe(int minutes) {
        // Try to find the closest standard timeframe
        Map.Entry<Integer, String> standardLower = STANDARD_TIMEFRAMES_MAP.floorEntry(minutes);
        Map.Entry<Integer, String> standardHigher = STANDARD_TIMEFRAMES_MAP.ceilingEntry(minutes);
        
        // Try to find the closest power of 2 timeframe
        Map.Entry<Integer, String> power2Lower = FRACTAL_MINUTES_MAP.floorEntry(minutes);
        Map.Entry<Integer, String> power2Higher = FRACTAL_MINUTES_MAP.ceilingEntry(minutes);
        
        // Try to find the closest power of 3 timeframe
        Map.Entry<Integer, String> power3Lower = POWER3_MINUTES_MAP.floorEntry(minutes);
        Map.Entry<Integer, String> power3Higher = POWER3_MINUTES_MAP.ceilingEntry(minutes);
        
        // Find the closest entry among all options
        TreeMap<Integer, Map.Entry<Integer, String>> distanceMap = new TreeMap<>();
        
        addIfNotNull(distanceMap, standardLower, minutes);
        addIfNotNull(distanceMap, standardHigher, minutes);
        addIfNotNull(distanceMap, power2Lower, minutes);
        addIfNotNull(distanceMap, power2Higher, minutes);
        addIfNotNull(distanceMap, power3Lower, minutes);
        addIfNotNull(distanceMap, power3Higher, minutes);
        
        // Return the entry with the smallest distance
        if (!distanceMap.isEmpty()) {
            return distanceMap.firstEntry().getValue();
        }
        
        return null;
    }
    
    /**
     * Helper method to add an entry to the distance map if it's not null
     */
    private static void addIfNotNull(TreeMap<Integer, Map.Entry<Integer, String>> distanceMap, 
                                   Map.Entry<Integer, String> entry, int targetMinutes) {
        if (entry != null) {
            int distance = Math.abs(entry.getKey() - targetMinutes);
            distanceMap.put(distance, entry);
        }
    }
    
    /**
     * Converts minutes to a timeframe string
     */
    private static String getTimeframeString(int minutes) {
        if (minutes < 60) {
            return "M" + minutes;
        } else if (minutes < 1440) {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return "H" + hours;
            } else {
                return "H" + hours + "+M" + remainingMinutes;
            }
        } else {
            int days = minutes / 1440;
            int remainingMinutes = minutes % 1440;
            if (remainingMinutes == 0) {
                return "D" + days;
            } else {
                int hours = remainingMinutes / 60;
                int mins = remainingMinutes % 60;
                if (mins == 0) {
                    return "D" + days + "+H" + hours;
                } else {
                    return "D" + days + "+H" + hours + "+M" + mins;
                }
            }
        }
    }
    
    /**
     * Creates a BarSize object for the pattern timeframe (one fractal level down)
     * 
     * @param barSize The original bar size
     * @return The BarSize for the pattern timeframe
     */
    public static BarSize getPatternBarSize(BarSize barSize) {
        long totalSeconds = getTotalSeconds(barSize);
        long patternSeconds = Math.max(1, totalSeconds / 4);

        if (patternSeconds < 60) {
            return createBarSizeFromSeconds((int) patternSeconds);
        } else {
            long minutes = Math.max(1, patternSeconds / 60);
            return BarSize.getBarSize((int) minutes);
        }
    }

    /**
     * Creates a BarSize object for the trigger timeframe (two fractal levels down)
     * 
     * @param barSize The original bar size
     * @return The BarSize for the trigger timeframe
     */
    public static BarSize getTriggerBarSize(BarSize barSize) {
        long totalSeconds = getTotalSeconds(barSize);
        long triggerSeconds = Math.max(1, totalSeconds / 16);

        if (triggerSeconds < 60) {
            return createBarSizeFromSeconds((int) triggerSeconds);
        } else {
            long minutes = Math.max(1, triggerSeconds / 60);
            return BarSize.getBarSize((int) minutes);
        }
    }

    /**
     * Helper method to create a BarSize object from seconds.
     * Attempts to create a seconds-based BarSize using the best available method.
     * 
     * @param seconds The number of seconds
     * @return A BarSize object representing the specified seconds
     */
    private static BarSize createBarSizeFromSeconds(int seconds) {
        if (seconds <= 0) {
            seconds = 1; // Default to 1 second if input is invalid
        }
        // Use the static factory method instead of a direct constructor
        return BarSize.getBarSize(Enums.BarSizeType.LINEAR, Enums.IntervalType.SECOND, seconds);
    }
    
    /**
     * Converts a timeframe string to its equivalent in minutes
     * 
     * @param timeframeStr The timeframe string representation (e.g., "M5", "H1")
     * @return The number of minutes represented by this timeframe
     */
    private static int getTimeframeMinutes(String timeframeStr) {
        if (timeframeStr.startsWith("M")) {
            try {
                return Integer.parseInt(timeframeStr.substring(1));
            } catch (NumberFormatException e) {
                return 1;
            }
        } 
        else if (timeframeStr.startsWith("H")) {
            try {
                return Integer.parseInt(timeframeStr.substring(1)) * 60;
            } catch (NumberFormatException e) {
                return 60;
            }
        } 
        else if (timeframeStr.startsWith("D")) {
            try {
                return Integer.parseInt(timeframeStr.substring(1)) * 1440;
            } catch (NumberFormatException e) {
                return 1440;
            }
        }
        else if (timeframeStr.equals("W1")) {
            return 10080;
        }
        else if (timeframeStr.equals("MN")) {
            return 43200;
        }
        
        // Complex fractal timeframes like "H4+M16" - approximate by using first component
        if (timeframeStr.contains("+")) {
            String[] parts = timeframeStr.split("\\+");
            if (parts.length > 0) {
                return getTimeframeMinutes(parts[0]);
            }
        }
        
        return 1; // Default to M1
    }
    
    /**
     * Converts a BarSize to total minutes for easier timeframe comparison.
     * Special case: For seconds-based timeframes in our FRACTAL_SECONDS_MAP,
     * this method returns a negative value equal to the negative of the seconds.
     * This serves as a marker to identify seconds-based timeframes elsewhere in the code.
     * 
     * @param barSize The bar size to convert
     * @return The total minutes represented by this bar size, or a negative value for seconds-based timeframes
     */
    public static int getTotalMinutes(BarSize barSize) {
        // Special case for seconds-based timeframes
        if (barSize.getIntervalType() == Enums.IntervalType.SECOND) {
            int seconds = barSize.getInterval();
            // Check if this is one of our defined seconds-based timeframes
            if (FRACTAL_SECONDS_MAP.containsKey(seconds)) {
                // Return a negative value as a special marker for seconds-based timeframes
                return -seconds;
            }
        }
        
        int interval = barSize.getInterval();
        Enums.IntervalType type = barSize.getIntervalType();
        
        switch (type) {
            case TICK:
                return 1; // Treat tick as 1 minute
            case SECOND:
                return Math.max(1, interval / 60); // Convert seconds to minutes, minimum 1
            case MINUTE:
                return interval;
            case HOUR:
                return interval * 60;
            case DAY:
                return interval * 24 * 60;
            case WEEK:
                return interval * 7 * 24 * 60;
            case MONTH:
                return interval * 30 * 24 * 60; // Approximate month as 30 days
            case YEAR:
                return interval * 365 * 24 * 60; // Approximate year as 365 days
            default:
                return 240; // Default to 4 hours (240 minutes)
        }
    }

    /**
     * Converts a BarSize object to its total duration in seconds.
     *
     * @param barSize The BarSize object to convert.
     * @return The total number of seconds for the given BarSize.
     */
    public static long getTotalSeconds(BarSize barSize) {
        if (barSize == null) {
            return 1; // Default to 1 second for safety
        }

        long interval = barSize.getInterval();
        switch (barSize.getIntervalType()) {
            case SECOND:
                return interval;
            case MINUTE:
                return interval * 60;
            case HOUR:
                return interval * 60 * 60;
            case DAY:
                return interval * 24 * 60 * 60;
            case WEEK:
                return interval * 7 * 24 * 60 * 60;
            case MONTH:
                return interval * 30 * 24 * 60 * 60; // Approximation
            case YEAR:
                return interval * 365 * 24 * 60 * 60; // Approximation
            default:
                return 1;
        }
    }

    /**
     * Determines if a timeframe is seconds-based.
     * 
     * @param barSize The bar size to check
     * @return true if the timeframe is seconds-based, false otherwise
     */
    public static boolean isSecondsBasedTimeframe(BarSize barSize) {
        if (barSize.getIntervalType() == Enums.IntervalType.SECOND) {
            return true;
        }
        
        // Also check using the total minutes (negative value indicates seconds)
        int totalMinutes = getTotalMinutes(barSize);
        return totalMinutes < 0;
    }
    
    /**
     * Determines if a timeframe string represents a seconds-based timeframe.
     * 
     * @param timeframeStr The timeframe string to check (e.g., "S16", "16 sec")
     * @return true if the timeframe is seconds-based, false otherwise
     */
    public static boolean isSecondsBasedTimeframe(String timeframeStr) {
        if (timeframeStr == null) return false;
        
        // Check if it starts with 'S' followed by digits (our format)
        if (timeframeStr.startsWith("S") && timeframeStr.length() > 1) {
            try {
                Integer.parseInt(timeframeStr.substring(1));
                return true;
            } catch (NumberFormatException e) {
                // Not a number after 'S'
            }
        }
        
        // Check if it contains "sec" (MotiveWave format)
        return timeframeStr.contains("sec");
    }

    /**
     * Gets the bar size for the structure level (4x pattern or 16x current timeframe)
     * 
     * @param barSize The current bar size
     * @return The structure level bar size
     */
    public static BarSize getStructureBarSize(BarSize barSize) {
        // Special case for seconds-based timeframes
        if (barSize.getIntervalType() == Enums.IntervalType.SECOND) {
            int seconds = barSize.getInterval();
            // Calculate structure timeframe (16x of current seconds)
            int structureSeconds = seconds * 16;
            
            if (structureSeconds < 60) {
                // For seconds-based timeframes, use dedicated method
                return createBarSizeFromSeconds(structureSeconds);
            } else {
                // Convert to minutes
                return BarSize.getBarSize(structureSeconds / 60);
            }
        }
        
        // For standard timeframes, structure is 16x the current timeframe
        int totalMinutes = getTotalMinutes(barSize);
        
        // Handle special seconds-based timeframes (marked with negative values)
        if (totalMinutes < 0) {
            // Convert negative value back to seconds
            int seconds = Math.abs(totalMinutes);
            // Calculate structure timeframe (16x of current seconds)
            int structureSeconds = seconds * 16;
            
            if (structureSeconds < 60) {
                // For seconds-based timeframes, use dedicated method
                return createBarSizeFromSeconds(structureSeconds);
            } else {
                // Convert to minutes
                return BarSize.getBarSize(structureSeconds / 60);
            }
        }
        
        // For all other timeframes, structure is 16x the current timeframe in minutes
        int structureMinutes = totalMinutes * 16;
        return BarSize.getBarSize(structureMinutes);
    }

    // ------------------------------------------------------------------
    //  NEW PUBLIC UTILITY: Parse compound timeframe label to minutes
    // ------------------------------------------------------------------
    /**
     * Parses a compound timeframe string (e.g. "H1", "H4+M16", "6H52m", "MN") and returns
     * the total duration in minutes.
     * <p>
     * Supported units:
     * <ul>
     *   <li>S/s – seconds&nbsp;(rounded up to the nearest minute)</li>
     *   <li>M/m – minutes</li>
     *   <li>H/h – hours</li>
     *   <li>D/d – days</li>
     *   <li>W/w – weeks</li>
     *   <li>Special code "MN" – monthly (treated as 30&nbsp;days)</li>
     * </ul>
     * The method is tolerant of whitespace and '+' separators.
     * Examples:
     * <pre>
     *   parseCompoundTimeframe("H4")            == 240
     *   parseCompoundTimeframe("H1+M15")        == 75
     *   parseCompoundTimeframe("6H52m")         == 412
     *   parseCompoundTimeframe("MN")            == 43200
     * </pre>
     *
     * @param tf timeframe label to parse (not null)
     * @return total minutes or -1 if the input could not be parsed
     */
    public static int parseCompoundTimeframe(String tf) {
        if (tf == null || tf.isEmpty()) return -1;
        if (tf.equalsIgnoreCase("MN")) return 60 * 24 * 30; // treat month ≈ 30 days

        int minutes = 0;

        // Normalize by replacing '+' with space to ease regex processing
        String cleaned = tf.replace("+", " ");

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)([mMhHdDwWsS])|([mMhHdDwWsS])(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(cleaned);
        while (matcher.find()) {
            String numStr;
            char unit;
            if (matcher.group(1) != null) { // form: 56m / 4H / 2d … (digits first)
                numStr = matcher.group(1);
                unit   = Character.toUpperCase(matcher.group(2).charAt(0));
            } else {                       // form: m56 / H4 etc. (unit first)
                numStr = matcher.group(4);
                unit   = Character.toUpperCase(matcher.group(3).charAt(0));
            }
            if (numStr == null || numStr.isEmpty()) continue;
            try {
                int val = Integer.parseInt(numStr);
                switch (unit) {
                    case 'M' -> minutes += val;                    // minutes
                    case 'H' -> minutes += val * 60;               // hours
                    case 'D' -> minutes += val * 1440;             // days
                    case 'W' -> minutes += val * 10080;            // weeks
                    case 'S' -> minutes += Math.max(1, (int) Math.round(val / 60.0)); // seconds → ≈ minutes
                    default -> {}
                }
            } catch (NumberFormatException ignore) {
                // continue parsing other matches
            }
        }
        return minutes > 0 ? minutes : -1;
    }
} 