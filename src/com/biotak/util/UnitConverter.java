package com.biotak.util;

import com.motivewave.platform.sdk.common.Instrument;

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

    /**
     * Determines the appropriate pip multiplier for a given instrument.
     * 
     * @param instrument The trading instrument
     * @return The multiplier to convert from price to pips
     */
    public static double getPipMultiplier(Instrument instrument) {
        if (instrument == null) return 10.0; // Default multiplier
        
        // Create cache key from instrument properties
        String symbol = instrument.getSymbol();
        double tickSize = instrument.getTickSize();
        String cacheKey = symbol + "_" + tickSize;
        
        // Check cache first
        Double cached = ComputationCache.getCachedPipMultiplier(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        double result;
        
        // Determine number of decimal places in tick size
        int decimalPlaces = 0;
        if (tickSize > 0) {
            String tickStr = String.valueOf(tickSize);
            if (tickStr.contains(".")) {
                decimalPlaces = tickStr.length() - tickStr.indexOf('.') - 1;
            }
        }
        
        String sym = symbol == null ? "" : symbol.toUpperCase();
        
        // 1) Explicit symbol-based overrides (for assets with conventional pip different from tick)
        //    Metals (spot): many platforms consider 1 pip = 0.1 for XAU/XAG
        if (sym.startsWith("XAU") || sym.contains("GOLD") || sym.startsWith("XAG") || sym.contains("SILVER")) {
            result = 10.0; // 0.1 unit per pip
        }
        //    Crypto (BTC, ETH, ...): treat 1 pip = 1.0 by default
        else if (sym.contains("BTC") || sym.contains("ETH") || sym.contains("SOL") || sym.contains("ADA") || sym.contains("DOGE") || sym.contains("XRP")) {
            result = 1.0; // $1 per pip (can be refined per broker later)
        }
        // 2) Forex pairs
        else if (symbol != null && 
            (symbol.contains("/") || 
             (symbol.length() >= 6 && !symbol.contains(".")))) {
            
            // JPY pairs typically have 2 decimal places (1 pip = 0.01)
            if (sym.contains("JPY")) {
                result = 100.0;
            }
            // Most other forex pairs: 1 pip = 0.0001
            else if (decimalPlaces >= 4) {
                result = 10.0;
            }
            else {
                result = 10.0; // Default for forex
            }
        }
        // 3) Fallback: infer by decimal places of tick size
        else {
            switch (decimalPlaces) {
                case 0: result = 1.0; break;    // No decimal places
                case 1: result = 10.0; break;   // 1 decimal place
                case 2: result = 100.0; break;  // 2 decimal places
                case 3: result = 10.0; break;   // 3 decimal places (unusual)
                case 4: result = 10.0; break;   // 4 decimal places (standard forex)
                case 5: result = 10.0; break;   // 5 decimal places (some brokers)
                default: result = 10.0; break;  // Default
            }
        }
        
        // Cache the result
        ComputationCache.cachePipMultiplier(cacheKey, result);
        
        return result;
    }

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
        double pipMultiplier = getPipMultiplier(instrument);
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
        double pipMultiplier = getPipMultiplier(instrument);
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