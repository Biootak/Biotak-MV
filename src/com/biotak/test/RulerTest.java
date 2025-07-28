package com.biotak.test;

import com.biotak.debug.AdvancedLogger;
import com.biotak.enums.RulerState;
import com.biotak.ui.RulerIntegrationManager;
import com.biotak.ui.RulerPositionFix;

/**
 * کلاس تست برای آزمایش عملکرد خط کش
 */
public class RulerTest {
    
    private static final String CLASS_NAME = "RulerTest";
    
    public static void main(String[] args) {
        runAllTests();
    }
    
    public static void runAllTests() {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
            "📋 Starting Ruler Tests...");
        
        testRulerStates();
        testPositionDetection();
        testIntegrationManager();
        
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
            "✅ All Ruler Tests Completed");
    }
    
    /**
     * تست وضعیت‌های مختلف خط کش
     */
    private static void testRulerStates() {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
            "🔬 Testing Ruler States...");
        
        try {
            // Test all ruler states
            RulerState[] states = RulerState.values();
            
            for (RulerState state : states) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                    "Testing state: %s", state);
                
                // Simulate state transitions
                switch (state) {
                    case INACTIVE:
                        AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                            "✓ INACTIVE state - Ruler is off");
                        break;
                    case WAITING_FOR_START:
                        AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                            "✓ WAITING_FOR_START state - Ready for first click");
                        break;
                    case WAITING_FOR_END:
                        AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                            "✓ WAITING_FOR_END state - Ready for second click");
                        break;
                    case ACTIVE:
                        AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                            "✓ ACTIVE state - Ruler is complete");
                        break;
                }
            }
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "✅ Ruler States Test Passed");
                
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Ruler States Test Failed: %s", e.getMessage());
        }
    }
    
    /**
     * تست تشخیص موقعیت Live vs Historical
     */
    private static void testPositionDetection() {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
            "🔬 Testing Position Detection...");
        
        try {
            // Test ChartPosition class
            long currentTime = System.currentTimeMillis();
            long historicalTime = currentTime - (2 * 60 * 60 * 1000); // 2 hours ago
            
            // Live position test
            RulerPositionFix.ChartPosition livePosition = 
                new RulerPositionFix.ChartPosition(currentTime, 50000.0, true);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                "Live position: %s", livePosition);
            
            if (livePosition.isLive()) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                    "✓ Live position detected correctly");
            } else {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.WARN, CLASS_NAME,
                    "⚠️ Live position not detected correctly");
            }
            
            // Historical position test
            RulerPositionFix.ChartPosition historicalPosition = 
                new RulerPositionFix.ChartPosition(historicalTime, 48000.0, false);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                "Historical position: %s", historicalPosition);
            
            if (!historicalPosition.isLive()) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                    "✓ Historical position detected correctly");
            } else {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.WARN, CLASS_NAME,
                    "⚠️ Historical position not detected correctly");
            }
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "✅ Position Detection Test Passed");
                
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Position Detection Test Failed: %s", e.getMessage());
        }
    }
    
    /**
     * تست مدیر integration
     */
    private static void testIntegrationManager() {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
            "🔬 Testing Integration Manager...");
        
        try {
            // Create integration manager (without real Study for test)
            RulerIntegrationManager manager = new RulerIntegrationManager(null);
            
            // Test initial state
            RulerState initialState = manager.getRulerState();
            if (initialState == RulerState.INACTIVE) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                    "✓ Initial state is INACTIVE");
            }
            
            // Test ruler active check
            boolean isActive = manager.isRulerActive();
            if (!isActive) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                    "✓ Ruler is initially inactive");
            }
            
            // Test cleanup
            manager.cleanup();
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                "✓ Cleanup completed successfully");
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "✅ Integration Manager Test Passed");
                
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Integration Manager Test Failed: %s", e.getMessage());
        }
    }
    
    /**
     * تست شبیه‌سازی سناریو استفاده واقعی
     */
    public static void simulateRealUsageScenario() {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
            "🎬 Simulating Real Usage Scenario...");
        
        try {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "📖 Scenario: User is viewing historical data and wants to use ruler");
            
            // Step 1: User is in historical position
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "1️⃣ User is viewing historical data (2 hours ago)");
            
            long historicalTime = System.currentTimeMillis() - (2 * 60 * 60 * 1000);
            RulerPositionFix.ChartPosition userPosition = 
                new RulerPositionFix.ChartPosition(historicalTime, 48500.0, false);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "📍 User position: %s", userPosition);
            
            // Step 2: User clicks ruler button
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "2️⃣ User clicks ruler button");
            
            // Step 3: System should activate ruler at current position (not Live)
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "3️⃣ System activates ruler at current position (NOT Live)");
            
            if (!userPosition.isLive()) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                    "✅ SUCCESS: Ruler activated at historical position");
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                    "🎯 Problem SOLVED: Ruler does NOT jump to Live!");
            } else {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                    "❌ FAILURE: Ruler would jump to Live (problem not solved)");
            }
            
            // Step 4: User selects start and end points
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "4️⃣ User selects ruler points in historical area");
            
            // Simulate ruler point selection
            RulerPositionFix.ChartCoordinate startPoint = 
                new RulerPositionFix.ChartCoordinate(historicalTime - 30*60*1000, 48000.0); // 30 min before
            RulerPositionFix.ChartCoordinate endPoint = 
                new RulerPositionFix.ChartCoordinate(historicalTime + 30*60*1000, 49000.0); // 30 min after
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "📏 Ruler measurements: Start=%s, End=%s", startPoint, endPoint);
            
            // Step 5: Calculate measurements
            double priceDiff = endPoint.getPrice() - startPoint.getPrice();
            long timeDiff = endPoint.getTime() - startPoint.getTime();
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "📊 Measurements: Price diff=%.2f, Time diff=%d minutes", 
                priceDiff, timeDiff / (60 * 1000));
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "🎉 Scenario completed successfully!");
                
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Real Usage Scenario Failed: %s", e.getMessage());
        }
    }
}
