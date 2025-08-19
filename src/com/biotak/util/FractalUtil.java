package com.biotak.util;

import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Instrument;
import com.motivewave.platform.sdk.common.Settings;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper utilities for fractal calculations used by the Biotak Trigger indicator.
 * <p>
 * تمام منطق محاسبات فراکتالی که قبلاً در چند نقطه پراکنده بود، این‌جا متمرکز شده است تا
 * نگه‌داری آسان‌تر شود و از تکرار کد جلوگیری گردد.
 */
public final class FractalUtil {

    // Cache names for different types of calculations
    private static final String TH_BUNDLE_CACHE = "th_bundle";
    private static final long CACHE_EXPIRY_MS = 60000; // 1 minute

    private FractalUtil() {}

    /**
     * Container for TH-مقادیر پنج سطح فراکتالی (Current, Pattern, Trigger, Structure, Higher-Pattern).
     */
    public record THBundle(double th, double pattern, double trigger,
                           double structure, double higherPattern) {}

    /**
     * Calculates TH values for پنج سطح فراکتالی اطراف {@code barSize} با استفاده از قیمت پایه.
     * Uses caching to improve performance for repeated calculations.
     */
    public static THBundle calculateTHBundle(Instrument instrument, BarSize barSize, double basePrice) {
        // Create cache key
        String cacheKey = instrument.getSymbol() + "_" + barSize.toString() + "_" + 
                         String.format("%.5f", basePrice);
        
        // Check cache first
        THBundle cached = CacheManager.get(TH_BUNDLE_CACHE, cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Calculate if not in cache or expired
        double tick = instrument.getTickSize();

        double th = calcTH(instrument, barSize, basePrice, tick);
        BarSize patternSize   = TimeframeUtil.getPatternBarSize(barSize);
        BarSize triggerSize   = TimeframeUtil.getTriggerBarSize(barSize);
        BarSize structureSize = TimeframeUtil.getStructureBarSize(barSize);
        BarSize higherPattern = TimeframeUtil.getPatternBarSize(structureSize);

        double patternTH      = calcTH(instrument, patternSize,   basePrice, tick);
        double triggerTH      = calcTH(instrument, triggerSize,   basePrice, tick);
        double structureTH    = calcTH(instrument, structureSize, basePrice, tick);
        double higherPatternTH= calcTH(instrument, higherPattern, basePrice, tick);

        THBundle result = new THBundle(th, patternTH, triggerTH, structureTH, higherPatternTH);
        
        // Cache the result
        CacheManager.put(TH_BUNDLE_CACHE, cacheKey, result, CACHE_EXPIRY_MS);
        
        return result;
    }

    /**
     * Calculates the historical high and low for the loaded {@link DataSeries}. This logic previously
     * تکراری در دو مکان (calculate و drawFigures) قرار داشت.
     * Optimized to avoid full series scan when cached values are available.
     *
     * @param series      داده‌های قیمت
     * @param settings    {@link com.motivewave.platform.sdk.common.desc.Settings} شیء مربوط به اندیکاتور
     * @param cachedHigh  بالاترین مقدار ذخیره‌شده تا این لحظه (می‌تواند {@link Double#NEGATIVE_INFINITY} باشد)
     * @param cachedLow   کمترین مقدار ذخیره‌شده تا این لحظه (می‌تواند {@link Double#POSITIVE_INFINITY} باشد)
     * @param manualMode  اگر فعال باشد اعداد دستی از تنظیمات خوانده می‌شود
     * @return آرایه‌ای با دو عنصر: index 0 → high , index 1 → low
     */
    public static double[] getHistoricalRange(DataSeries series, Settings settings,
                                              double cachedHigh, double cachedLow, boolean manualMode) {
        if (manualMode) {
            double manualHigh = settings.getDouble(com.biotak.config.SettingsRepository.S_MANUAL_HIGH, 0);
            double manualLow  = settings.getDouble(com.biotak.config.SettingsRepository.S_MANUAL_LOW, 0);
            return new double[]{manualHigh, manualLow};
        }

        // If we have valid cached values, only scan recent bars for updates
        boolean hasValidCache = cachedHigh != Double.NEGATIVE_INFINITY && 
                               cachedLow != Double.POSITIVE_INFINITY;
        
        if (hasValidCache) {
            // Only check the last few bars for new extremes
            int sz = series.size();
            int startIndex = Math.max(0, sz - 50); // Check last 50 bars only
            
            double recentHigh = cachedHigh;
            double recentLow = cachedLow;
            
            for (int i = startIndex; i < sz; i++) {
                recentHigh = Math.max(recentHigh, series.getHigh(i));
                recentLow = Math.min(recentLow, series.getLow(i));
            }
            
            return new double[]{recentHigh, recentLow};
        }
        
        // Full scan only when no cache is available - use optimized calculation
        int sz = series.size();
        
        // Limit full scan to reasonable number of bars to prevent performance issues
        int maxBarsToScan = Math.min(sz, 1000); // کاهش از 10k به 1k bars max
        int startIndex = Math.max(0, sz - maxBarsToScan);
        
        // Use optimized min/max calculation
        double[] minMax = OptimizedCalculations.findMinMaxOptimized(series, startIndex, sz);
        double computedHigh = minMax[0];
        double computedLow = minMax[1];

        return new double[]{computedHigh, computedLow};
    }
    
    /**
     * Incremental update method for extremes - more efficient than full recalculation
     */
    public static double[] updateExtremes(double currentHigh, double currentLow, 
                                         double newHigh, double newLow) {
        return new double[]{
            Math.max(currentHigh, newHigh),
            Math.min(currentLow, newLow)
        };
    }

    private static double calcTH(Instrument inst, BarSize size, double basePrice, double tick) {
        double perc = TimeframeUtil.getTimeframePercentage(size);
        return com.biotak.util.OptimizedCalculations.calculateTHPoints(inst, basePrice, perc) * tick;
    }

    /**
     * Builds a complete M-map (label → distance in price) used for Ruler-matching.
     * The map شامل تمام برچسب‌های موجود در power-of-2 و power-of-3 لیست‌هاست.
     */
    public static Map<String, Double> buildMMap(DataSeries series, double basePrice, double mScale) {
        Map<String, Double> out = new HashMap<>();
        Instrument inst = series.getInstrument();
        double tick = inst.getTickSize();

        // Iterate over both fractal maps
        java.util.function.BiConsumer<Integer,String> adder = (min,label)->{
            if (label==null||label.isEmpty()) return;
            if (out.containsKey(label)) return;
            double perc   = TimeframeUtil.getTimeframePercentageFromMinutes(min);
            double thPts  = com.biotak.util.OptimizedCalculations.calculateTHPoints(inst, basePrice, perc) * tick;
            double mVal   = mScale * thPts;
            if (mVal>0) out.put(label, mVal);
        };

        TimeframeUtil.getFractalMinutesMap().forEach(adder);
        TimeframeUtil.getPower3MinutesMap().forEach(adder);

        return out;
    }

    /**
     * Builds a map label → 3×ATRPrice for ruler matching.
     *
     * @param structureMinutes minutes of structure timeframe (current barSize converted)
     * @param structureATRPrice 1× ATR price of structure timeframe
     */
    public static Map<String, Double> buildATR3Map(int structureMinutes, double structureATRPrice) {
        Map<String, Double> out = new HashMap<>();
        java.util.function.BiConsumer<Integer,String> adder = (min,label)->{
            if (label==null||label.isEmpty()) return;
            if (out.containsKey(label)) return;
            double scale = Math.sqrt((double)min / structureMinutes);
            out.put(label, 3.0 * structureATRPrice * scale);
        };

        adder.accept(structureMinutes, formatMinutes(structureMinutes));
        TimeframeUtil.getFractalMinutesMap().forEach(adder);
        TimeframeUtil.getPower3MinutesMap().forEach(adder);

        return out;
    }
    
    /**
     * Builds a comprehensive ATR map covering all major timeframes for consistent matching
     * regardless of current timeframe. Maps timeframe labels to 1×ATR values (not 3×ATR).
     * 
     * Formula: leg = ATR × 3, so ATR = leg ÷ 3
     * We find the timeframe whose ATR is closest to (leg ÷ 3)
     *
     * @param basePrice Current price for calculations (like M method)
     * @param instrument Trading instrument for proper scaling
     * @return Map of timeframe labels to 1×ATR values in price units
     */
    public static Map<String, Double> buildComprehensiveATRMap(double basePrice, Instrument instrument) {
        Map<String, Double> out = new HashMap<>();
        double tick = instrument.getTickSize();
        
        // Use same method as M: calculate ATR (1×, not 3×) for each timeframe based on price and instrument
        // This ensures timeframe-independent results like M method
        java.util.function.BiConsumer<Integer,String> adder = (minutes, label) -> {
            if (label == null || label.isEmpty()) return;
            if (out.containsKey(label)) return;
            
            // Calculate TH for this timeframe (same as M method)
            double perc = TimeframeUtil.getTimeframePercentageFromMinutes(minutes);
            double thPts = com.biotak.util.OptimizedCalculations.calculateTHPoints(instrument, basePrice, perc) * tick;
            
            // For ATR matching: we store 1×ATR value (not 3×ATR)
            // TH approximates ATR for this timeframe (empirical relationship)
            double atrValue = thPts; // TH approximates 1×ATR for this timeframe
            
            if (atrValue > 0) {
                out.put(label, atrValue);
            }
        };
        
        // Add all fractal timeframes for comprehensive coverage
        TimeframeUtil.getFractalMinutesMap().forEach(adder);
        TimeframeUtil.getPower3MinutesMap().forEach(adder);
        
        // Add additional common timeframes to ensure comprehensive coverage
        Map<Integer, String> additionalTimeframes = Map.of(
            1, "1m",      // 1 minute
            5, "5m",      // 5 minutes  
            15, "15m",    // 15 minutes
            30, "30m",    // 30 minutes
            60, "1H",     // 1 hour
            240, "4H",    // 4 hours
            1440, "1D",   // 1 day
            10080, "1W"   // 1 week
        );
        
        additionalTimeframes.forEach(adder);
        
        return out;
    }

    /**
     * Builds a comprehensive step values map for any step type (E, TP, TH, SS, LS)
     * used for ruler matching.
     *
     * @param series DataSeries for instrument information
     * @param basePrice Current price for TH calculations
     * @param stepType Type of step ("E", "TP", "TH", "SS", "LS")
     * @return Map of timeframe labels to step values in price units
     */
    public static Map<String, Double> buildStepValuesMap(DataSeries series, double basePrice, String stepType) {
        Map<String, Double> out = new HashMap<>();
        Instrument inst = series.getInstrument();
        double tick = inst.getTickSize();
        
        // Define the multiplier based on step type
        double multiplier = switch (stepType.toUpperCase()) {
            case "E" -> 0.75;      // E = 0.75 * TH
            case "TP" -> 2.25;     // TP = 3 * E = 3 * 0.75 * TH = 2.25 * TH
            case "TH" -> 1.0;      // TH = 1.0 * TH
            case "SS" -> 1.5;      // SS = 1.5 * TH
            case "LS" -> 2.0;      // LS = 2.0 * TH
            default -> 1.0;       // Default to TH
        };

        // Iterate over both fractal maps
        java.util.function.BiConsumer<Integer,String> adder = (min,label)->{
            if (label==null||label.isEmpty()) return;
            if (out.containsKey(label)) return;
            double perc = TimeframeUtil.getTimeframePercentageFromMinutes(min);
            double thPts = com.biotak.util.OptimizedCalculations.calculateTHPoints(inst, basePrice, perc) * tick;
            double stepVal = multiplier * thPts;
            if (stepVal > 0) out.put(label, stepVal);
        };

        TimeframeUtil.getFractalMinutesMap().forEach(adder);
        TimeframeUtil.getPower3MinutesMap().forEach(adder);

        return out;
    }

    /**
     * Calculates the exact timeframe (in minutes) that produces the given ATR value
     * using the inverse of the fractal ATR relationship.
     * 
     * Formula: ATR scales as √(timeframe/baseTimeframe)
     * So: targetTimeframe = baseTimeframe × (targetATR/baseATR)²
     * 
     * @param targetATRValue The desired 1×ATR value in price units
     * @param basePrice Current price for calculations
     * @param instrument Trading instrument for proper scaling
     * @return Exact timeframe in minutes that produces the target ATR
     */
    public static double calculateExactTimeframeForATR(double targetATRValue, double basePrice, Instrument instrument) {
        double tick = instrument.getTickSize();
        
        // Use 1-minute as base reference timeframe
        double baseTimeframeMinutes = 1.0;
        double basePercentage = TimeframeUtil.getTimeframePercentageFromMinutes(1);
        double baseTHPoints = com.biotak.util.OptimizedCalculations.calculateTHPoints(instrument, basePrice, basePercentage) * tick;
        double baseATR = baseTHPoints; // TH approximates 1×ATR for this timeframe
        
        // Calculate exact timeframe using inverse square-root relationship
        // targetATR = baseATR × √(targetTimeframe/baseTimeframe)
        // So: targetTimeframe = baseTimeframe × (targetATR/baseATR)²
        double ratio = targetATRValue / baseATR;
        double exactTimeframeMinutes = baseTimeframeMinutes * ratio * ratio;
        
        return Math.max(1.0, exactTimeframeMinutes); // Ensure minimum 1 minute
    }
    
    /**
     * Formats exact timeframe minutes into a readable timeframe label.
     * Shows precise decimal minutes for accurate display.
     * 
     * @param exactMinutes Exact timeframe in minutes (can be fractional)
     * @return Formatted timeframe label (e.g., "1H23.5m", "87.2m", "2H15.8m")
     */
    public static String formatExactTimeframe(double exactMinutes) {
        if (exactMinutes <= 0) return "1m";
        
        return formatPreciseMinutes(exactMinutes);
    }
    
    /**
     * Formats precise decimal minutes into a readable timeframe label.
     * Shows decimal precision for better accuracy.
     * 
     * @param exactMinutes Exact timeframe in minutes (can be fractional)
     * @return Formatted label with precision (e.g., "16.1m", "1H23.5m", "87.2m")
     */
    private static String formatPreciseMinutes(double exactMinutes) {
        if (exactMinutes <= 0) return "1m";
        
        // For timeframes less than 60 minutes, show decimal minutes
        if (exactMinutes < 60) {
            // Round to 1 decimal place for readability
            double rounded = Math.round(exactMinutes * 10.0) / 10.0;
            // If it rounds to a whole number, don't show .0
            if (rounded == Math.floor(rounded)) {
                return String.format("%.0fm", rounded);
            } else {
                return String.format("%.1fm", rounded);
            }
        }
        
        // For timeframes >= 60 minutes, show hours and decimal minutes
        int hours = (int) (exactMinutes / 60);
        double remainingMinutes = exactMinutes - (hours * 60);
        
        if (remainingMinutes < 0.1) {
            // Very close to exact hour
            return hours + "H";
        } else {
            // Round remaining minutes to 1 decimal place
            double roundedMinutes = Math.round(remainingMinutes * 10.0) / 10.0;
            if (roundedMinutes == Math.floor(roundedMinutes)) {
                return String.format("%dH%.0fm", hours, roundedMinutes);
            } else {
                return String.format("%dH%.1fm", hours, roundedMinutes);
            }
        }
    }
    
    private static String formatMinutes(int minutes) {
        if (minutes <= 0) return String.valueOf(minutes);
        if (minutes % 60 == 0) {
            int h = minutes / 60;
            return h + "H";
        }
        if (minutes > 60) {
            int h = minutes / 60;
            int m = minutes % 60;
            return h + "H" + m + "m";
        }
        return minutes + "m";
    }
}
