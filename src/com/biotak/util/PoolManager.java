package com.biotak.util;

import java.util.ArrayList;

/**
 * Centralized pool manager for commonly used objects to reduce memory allocation
 */
public final class PoolManager {
    
    // StringBuilder pool for string operations
    private static final ObjectPool<StringBuilder> stringBuilderPool = 
        new ObjectPool<>(() -> new StringBuilder(128), 20);
    
    // ArrayList pool for temporary lists
    private static final ObjectPool<ArrayList<String>> stringListPool = 
        new ObjectPool<>(() -> new ArrayList<>(10), 15);
    
    // Double array pool for calculations
    private static final ObjectPool<double[]> doubleArrayPool = 
        new ObjectPool<>(() -> new double[10], 10);
    
    private PoolManager() {}
    
    /**
     * Get a StringBuilder from the pool
     */
    public static StringBuilder getStringBuilder() {
        StringBuilder sb = stringBuilderPool.acquire();
        sb.setLength(0); // Clear any existing content
        return sb;
    }
    
    /**
     * Return a StringBuilder to the pool
     */
    public static void releaseStringBuilder(StringBuilder sb) {
        if (sb != null && sb.capacity() < 1024) { // Don't pool very large builders
            stringBuilderPool.release(sb);
        }
    }
    
    /**
     * Get a String ArrayList from the pool
     */
    public static ArrayList<String> getStringList() {
        ArrayList<String> list = stringListPool.acquire();
        list.clear(); // Clear any existing content
        return list;
    }
    
    /**
     * Return a String ArrayList to the pool
     */
    public static void releaseStringList(ArrayList<String> list) {
        if (list != null && list.size() < 100) { // Don't pool very large lists
            stringListPool.release(list);
        }
    }
    
    /**
     * Get a double array from the pool
     */
    public static double[] getDoubleArray(int minSize) {
        double[] array = doubleArrayPool.acquire();
        if (array.length < minSize) {
            // If pooled array is too small, create a new one
            return new double[minSize];
        }
        return array;
    }
    
    /**
     * Return a double array to the pool
     */
    public static void releaseDoubleArray(double[] array) {
        if (array != null && array.length <= 50) { // Don't pool very large arrays
            doubleArrayPool.release(array);
        }
    }
    
    /**
     * Clear all pools (for cleanup)
     */
    public static void clearAll() {
        stringBuilderPool.clear();
        stringListPool.clear();
        doubleArrayPool.clear();
    }
}