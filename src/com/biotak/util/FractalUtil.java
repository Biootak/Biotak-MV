package com.biotak.util;

import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Instrument;
import com.motivewave.platform.sdk.common.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper utilities for fractal calculations used by the Biotak Trigger indicator.
 * <p>
 * تمام منطق محاسبات فراکتالی که قبلاً در چند نقطه پراکنده بود، این‌جا متمرکز شده است تا
 * نگه‌داری آسان‌تر شود و از تکرار کد جلوگیری گردد.
 */
public final class FractalUtil {

    // Cache names for different types of calculations
    private static final String TH_BUNDLE_CACHE = "th_bundle";
    private static final String PERCENTAGE_CACHE = "percentage";
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
            double manualHigh = settings.getDouble(com.biotak.util.Constants.S_MANUAL_HIGH, 0);
            double manualLow  = settings.getDouble(com.biotak.util.Constants.S_MANUAL_LOW, 0);
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
        return THCalculator.calculateTHPoints(inst, basePrice, perc) * tick;
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
            double thPts  = THCalculator.calculateTHPoints(inst, basePrice, perc) * tick;
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