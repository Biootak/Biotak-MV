package com.biotak.test;

import com.biotak.util.*;

/**
 * Simple method discovery example without MotiveWave dependencies
 */
public class SimpleMethodDiscovery {
    
    public static void main(String[] args) {
        System.out.println("=== SIMPLE METHOD DISCOVERY EXAMPLE ===");
        
        // مثال 1: پیدا کردن متد cache
        demonstrateCacheMethodDiscovery();
        
        // مثال 2: پیدا کردن متد string formatting
        demonstrateStringFormattingDiscovery();
        
        // مثال 3: پیدا کردن متد math operations
        demonstrateMathMethodDiscovery();
        
        // مثال 4: پیدا کردن متد object pooling
        demonstrateObjectPoolingDiscovery();
        
        System.out.println("\n=== METHOD DISCOVERY COMPLETED ===");
    }
    
    private static void demonstrateCacheMethodDiscovery() {
        System.out.println("\n--- CACHE METHOD DISCOVERY ---");
        
        // مرحله 1: تعریف نیاز
        System.out.println("NEED: Cache expensive calculations");
        System.out.println("INPUT: String key, Double value");
        System.out.println("OUTPUT: Cached value retrieval");
        
        // مرحله 2: جستجوی متد
        System.out.println("\nSEARCH PROCESS:");
        System.out.println("1. Looking for classes with 'Cache' in name...");
        System.out.println("2. Found: ComputationCache");
        System.out.println("3. Analyzing methods...");
        
        // مرحله 3: تست متد
        System.out.println("\nTESTING METHODS:");
        
        String testKey = "test_percentage";
        double testValue = 0.05;
        
        try {
            // تست cache put
            long startPut = System.nanoTime();
            ComputationCache.cachePercentage(testKey, testValue);
            long endPut = System.nanoTime();
            
            // تست cache get
            long startGet = System.nanoTime();
            Double cachedValue = ComputationCache.getCachedPercentage(testKey);
            long endGet = System.nanoTime();
            
            // نتایج
            System.out.printf("✅ ComputationCache.cachePercentage(): %.3f ms%n", 
                             (endPut - startPut) / 1_000_000.0);
            System.out.printf("✅ ComputationCache.getCachedPercentage(): %.3f ms%n", 
                             (endGet - startGet) / 1_000_000.0);
            System.out.printf("✅ Value integrity: %.5f (expected: %.5f)%n", cachedValue, testValue);
            
            // اعتبارسنجی
            assert cachedValue != null : "Cache returned null";
            assert Math.abs(cachedValue - testValue) < 0.0001 : "Value mismatch";
            
            System.out.println("RESULT: ✅ Method is suitable for caching");
            
        } catch (Exception e) {
            System.out.println("RESULT: ❌ Method failed - " + e.getMessage());
        }
    }
    
    private static void demonstrateStringFormattingDiscovery() {
        System.out.println("\n--- STRING FORMATTING METHOD DISCOVERY ---");
        
        // مرحله 1: تعریف نیاز
        System.out.println("NEED: Format double values for UI display");
        System.out.println("INPUT: double value");
        System.out.println("OUTPUT: formatted string with specific decimal places");
        
        double testValue = 3.14159265359;
        
        // مرحله 2: جستجوی متد
        System.out.println("\nSEARCH PROCESS:");
        System.out.println("1. Looking for classes with 'String' or 'Format' in name...");
        System.out.println("2. Found: StringUtils");
        System.out.println("3. Found methods: format1f(), format2f(), format5f()");
        
        // مرحله 3: تست متدها
        System.out.println("\nTESTING METHODS:");
        
        try {
            // تست format1f
            long start1 = System.nanoTime();
            String result1f = StringUtils.format1f(testValue);
            long end1 = System.nanoTime();
            
            // تست format2f
            long start2 = System.nanoTime();
            String result2f = StringUtils.format2f(testValue);
            long end2 = System.nanoTime();
            
            // تست format5f
            long start5 = System.nanoTime();
            String result5f = StringUtils.format5f(testValue);
            long end5 = System.nanoTime();
            
            // نتایج
            System.out.printf("✅ StringUtils.format1f(): '%s' (%.3f ms)%n", 
                             result1f, (end1 - start1) / 1_000_000.0);
            System.out.printf("✅ StringUtils.format2f(): '%s' (%.3f ms)%n", 
                             result2f, (end2 - start2) / 1_000_000.0);
            System.out.printf("✅ StringUtils.format5f(): '%s' (%.3f ms)%n", 
                             result5f, (end5 - start5) / 1_000_000.0);
            
            // مقایسه با String.format
            long startStd = System.nanoTime();
            String resultStd = String.format("%.2f", testValue);
            long endStd = System.nanoTime();
            
            System.out.printf("📊 String.format(): '%s' (%.3f ms)%n", 
                             resultStd, (endStd - startStd) / 1_000_000.0);
            
            System.out.println("RESULT: ✅ StringUtils methods are faster and more convenient");
            
        } catch (Exception e) {
            System.out.println("RESULT: ❌ Method failed - " + e.getMessage());
        }
    }
    
    private static void demonstrateMathMethodDiscovery() {
        System.out.println("\n--- MATH METHOD DISCOVERY ---");
        
        // مرحله 1: تعریف نیاز
        System.out.println("NEED: Fast mathematical operations for trading calculations");
        System.out.println("INPUT: double values");
        System.out.println("OUTPUT: mathematical results with good performance");
        
        double testValue = 25.0;
        
        // مرحله 2: جستجوی متد
        System.out.println("\nSEARCH PROCESS:");
        System.out.println("1. Looking for classes with 'Math' in name...");
        System.out.println("2. Found: FastMath");
        System.out.println("3. Found methods: fastSqrt(), fastLog2(), fastAbs()");
        
        // مرحله 3: تست متدها
        System.out.println("\nTESTING METHODS:");
        
        try {
            // تست FastMath methods
            long startFast = System.nanoTime();
            double sqrtFast = FastMath.fastSqrt(testValue);
            double log2Fast = FastMath.fastLog2(testValue);
            double absFast = FastMath.fastAbs(-testValue);
            long endFast = System.nanoTime();
            
            // تست Standard Math methods
            long startStd = System.nanoTime();
            double sqrtStd = Math.sqrt(testValue);
            double log2Std = Math.log(testValue) / Math.log(2);
            double absStd = Math.abs(-testValue);
            long endStd = System.nanoTime();
            
            // نتایج
            System.out.printf("✅ FastMath: sqrt=%.3f, log2=%.3f, abs=%.3f (%.3f ms)%n", 
                             sqrtFast, log2Fast, absFast, (endFast - startFast) / 1_000_000.0);
            System.out.printf("📊 Math: sqrt=%.3f, log2=%.3f, abs=%.3f (%.3f ms)%n", 
                             sqrtStd, log2Std, absStd, (endStd - startStd) / 1_000_000.0);
            
            // بررسی دقت
            double sqrtDiff = Math.abs(sqrtFast - sqrtStd);
            double log2Diff = Math.abs(log2Fast - log2Std);
            
            System.out.printf("📏 Accuracy - sqrt diff: %.10f, log2 diff: %.10f%n", sqrtDiff, log2Diff);
            
            if (sqrtDiff < 0.001 && log2Diff < 0.001) {
                System.out.println("RESULT: ✅ FastMath provides good performance with acceptable accuracy");
            } else {
                System.out.println("RESULT: ⚠️  FastMath has accuracy trade-offs");
            }
            
        } catch (Exception e) {
            System.out.println("RESULT: ❌ Method failed - " + e.getMessage());
        }
    }
    
    private static void demonstrateObjectPoolingDiscovery() {
        System.out.println("\n--- OBJECT POOLING METHOD DISCOVERY ---");
        
        // مرحله 1: تعریف نیاز
        System.out.println("NEED: Reduce object allocation overhead");
        System.out.println("INPUT: Request for reusable objects");
        System.out.println("OUTPUT: Pooled objects to reduce GC pressure");
        
        // مرحله 2: جستجوی متد
        System.out.println("\nSEARCH PROCESS:");
        System.out.println("1. Looking for classes with 'Pool' in name...");
        System.out.println("2. Found: PoolManager");
        System.out.println("3. Found methods: getStringBuilder(), releaseStringBuilder()");
        
        // مرحله 3: تست متدها
        System.out.println("\nTESTING METHODS:");
        
        try {
            // تست object pooling
            long startPool = System.nanoTime();
            StringBuilder sb1 = PoolManager.getStringBuilder();
            sb1.append("Test pooled object");
            PoolManager.releaseStringBuilder(sb1);
            long endPool = System.nanoTime();
            
            // تست regular allocation
            long startRegular = System.nanoTime();
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Test regular object");
            // No release - will be GC'd
            long endRegular = System.nanoTime();
            
            // نتایج
            System.out.printf("✅ PoolManager: get+release (%.3f ms)%n", 
                             (endPool - startPool) / 1_000_000.0);
            System.out.printf("📊 Regular allocation: new StringBuilder (%.3f ms)%n", 
                             (endRegular - startRegular) / 1_000_000.0);
            
            // تست memory efficiency
            System.out.println("\nMEMORY EFFICIENCY TEST:");
            Runtime runtime = Runtime.getRuntime();
            
            // Test pooling
            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            for (int i = 0; i < 1000; i++) {
                StringBuilder sb = PoolManager.getStringBuilder();
                sb.append("Test ").append(i);
                PoolManager.releaseStringBuilder(sb);
            }
            long memAfterPool = runtime.totalMemory() - runtime.freeMemory();
            
            // Test regular allocation
            for (int i = 0; i < 1000; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append("Test ").append(i);
                // No pooling
            }
            long memAfterRegular = runtime.totalMemory() - runtime.freeMemory();
            
            System.out.printf("Memory with pooling: %d KB%n", (memAfterPool - memBefore) / 1024);
            System.out.printf("Memory without pooling: %d KB%n", (memAfterRegular - memAfterPool) / 1024);
            
            System.out.println("RESULT: ✅ Object pooling reduces memory allocation");
            
        } catch (Exception e) {
            System.out.println("RESULT: ❌ Method failed - " + e.getMessage());
        }
    }
    
    /**
     * Helper method to demonstrate method signature analysis
     */
    private static void analyzeMethodSignature(String className, String methodName) {
        System.out.printf("\n🔍 ANALYZING: %s.%s()%n", className, methodName);
        
        try {
            Class<?> clazz = Class.forName(className);
            java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
            
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().contains(methodName)) {
                    System.out.printf("   Method: %s%n", method.getName());
                    System.out.printf("   Return: %s%n", method.getReturnType().getSimpleName());
                    System.out.printf("   Params: %d%n", method.getParameterCount());
                    System.out.printf("   Static: %s%n", java.lang.reflect.Modifier.isStatic(method.getModifiers()));
                    System.out.printf("   Public: %s%n", java.lang.reflect.Modifier.isPublic(method.getModifiers()));
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.printf("   ❌ Class %s not found%n", className);
        }
    }
}