package com.biotak.util;

/**
 * Fast mathematical operations optimized for trading calculations
 */
public final class FastMath {
    
    // Pre-calculated constants for common operations
    private static final double LOG_2 = Math.log(2.0);
    private static final double LOG_10 = Math.log(10.0);
    private static final double INV_LOG_2 = 1.0 / LOG_2;
    private static final double INV_LOG_10 = 1.0 / LOG_10;
    
    // Lookup tables for common trigonometric values (if needed)
    private static final double[] SIN_TABLE = new double[360];
    private static final double[] COS_TABLE = new double[360];
    
    // Square root lookup table for small integers
    private static final double[] SQRT_TABLE = new double[10000];
    private static final boolean[] SQRT_TABLE_VALID = new boolean[10000];
    
    static {
        // Pre-calculate square roots for common values
        for (int i = 0; i < SQRT_TABLE.length; i++) {
            SQRT_TABLE[i] = Math.sqrt(i);
            SQRT_TABLE_VALID[i] = true;
        }
        
        // Pre-calculate trigonometric values if needed
        for (int i = 0; i < 360; i++) {
            double radians = Math.toRadians(i);
            SIN_TABLE[i] = Math.sin(radians);
            COS_TABLE[i] = Math.cos(radians);
        }
    }
    
    private FastMath() {}
    
    /**
     * Fast square root using lookup table for small values
     */
    public static double fastSqrt(double x) {
        if (x >= 0 && x < SQRT_TABLE.length && x == (int)x) {
            return SQRT_TABLE[(int)x];
        }
        return Math.sqrt(x);
    }
    
    /**
     * Fast power of 2 calculation using bit shifting
     */
    public static long fastPow2(int exponent) {
        if (exponent < 0 || exponent >= 63) {
            return (long) Math.pow(2, exponent);
        }
        return 1L << exponent;
    }
    
    /**
     * Fast logarithm base 2
     */
    public static double fastLog2(double x) {
        return Math.log(x) * INV_LOG_2;
    }
    
    /**
     * Fast logarithm base 10
     */
    public static double fastLog10(double x) {
        return Math.log(x) * INV_LOG_10;
    }
    
    /**
     * Fast absolute value for doubles
     */
    public static double fastAbs(double x) {
        return x < 0 ? -x : x;
    }
    
    /**
     * Fast min/max without branching (using bit manipulation)
     */
    public static double fastMin(double a, double b) {
        return a < b ? a : b; // JVM optimizes this well
    }
    
    public static double fastMax(double a, double b) {
        return a > b ? a : b; // JVM optimizes this well
    }
    
    /**
     * Fast rounding to specified decimal places
     */
    public static double fastRound(double value, int decimalPlaces) {
        if (decimalPlaces == 0) {
            return Math.round(value);
        }
        
        double multiplier = fastPow10(decimalPlaces);
        return Math.round(value * multiplier) / multiplier;
    }
    
    /**
     * Fast power of 10 calculation
     */
    private static double fastPow10(int exponent) {
        switch (exponent) {
            case 0: return 1.0;
            case 1: return 10.0;
            case 2: return 100.0;
            case 3: return 1000.0;
            case 4: return 10000.0;
            case 5: return 100000.0;
            default: return Math.pow(10, exponent);
        }
    }
    
    /**
     * Fast percentage calculation
     */
    public static double fastPercentage(double value, double total) {
        if (total == 0) return 0;
        return (value * 100.0) / total;
    }
    
    /**
     * Fast percentage of value calculation
     */
    public static double fastPercentageOf(double percentage, double value) {
        return (percentage * value) / 100.0;
    }
    
    /**
     * Fast linear interpolation
     */
    public static double fastLerp(double a, double b, double t) {
        return a + t * (b - a);
    }
    
    /**
     * Fast clamp function
     */
    public static double fastClamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    
    /**
     * Fast sign function
     */
    public static int fastSign(double value) {
        if (value > 0) return 1;
        if (value < 0) return -1;
        return 0;
    }
    
    /**
     * Fast comparison with epsilon for floating point numbers
     */
    public static boolean fastEquals(double a, double b, double epsilon) {
        return fastAbs(a - b) < epsilon;
    }
    
    /**
     * Fast comparison with default epsilon
     */
    public static boolean fastEquals(double a, double b) {
        return fastEquals(a, b, 1e-9);
    }
}