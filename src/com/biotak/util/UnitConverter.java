package com.biotak.util;

import com.motivewave.platform.sdk.common.Instrument;
import com.biotak.core.FractalCalculator;

/**
 * Centralized helper for converting between price, pip and point units.
 *
 * <p>Design rules:
 * <ul>
 *     <li>ALL core calculations in the project should remain in <b>price units</b>.</li>
 *     <li>Use these helpers only when you need to convert for UI display or user-provided pip/point inputs.</li>
 * </ul>
 *
 * Keeping the conversion logic in one place guarantees consistent behaviour across the code-base and
 * makes it trivial to adapt when new instrument types are introduced.
 */
public final class UnitConverter {

    private UnitConverter() {}

    /* ------------------------------------------------------------- */
    /*  Pip ↔ Price                                                  */
    /* ------------------------------------------------------------- */

    /**
     * Converts a price difference (absolute value) to pips.
     *
     * @param priceDiff  difference in price units (e.g. 0.0012 for EURUSD)
     * @param instrument trading instrument
     * @return equivalent value in pips
     */
    public static double priceToPip(double priceDiff, Instrument instrument) {
        if (instrument == null) return 0;
        double pipMultiplier = FractalCalculator.getPipMultiplier(instrument);
        return priceDiff * pipMultiplier;
    }

    /**
     * Converts pips to price units.
     *
     * @param pips       size in pips
     * @param instrument trading instrument
     * @return equivalent price difference
     */
    public static double pipToPrice(double pips, Instrument instrument) {
        if (instrument == null) return 0;
        double pipMultiplier = FractalCalculator.getPipMultiplier(instrument);
        return pips / pipMultiplier;
    }

    /* ------------------------------------------------------------- */
    /*  Point ↔ Price                                                */
    /* ------------------------------------------------------------- */

    /**
     * Converts a price difference to points (MotiveWave tick units).
     */
    public static double priceToPoint(double priceDiff, Instrument instrument) {
        if (instrument == null) return 0;
        double tick = instrument.getTickSize();
        if (tick == 0) return 0;
        return priceDiff / tick;
    }

    /**
     * Converts points to price units.
     */
    public static double pointToPrice(double points, Instrument instrument) {
        if (instrument == null) return 0;
        return points * instrument.getTickSize();
    }
} 