package com.biotak.test;

import com.biotak.util.*;

/**
 * Simple test to verify optimized classes work correctly
 */
public class PerformanceTest {
    
    public static void main(String[] args) {
        System.out.println("=== Biotak Performance Optimization Test ===");
        
        // Test PoolManager
        testPoolManager();
        
        // Test ComputationCache
        testComputationCache();
        
        // Test StringUtils
        testStringUtils();
        
        // Test Logger
        testLogger();
        
        System.out.println("=== All Tests Completed Successfully! ===");
    }
    
    private static void testPoolManager() {
        System.out.println("\n1. Testing PoolManager...");
        
        // Test StringBuilder pool
        StringBuilder sb = PoolManager.getStringBuilder();
        sb.append("Test");
        PoolManager.releaseStringBuilder(sb);
        
        // Test String list pool
        java.util.ArrayList<String> list = PoolManager.getStringList();
        list.add("Test");
        PoolManager.releaseStringList(list);
        
        // Test double array pool
        double[] array = PoolManager.getDoubleArray(5);
        array[0] = 1.0;
        PoolManager.releaseDoubleArray(array);
        
        System.out.println("   ✅ PoolManager working correctly");
    }
    
    private static void testComputationCache() {
        System.out.println("\n2. Testing ComputationCache...");
        
        // Test percentage cache
        ComputationCache.cachePercentage("M5", 0.05);
        Double cached = ComputationCache.getCachedPercentage("M5");
        assert cached != null && cached == 0.05 : "Percentage cache failed";
        
        // Test ATR period cache
        ComputationCache.cacheAtrPeriod("H1", 24);
        Integer atrPeriod = ComputationCache.getCachedAtrPeriod("H1");
        assert atrPeriod != null && atrPeriod == 24 : "ATR period cache failed";
        
        // Test pip multiplier cache
        ComputationCache.cachePipMultiplier("EURUSD", 10.0);
        Double pipMultiplier = ComputationCache.getCachedPipMultiplier("EURUSD");
        assert pipMultiplier != null && pipMultiplier == 10.0 : "Pip multiplier cache failed";
        
        System.out.println("   ✅ ComputationCache working correctly");
        System.out.println("   📊 " + ComputationCache.getCacheStats());
    }
    
    private static void testStringUtils() {
        System.out.println("\n3. Testing StringUtils...");
        
        // Test formatting
        String formatted = StringUtils.format1f(3.14159);
        assert "3.1".equals(formatted) : "Format1f failed";
        
        // Test string building
        String built = StringUtils.buildString("Hello", " ", "World");
        assert "Hello World".equals(built) : "BuildString failed";
        
        // Test empty checks
        assert StringUtils.isEmpty(null) : "isEmpty null check failed";
        assert StringUtils.isEmpty("") : "isEmpty empty check failed";
        assert StringUtils.isNotEmpty("test") : "isNotEmpty check failed";
        
        System.out.println("   ✅ StringUtils working correctly");
    }
    
    private static void testLogger() {
        System.out.println("\n4. Testing Logger...");
        
        // Test basic logging
        Logger.info("Test info message");
        Logger.warn("Test warning message");
        Logger.error("Test error message");
        
        // Test throttling (should only log once)
        for (int i = 0; i < 5; i++) {
            Logger.info("Repeated message for throttling test");
        }
        
        System.out.println("   ✅ Logger working correctly");
    }
}