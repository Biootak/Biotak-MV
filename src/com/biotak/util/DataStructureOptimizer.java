package com.biotak.util;

import com.biotak.debug.AdvancedLogger;

import java.util.Arrays;

/**
 * Optimized data structures for better memory layout and cache performance
 */
public final class DataStructureOptimizer {
    
    // Pre-allocated arrays for common calculations to avoid repeated allocation
    private static final ThreadLocal<double[]> TEMP_DOUBLE_ARRAY_5 = 
        ThreadLocal.withInitial(() -> new double[5]);
    
    private static final ThreadLocal<double[]> TEMP_DOUBLE_ARRAY_10 = 
        ThreadLocal.withInitial(() -> new double[10]);
    
    private static final ThreadLocal<double[]> TEMP_DOUBLE_ARRAY_100 = 
        ThreadLocal.withInitial(() -> new double[100]);
    
    // Primitive collections for better performance
    private static final ThreadLocal<int[]> TEMP_INT_ARRAY = 
        ThreadLocal.withInitial(() -> new int[50]);
    
    private DataStructureOptimizer() {}
    
    /**
     * Get a temporary double array for calculations
     */
    public static double[] getTempDoubleArray(int size) {
        if (size <= 5) {
            double[] array = TEMP_DOUBLE_ARRAY_5.get();
            Arrays.fill(array, 0, size, 0.0); // Clear only needed elements
            return array;
        } else if (size <= 10) {
            double[] array = TEMP_DOUBLE_ARRAY_10.get();
            Arrays.fill(array, 0, size, 0.0);
            return array;
        } else if (size <= 100) {
            double[] array = TEMP_DOUBLE_ARRAY_100.get();
            Arrays.fill(array, 0, size, 0.0);
            return array;
        } else {
            // For very large arrays, create new one
            return new double[size];
        }
    }
    
    /**
     * Get a temporary int array for calculations
     */
    public static int[] getTempIntArray(int size) {
        if (size <= 50) {
            int[] array = TEMP_INT_ARRAY.get();
            Arrays.fill(array, 0, size, 0);
            return array;
        } else {
            return new int[size];
        }
    }
    
    /**
     * Optimized array copy for small arrays
     */
    public static void fastArrayCopy(double[] src, double[] dest, int length) {
        if (length <= 8) {
            // Manual unrolled copy for small arrays
            switch (length) {
                case 8: dest[7] = src[7];
                case 7: dest[6] = src[6];
                case 6: dest[5] = src[5];
                case 5: dest[4] = src[4];
                case 4: dest[3] = src[3];
                case 3: dest[2] = src[2];
                case 2: dest[1] = src[1];
                case 1: dest[0] = src[0];
                case 0: break;
            }
        } else {
            System.arraycopy(src, 0, dest, 0, length);
        }
    }
    
    /**
     * Optimized min/max finding with SIMD-like approach
     */
    public static double[] findMinMaxSIMD(double[] values, int start, int end) {
        if (start >= end) return new double[]{0, 0};
        
        double min = values[start];
        double max = values[start];
        
        // Process 4 elements at once for better CPU cache utilization
        int i = start + 1;
        for (; i + 3 < end; i += 4) {
            double v1 = values[i];
            double v2 = values[i + 1];
            double v3 = values[i + 2];
            double v4 = values[i + 3];
            
            // Find min/max of the 4 values
            double localMin = Math.min(Math.min(v1, v2), Math.min(v3, v4));
            double localMax = Math.max(Math.max(v1, v2), Math.max(v3, v4));
            
            if (localMin < min) min = localMin;
            if (localMax > max) max = localMax;
        }
        
        // Handle remaining elements
        for (; i < end; i++) {
            double v = values[i];
            if (v < min) min = v;
            if (v > max) max = v;
        }
        
        return new double[]{max, min};
    }
    
    /**
     * Bit manipulation optimizations for common operations
     */
    public static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    /**
     * Fast integer division by power of 2
     */
    public static int fastDivideByPowerOf2(int value, int powerOf2) {
        if (!isPowerOfTwo(powerOf2)) {
            return value / powerOf2; // Fallback to normal division
        }
        
        // Use bit shifting for power of 2 divisions
        int shift = Integer.numberOfTrailingZeros(powerOf2);
        return value >> shift;
    }
    
    /**
     * Fast modulo by power of 2
     */
    public static int fastModByPowerOf2(int value, int powerOf2) {
        if (!isPowerOfTwo(powerOf2)) {
            return value % powerOf2; // Fallback to normal modulo
        }
        
        return value & (powerOf2 - 1);
    }
}