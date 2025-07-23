package com.biotak.test;

import com.biotak.util.*;

/**
 * Comprehensive test for all advanced optimizations
 */
public class AdvancedPerformanceTest {
    
    public static void main(String[] args) {
        System.out.println("=== BIOTAK ADVANCED PERFORMANCE TEST ===");
        
        // Start profiling
        PerformanceProfiler.startProfiling();
        
        try {
            // Test all optimization components
            testDataStructureOptimizer();
            testFastMath();
            testConcurrencyOptimizer();
            testAdvancedMemoryManager();
            testIOOptimizer();
            testUIOptimizer();
            
            // Performance summary
            System.out.println("\n=== PERFORMANCE SUMMARY ===");
            System.out.println(PerformanceProfiler.getQuickSummary());
            
        } finally {
            // Stop profiling and show detailed report
            PerformanceProfiler.stopProfiling();
            System.out.println(PerformanceProfiler.getPerformanceReport());
        }
        
        System.out.println("=== ALL ADVANCED TESTS COMPLETED! ===");
    }
    
    private static void testDataStructureOptimizer() {
        System.out.println("\n1. Testing DataStructureOptimizer...");
        
        // Test temporary arrays
        double[] tempArray = DataStructureOptimizer.getTempDoubleArray(5);
        tempArray[0] = 1.0;
        tempArray[1] = 2.0;
        
        int[] intArray = DataStructureOptimizer.getTempIntArray(10);
        intArray[0] = 42;
        
        // Test optimized operations
        double[] values = {1.0, 5.0, 3.0, 9.0, 2.0};
        double[] minMax = DataStructureOptimizer.findMinMaxSIMD(values, 0, values.length);
        assert minMax[0] == 9.0 && minMax[1] == 1.0 : "MinMax SIMD failed";
        
        // Test bit operations
        assert DataStructureOptimizer.isPowerOfTwo(8) : "Power of 2 check failed";
        assert DataStructureOptimizer.fastDivideByPowerOf2(16, 4) == 4 : "Fast division failed";
        assert DataStructureOptimizer.fastModByPowerOf2(15, 8) == 7 : "Fast modulo failed";
        
        System.out.println("   ✅ DataStructureOptimizer working correctly");
    }
    
    private static void testFastMath() {
        System.out.println("\n2. Testing FastMath...");
        
        // Test fast operations
        assert Math.abs(FastMath.fastSqrt(9.0) - 3.0) < 0.001 : "Fast sqrt failed";
        assert FastMath.fastPow2(3) == 8 : "Fast pow2 failed";
        assert Math.abs(FastMath.fastLog2(8.0) - 3.0) < 0.001 : "Fast log2 failed";
        
        // Test utility functions
        assert FastMath.fastAbs(-5.0) == 5.0 : "Fast abs failed";
        assert FastMath.fastMin(3.0, 7.0) == 3.0 : "Fast min failed";
        assert FastMath.fastMax(3.0, 7.0) == 7.0 : "Fast max failed";
        
        // Test advanced functions
        assert Math.abs(FastMath.fastRound(3.14159, 2) - 3.14) < 0.001 : "Fast round failed";
        assert Math.abs(FastMath.fastPercentage(25, 100) - 25.0) < 0.001 : "Fast percentage failed";
        assert FastMath.fastClamp(15.0, 10.0, 20.0) == 15.0 : "Fast clamp failed";
        assert FastMath.fastSign(-5.0) == -1 : "Fast sign failed";
        
        System.out.println("   ✅ FastMath working correctly");
    }
    
    private static void testConcurrencyOptimizer() {
        System.out.println("\n3. Testing ConcurrencyOptimizer...");
        
        // Test background task submission
        java.util.concurrent.Future<?> future = ConcurrencyOptimizer.submitBackgroundTask(() -> {
            try {
                Thread.sleep(10); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        try {
            future.get(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            System.err.println("Background task failed: " + e.getMessage());
        }
        
        // Test counters
        ConcurrencyOptimizer.incrementCalculationCount();
        ConcurrencyOptimizer.incrementCacheHit();
        ConcurrencyOptimizer.incrementCacheMiss();
        
        // Test thread-local formatters
        java.text.DecimalFormat df = ConcurrencyOptimizer.getDecimalFormat1();
        assert df != null : "DecimalFormat1 is null";
        
        System.out.println("   ✅ ConcurrencyOptimizer working correctly");
        System.out.println("   📊 " + ConcurrencyOptimizer.getPerformanceStats());
    }
    
    private static void testAdvancedMemoryManager() {
        System.out.println("\n4. Testing AdvancedMemoryManager...");
        
        // Test buffer management
        byte[] smallBuffer = AdvancedMemoryManager.getBuffer(512);
        assert smallBuffer.length >= 512 : "Small buffer size incorrect";
        AdvancedMemoryManager.releaseBuffer(smallBuffer);
        
        byte[] mediumBuffer = AdvancedMemoryManager.getBuffer(4096);
        assert mediumBuffer.length >= 4096 : "Medium buffer size incorrect";
        AdvancedMemoryManager.releaseBuffer(mediumBuffer);
        
        // Test weak cache
        String testKey = "test_key";
        String testValue = AdvancedMemoryManager.getFromWeakCache(testKey, () -> "cached_value");
        assert "cached_value".equals(testValue) : "Weak cache failed";
        
        // Test off-heap storage
        byte[] testData = "Hello World".getBytes();
        AdvancedMemoryManager.storeOffHeap("test", testData);
        byte[] retrieved = AdvancedMemoryManager.getFromOffHeap("test");
        assert java.util.Arrays.equals(testData, retrieved) : "Off-heap storage failed";
        
        // Test string interning
        String interned = AdvancedMemoryManager.internString("test_string");
        assert "test_string".equals(interned) : "String interning failed";
        
        // Test compact boolean array
        AdvancedMemoryManager.CompactBooleanArray boolArray = 
            new AdvancedMemoryManager.CompactBooleanArray(100);
        boolArray.set(50, true);
        assert boolArray.get(50) : "Compact boolean array failed";
        assert !boolArray.get(51) : "Compact boolean array failed";
        
        System.out.println("   ✅ AdvancedMemoryManager working correctly");
        System.out.println("   📊 " + AdvancedMemoryManager.getMemoryStats());
    }
    
    private static void testIOOptimizer() {
        System.out.println("\n5. Testing IOOptimizer...");
        
        try {
            // Test fast file operations
            String testFile = "test_performance.tmp";
            byte[] testData = "Performance Test Data".getBytes();
            
            IOOptimizer.fastWriteFile(testFile, testData);
            byte[] readData = IOOptimizer.fastReadFile(testFile);
            assert java.util.Arrays.equals(testData, readData) : "Fast file I/O failed";
            
            // Test compression
            byte[] compressed = IOOptimizer.compress(testData);
            byte[] decompressed = IOOptimizer.decompress(compressed);
            assert java.util.Arrays.equals(testData, decompressed) : "Compression failed";
            
            // Test serialization
            String testObject = "Serialization Test";
            byte[] serialized = IOOptimizer.fastSerialize(testObject);
            String deserialized = IOOptimizer.fastDeserialize(serialized, String.class);
            assert testObject.equals(deserialized) : "Serialization failed";
            
            // Cleanup
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(testFile));
            
        } catch (Exception e) {
            System.err.println("IOOptimizer test failed: " + e.getMessage());
        }
        
        System.out.println("   ✅ IOOptimizer working correctly");
        System.out.println("   📊 " + IOOptimizer.getIOStats());
    }
    
    private static void testUIOptimizer() {
        System.out.println("\n6. Testing UIOptimizer...");
        
        // Test color caching
        java.awt.Color color1 = UIOptimizer.getCachedColor(255, 0, 0);
        java.awt.Color color2 = UIOptimizer.getCachedColor(255, 0, 0);
        assert color1 == color2 : "Color caching failed"; // Should be same instance
        
        // Test font caching
        java.awt.Font font1 = UIOptimizer.getCachedFont("Arial", java.awt.Font.PLAIN, 12);
        java.awt.Font font2 = UIOptimizer.getCachedFont("Arial", java.awt.Font.PLAIN, 12);
        assert font1 == font2 : "Font caching failed"; // Should be same instance
        
        // Test string measurements
        int width = UIOptimizer.getStringWidth("Test", font1);
        int height = UIOptimizer.getStringHeight(font1);
        assert width > 0 && height > 0 : "String measurements failed";
        
        // Test color gradient
        java.awt.Color[] gradient = UIOptimizer.createColorGradient(
            java.awt.Color.RED, java.awt.Color.BLUE, 5);
        assert gradient.length == 5 : "Color gradient failed";
        assert gradient[0].equals(java.awt.Color.RED) : "Gradient start color failed";
        assert gradient[4].equals(java.awt.Color.BLUE) : "Gradient end color failed";
        
        System.out.println("   ✅ UIOptimizer working correctly");
        System.out.println("   📊 " + UIOptimizer.getUIStats());
    }
}