package com.biotak.util;

import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.Enums.IntervalType;

/**
 * Class for testing the TimeframeUtil functionality,
 * especially for non-fractal timeframes.
 */
public class TimeframeUtilTester {

    public static void main(String[] args) {
        System.out.println("=== TIMEFRAME UTIL TESTER (UPDATED FOR EXACT CALCULATIONS) ===");
        System.out.println("This test demonstrates the improved calculation of pattern and trigger timeframes");
        System.out.println("for non-fractal timeframes, ensuring mathematical precision (1/4 and 1/16 ratios).\n");
        
        // Test some standard timeframes
        testTimeframe(BarSize.getBarSize(5));    // M5
        testTimeframe(BarSize.getBarSize(15));   // M15
        testTimeframe(BarSize.getBarSize(60));   // H1
        
        System.out.println("\n=== NON-FRACTAL TIMEFRAMES ===\n");
        
        // Test non-fractal timeframes that previously had issues
        testTimeframe(BarSize.getBarSize(20));   // M20
        testTimeframe(BarSize.getBarSize(45));   // M45
        testTimeframe(BarSize.getBarSize(90));   // M90 (1.5 hours)
        
        // Additional test cases
        testTimeframe(BarSize.getBarSize(17));   // M17 (a prime number)
        testTimeframe(BarSize.getBarSize(23));   // M23 (another prime number)
        testTimeframe(BarSize.getBarSize(120));  // H2
        testTimeframe(BarSize.getBarSize(180));  // H3
    }
    
    private static void testTimeframe(BarSize barSize) {
        String timeframeStr = TimeframeUtil.getStandardTimeframeString(barSize);
        
        // Get string representations
        String patternTimeframe = TimeframeUtil.getPatternTimeframeString(barSize);
        String triggerTimeframe = TimeframeUtil.getTriggerTimeframeString(barSize);
        
        // Get actual BarSize objects
        BarSize patternBarSize = TimeframeUtil.getPatternBarSize(barSize);
        BarSize triggerBarSize = TimeframeUtil.getTriggerBarSize(barSize);
        
        // Get minutes for verification
        int minutes = getTotalMinutes(barSize);
        int patternMinutes = Math.max(1, minutes / 4);
        int triggerMinutes = Math.max(1, minutes / 16);
        
        // Get percentage values
        double timeframePercentage = TimeframeUtil.getTimeframePercentage(barSize);
        double patternPercentage = TimeframeUtil.getTimeframePercentage(patternBarSize);
        double triggerPercentage = TimeframeUtil.getTimeframePercentage(triggerBarSize);
        
        // Print detailed information
        System.out.println("تایم‌فریم: " + timeframeStr + " (" + minutes + " دقیقه)");
        System.out.println("  درصد تایم‌فریم: " + String.format("%.5f%%", timeframePercentage));
        
        System.out.println("  تایم‌فریم الگو (Pattern): " + patternTimeframe + 
                           " (" + patternMinutes + " دقیقه = " + minutes + "/4)");
        System.out.println("    درصد تایم‌فریم الگو: " + String.format("%.5f%%", patternPercentage));
        
        System.out.println("  تایم‌فریم اشاره‌گر (Trigger): " + triggerTimeframe + 
                           " (" + triggerMinutes + " دقیقه = " + minutes + "/16)");
        System.out.println("    درصد تایم‌فریم اشاره‌گر: " + String.format("%.5f%%", triggerPercentage));
        
        // Verify calculations
        System.out.println("  بررسی محاسبات فراکتالی:");
        System.out.println("    - نسبت الگو به ساختار: " + String.format("%.2f", (double)patternMinutes / minutes) + 
                           " (انتظار: 0.25)");
        System.out.println("    - نسبت اشاره‌گر به ساختار: " + String.format("%.2f", (double)triggerMinutes / minutes) + 
                           " (انتظار: 0.0625)");
        System.out.println("    - نسبت درصدی الگو به ساختار: " + String.format("%.2f", patternPercentage / timeframePercentage) + 
                           " (انتظار: ~0.50)");
        System.out.println("    - نسبت درصدی اشاره‌گر به ساختار: " + String.format("%.2f", triggerPercentage / timeframePercentage) + 
                           " (انتظار: ~0.25)");
        
        System.out.println("----------------------------------------------------------");
    }
    
    private static int getTotalMinutes(BarSize barSize) {
        int interval = barSize.getInterval();
        IntervalType type = barSize.getIntervalType();
        
        switch (type) {
            case MINUTE:
                return interval;
            case HOUR:
                return interval * 60;
            case DAY:
                return interval * 24 * 60;
            case WEEK:
                return interval * 7 * 24 * 60;
            default:
                return interval;
        }
    }
} 