package com.biotak.test;

import com.biotak.util.*;
import com.biotak.enums.*;
import com.biotak.core.*;

/**
 * JDK 24 and MotiveWave 7 Compatibility Test
 */
public class CompatibilityTest {
    
    public static void main(String[] args) {
        System.out.println("=== JDK 24 and MotiveWave 7 Compatibility Test ===");
        
        // Check Java version
        testJavaVersion();
        
        // Test Records (JDK 14+)
        testRecords();
        
        // Test Switch Expressions (JDK 14+)
        testSwitchExpressions();
        
        // Test Text Blocks (JDK 15+)
        testTextBlocks();
        
        // Test Pattern Matching (JDK 17+)
        testPatternMatching();
        
        // Test core classes
        testCoreClasses();
        
        System.out.println("=== All tests completed successfully! ===");
    }
    
    private static void testJavaVersion() {
        System.out.println("\n1. Checking Java version...");
        String version = System.getProperty("java.version");
        System.out.println("   Java version: " + version);
        
        // Check if JDK 24 or higher
        String[] parts = version.split("\\.");
        int majorVersion = Integer.parseInt(parts[0]);
        
        if (majorVersion >= 24) {
            System.out.println("   [OK] JDK 24+ detected");
        } else {
            System.out.println("   [WARN] JDK " + majorVersion + " in use, JDK 24 recommended");
        }
    }
    
    private static void testRecords() {
        System.out.println("\n2. Testing Records...");
        
        // Test FractalUtil.THBundle which is a record
        var bundle = new FractalUtil.THBundle(1.0, 2.0, 3.0, 4.0, 5.0);
        
        assert bundle.th() == 1.0 : "Record field access failed";
        assert bundle.pattern() == 2.0 : "Record field access failed";
        assert bundle.trigger() == 3.0 : "Record field access failed";
        assert bundle.structure() == 4.0 : "Record field access failed";
        assert bundle.higherPattern() == 5.0 : "Record field access failed";
        
        System.out.println("   [OK] Records working correctly");
    }
    
    private static void testSwitchExpressions() {
        System.out.println("\n3. Testing Switch Expressions...");
        
        // Test switch expression with enum
        StepCalculationMode mode = StepCalculationMode.TH_STEP;
        
        String result = switch (mode) {
            case TH_STEP -> "TH-Based";
            case SS_LS_STEP -> "SS/LS-Based";
            case CONTROL_STEP -> "Control-Based";
            case M_STEP -> "M-Based";
        };
        
        assert "TH-Based".equals(result) : "Switch expression failed";
        
        System.out.println("   [OK] Switch Expressions working correctly");
    }
    
    private static void testTextBlocks() {
        System.out.println("\n4. Testing Text Blocks...");
        
        // Test text block
        String textBlock = """
                This is a text block
                written in multiple lines
                and supported by JDK 15+
                """;
        
        assert textBlock.contains("text block") : "Text block failed";
        assert textBlock.contains("multiple lines") : "Text block failed";
        
        System.out.println("   [OK] Text Blocks working correctly");
    }
    
    private static void testPatternMatching() {
        System.out.println("\n5. Testing Pattern Matching...");
        
        // Test instanceof pattern matching
        Object obj = "Test String";
        
        if (obj instanceof String str && str.length() > 5) {
            assert str.equals("Test String") : "Pattern matching failed";
            System.out.println("   [OK] Pattern Matching working correctly");
        } else {
            throw new AssertionError("Pattern matching failed");
        }
    }
    
    private static void testCoreClasses() {
        System.out.println("\n6. Testing core classes...");
        
        // Test Logger
        Logger.info("Logger test");
        
        // Test StringUtils
        String formatted = StringUtils.format1f(3.14159);
        assert "3.1".equals(formatted) : "StringUtils failed";
        
        // Test FastMath
        double sqrt = FastMath.fastSqrt(9.0);
        assert Math.abs(sqrt - 3.0) < 0.001 : "FastMath failed";
        
        // Test EnumUtil
        THStartPointType startPoint = EnumUtil.safeEnum(
            THStartPointType.class, "MIDPOINT", THStartPointType.CUSTOM_PRICE);
        assert startPoint == THStartPointType.MIDPOINT : "EnumUtil failed";
        
        // Test ComputationCache
        ComputationCache.cachePercentage("TEST", 0.05);
        Double cached = ComputationCache.getCachedPercentage("TEST");
        assert cached != null && cached == 0.05 : "ComputationCache failed";
        
        System.out.println("   [OK] All core classes working correctly");
    }
}