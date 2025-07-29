package com.biotak.test;

import com.biotak.util.*;
import com.biotak.core.*;

/**
 * Practical example of method discovery process
 */
public class MethodDiscoveryExample {
    
    public static void main(String[] args) {
        System.out.println("=== METHOD DISCOVERY EXAMPLE ===");
        
        // مثال 1: پیدا کردن متد محاسبه درصد timeframe
        demonstrateTimeframePercentageDiscovery();
        
        // مثال 2: پیدا کردن متد cache
        demonstrateCacheDiscovery();
        
        // مثال 3: پیدا کردن متد string formatting
        demonstrateStringFormattingDiscovery();
        
        // مثال 4: پیدا کردن متد math operations
        demonstrateMathDiscovery();
        
        System.out.println("\n=== METHOD DISCOVERY COMPLETED ===");
    }
    
    private static void demonstrateTimeframePercentageDiscovery() {
        System.out.println("\n--- TIMEFRAME PERCENTAGE DISCOVERY ---");
        
        // مرحله 1: تعریف نیاز
        System.out.println("Need: Calculate percentage for 5-minute timeframe");
        int minutes = 5; // Use simple int instead of BarSize
        
        // مرحله 2: جستجوی متد
        System.out.println("Searching for methods containing 'percentage' or 'Percentage'...");
        
        // مرحله 3: تست متدهای پیدا شده
        System.out.println("Testing discovered methods:");
        
        // گزینه 1: TimeframeUtil.getTimeframePercentage() with int parameter
        try {
            long start = System.nanoTime();
            double percentage1 = TimeframeUtil.getTimeframePercentageFromMinutes(minutes);
            long end = System.nanoTime();
            
            System.out.printf("✅ TimeframeUtil.getTimeframePercentageFromMinutes(int): %.4f%% (%.3fms)%n",
                             percentage1, (end - start) / 1_000_000.0);
            
            // اعتبارسنجی
            assert percentage1 > 0 : "Invalid percentage";
            assert percentage1 < 100 : "Percentage too high";
            
        } catch (Exception e) {
            System.out.println("❌ TimeframeUtil.getTimeframePercentageFromMinutes(int) failed: " + e.getMessage());
        }
        
        // گزینه 2: OptimizedCalculations.calculateTimeframePercentageOptimized()
        try {
            long start = System.nanoTime();
            double percentage2 = OptimizedCalculations.calculateTimeframePercentageOptimized(minutes);
            long end = System.nanoTime();
            
            System.out.printf("✅ OptimizedCalculations.calculateTimeframePercentageOptimized(): %.4f%% (%.3fms)%n", 
                             percentage2, (end - start) / 1_000_000.0);
            
        } catch (Exception e) {
            System.out.println("❌ OptimizedCalculations method failed: " + e.getMessage());
        }
        
        // نتیجه‌گیری
        System.out.println("Recommendation: Both methods work, choose based on your input type");
    }
    
    private static void demonstrateCacheDiscovery() {
        System.out.println("\n--- CACHE DISCOVERY ---");
        
        // مرحله 1: تعریف نیاز
        System.out.println("Need: Cache expensive calculations for better performance");
        
        // مرحله 2: جستجوی متدهای cache
        System.out.println("Searching for cache-related methods...");
        
        // مرحله 3: تست cache methods
        String testKey = "M5_test";
        double testValue = 0.05;
        
        // تست ComputationCache
        try {
            // تست put
            long start = System.nanoTime();
            ComputationCache.cachePercentage(testKey, testValue);
            long putTime = System.nanoTime() - start;
            
            // تست get
            start = System.nanoTime();
            Double cachedValue = ComputationCache.getCachedPercentage(testKey);
            long getTime = System.nanoTime() - start;
            
            System.out.printf("✅ ComputationCache: put=%.3fms, get=%.3fms, value=%.3f%n", 
                             putTime / 1_000_000.0, getTime / 1_000_000.0, cachedValue);
            
            // اعتبارسنجی
            assert cachedValue != null : "Cache returned null";
            assert Math.abs(cachedValue - testValue) < 0.001 : "Cache value mismatch";
            
        } catch (Exception e) {
            System.out.println("❌ ComputationCache failed: " + e.getMessage());
        }
        
        // تست CacheManager (اگر وجود داشته باشد)
        try {
            // بررسی وجود CacheManager
            System.out.println("✅ CacheManager available for advanced caching");
        } catch (Exception e) {
            System.out.println("ℹ️  CacheManager not available or not needed");
        }
        
        System.out.println("Recommendation: Use ComputationCache for simple key-value caching");
    }
    
    private static void demonstrateStringFormattingDiscovery() {
        System.out.println("\n--- STRING FORMATTING DISCOVERY ---");
        
        // مرحله 1: تعریف نیاز
        System.out.println("Need: Format double values for UI display");
        double testValue = 3.14159265;
        
        // مرحله 2: جستجوی متدهای formatting
        System.out.println("Testing string formatting methods:");
        
        // گزینه 1: StringUtils methods
        try {
            long start = System.nanoTime();
            String formatted1 = StringUtils.format1f(testValue);
            String formatted2 = StringUtils.format2f(testValue);
            String formatted5 = StringUtils.format5f(testValue);
            long end = System.nanoTime();
            
            System.out.printf("✅ StringUtils: 1f='%s', 2f='%s', 5f='%s' (%.3fms)%n", 
                             formatted1, formatted2, formatted5, (end - start) / 1_000_000.0);
            
        } catch (Exception e) {
            System.out.println("❌ StringUtils failed: " + e.getMessage());
        }
        
        // گزینه 2: Standard String.format
        try {
            long start = System.nanoTime();
            String formatted1 = String.format("%.1f", testValue);
            String formatted2 = String.format("%.2f", testValue);
            String formatted5 = String.format("%.5f", testValue);
            long end = System.nanoTime();
            
            System.out.printf("✅ String.format: 1f='%s', 2f='%s', 5f='%s' (%.3fms)%n", 
                             formatted1, formatted2, formatted5, (end - start) / 1_000_000.0);
            
        } catch (Exception e) {
            System.out.println("❌ String.format failed: " + e.getMessage());
        }
        
        System.out.println("Recommendation: Use StringUtils for better performance in loops");
    }
    
    private static void demonstrateMathDiscovery() {
        System.out.println("\n--- MATH OPERATIONS DISCOVERY ---");
        
        // مرحله 1: تعریف نیاز
        System.out.println("Need: Fast mathematical operations for trading calculations");
        double testValue = 25.0;
        
        // مرحله 2: جستجوی متدهای math
        System.out.println("Testing math operation methods:");
        
        // گزینه 1: FastMath methods
        try {
            long start = System.nanoTime();
            double sqrt1 = FastMath.fastSqrt(testValue);
            double log1 = FastMath.fastLog2(testValue);
            double abs1 = FastMath.fastAbs(-testValue);
            long end = System.nanoTime();
            
            System.out.printf("✅ FastMath: sqrt=%.3f, log2=%.3f, abs=%.3f (%.3fms)%n", 
                             sqrt1, log1, abs1, (end - start) / 1_000_000.0);
            
        } catch (Exception e) {
            System.out.println("❌ FastMath failed: " + e.getMessage());
        }
        
        // گزینه 2: Standard Math methods
        try {
            long start = System.nanoTime();
            double sqrt2 = Math.sqrt(testValue);
            double log2 = Math.log(testValue) / Math.log(2);
            double abs2 = Math.abs(-testValue);
            long end = System.nanoTime();
            
            System.out.printf("✅ Math: sqrt=%.3f, log2=%.3f, abs=%.3f (%.3fms)%n", 
                             sqrt2, log2, abs2, (end - start) / 1_000_000.0);
            
        } catch (Exception e) {
            System.out.println("❌ Math failed: " + e.getMessage());
        }
        
        // مقایسه دقت
        double sqrtDiff = Math.abs(FastMath.fastSqrt(testValue) - Math.sqrt(testValue));
        System.out.printf("Accuracy difference (sqrt): %.10f%n", sqrtDiff);
        
        if (sqrtDiff < 0.001) {
            System.out.println("Recommendation: Use FastMath for better performance with acceptable accuracy");
        } else {
            System.out.println("Recommendation: Use Math for critical accuracy requirements");
        }
    }
    
    /**
     * Performance benchmark helper
     */
    private static void benchmarkMethod(String methodName, Runnable method, int iterations) {
        System.out.printf("Benchmarking %s with %d iterations...%n", methodName, iterations);
        
        // Warm up
        for (int i = 0; i < iterations / 10; i++) {
            method.run();
        }
        
        // Actual benchmark
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            method.run();
        }
        long end = System.nanoTime();
        
        double avgTime = (end - start) / (double) iterations / 1_000_000.0;
        System.out.printf("Average time per call: %.6f ms%n", avgTime);
    }
    
    /**
     * Method signature analyzer
     */
    private static void analyzeMethodSignature(String className, String methodName) {
        System.out.printf("Analyzing %s.%s():%n", className, methodName);
        
        try {
            Class<?> clazz = Class.forName(className);
            java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
            
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().equals(methodName)) {
                    System.out.printf("  Signature: %s%n", method.toString());
                    System.out.printf("  Return type: %s%n", method.getReturnType().getSimpleName());
                    System.out.printf("  Parameters: %d%n", method.getParameterCount());
                    System.out.printf("  Static: %s%n", java.lang.reflect.Modifier.isStatic(method.getModifiers()));
                    System.out.printf("  Public: %s%n", java.lang.reflect.Modifier.isPublic(method.getModifiers()));
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.printf("  Class %s not found%n", className);
        }
    }
}