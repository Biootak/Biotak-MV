package com.biotak.util;

import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Instrument;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper utilities for fractal calculations used by the Biotak Trigger indicator.
 * <p>
 * تمام منطق محاسبات فراکتالی که قبلاً در چند نقطه پراکنده بود، این‌جا متمرکز شده است تا
 * نگه‌داری آسان‌تر شود و از تکرار کد جلوگیری گردد.
 */
public final class FractalUtil {

    private FractalUtil() {}

    /**
     * Container for TH-مقادیر پنج سطح فراکتالی (Current, Pattern, Trigger, Structure, Higher-Pattern).
     */
    public record THBundle(double th, double pattern, double trigger,
                           double structure, double higherPattern) {}

    /**
     * Calculates TH values for پنج سطح فراکتالی اطراف {@code barSize} با استفاده از قیمت پایه.
     */
    public static THBundle calculateTHBundle(Instrument instrument, BarSize barSize, double basePrice) {
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

        return new THBundle(th, patternTH, triggerTH, structureTH, higherPatternTH);
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
            double perc   = TimeframeUtil.getTimeframePercentage(min);
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