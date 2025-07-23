package com.biotak.util;

import com.motivewave.platform.sdk.common.Instrument;

/**
 * Utility class for performing the core TH (Trigger Horizon) calculations.
 */
public final class THCalculator {

    private THCalculator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Calculates the TH value. This is a core calculation from the original MQL4 code.
     * The logic with 'digits' is specific to how MT4 handles price normalization.
     */
    private static double calculateTH(double price, int digits, double percentage) {
        if (price <= 0 || percentage <= 0) return 0;

        double baseValue = price;
        switch (digits) {
            case 0: baseValue /= 100.0; break;
            case 1: baseValue /= 10.0; break;
            case 2: break; // Default
            case 3: baseValue /= 10.0; break;
            case 4:
            case 5: baseValue *= 100.0; break;
            case 6: baseValue *= 1000.0; break;
            case 7:
            case 8: baseValue *= 10000.0; break;
        }
        // Fixed: removed division by 10.0 to match MT4 implementation
        return (baseValue * percentage);
    }

    /**
     * Calculates the step size in points for the TH levels.
     * Fixed to match the MT4 implementation exactly.
     */
    public static double calculateTHPoints(Instrument instrument, double price, double percentage) {
        // Use optimized calculation for better performance
        return OptimizedCalculations.calculateTHOptimized(instrument, price, percentage);
    }
} 