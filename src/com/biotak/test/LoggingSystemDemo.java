package com.biotak.test;

import com.biotak.debug.AdvancedLogger;
import com.biotak.config.LoggingConfiguration;

/**
 * نمایش و تست سیستم لاگینگ یکپارچه Biotak
 * Biotak Unified Logging System Demonstration and Test
 */
public class LoggingSystemDemo {
    
    private static final String CLASS_NAME = "LoggingSystemDemo";
    
    public static void main(String[] args) {
        // Initialize logging configuration
        LoggingConfiguration.initialize();
        
        System.out.println("=== Biotak Unified Logging System Demo ===\n");
        
        // Print current configuration
        LoggingConfiguration.printConfiguration();
        System.out.println();
        
        // Demonstrate different log levels
        demonstrateLogLevels();
        
        // Demonstrate specialized logging
        demonstrateSpecializedLogging();
        
        // Demonstrate performance tracking
        demonstratePerformanceTracking();
        
        // Demonstrate method entry/exit logging
        demonstrateMethodTracing();
        
        // Demonstrate conditional logging
        demonstrateConditionalLogging();
        
        // Demonstrate state logging
        demonstrateStateLogging();
        
        // Show statistics
        showLoggingStatistics();
        
        System.out.println("\n=== Demo completed - Check log files in logs/ directory ===");
    }
    
    /**
     * Demonstrate different log levels
     */
    private static void demonstrateLogLevels() {
        System.out.println("--- Demonstrating Log Levels ---");
        
        AdvancedLogger.trace(CLASS_NAME, "demonstrateLogLevels", "This is a TRACE message");
        AdvancedLogger.debug(CLASS_NAME, "demonstrateLogLevels", "This is a DEBUG message");
        AdvancedLogger.info(CLASS_NAME, "demonstrateLogLevels", "This is an INFO message");
        AdvancedLogger.warn(CLASS_NAME, "demonstrateLogLevels", "This is a WARNING message");
        AdvancedLogger.error(CLASS_NAME, "demonstrateLogLevels", "This is an ERROR message");
        AdvancedLogger.fatal(CLASS_NAME, "demonstrateLogLevels", "This is a FATAL message");
        
        // With parameters
        AdvancedLogger.info(CLASS_NAME, "demonstrateLogLevels", 
            "Message with parameters: %s, %d, %.2f", "text", 42, 3.14);
        
        System.out.println();
    }
    
    /**
     * Demonstrate specialized logging methods
     */
    private static void demonstrateSpecializedLogging() {
        System.out.println("--- Demonstrating Specialized Logging ---");
        
        // Ruler logging
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, "calculateRuler", 
            "Ruler calculation completed successfully");
        
        // Performance logging
        AdvancedLogger.performance(CLASS_NAME, "demonstrateSpecializedLogging", 
            "Performance metric: Operation took 150ms");
        
        // Script logging simulation
        AdvancedLogger.powershell(AdvancedLogger.LogLevel.INFO, "DataBackup.ps1", 
            "PowerShell backup script executed successfully");
        
        AdvancedLogger.batch(AdvancedLogger.LogLevel.WARN, "CleanupTemp.bat", 
            "Batch cleanup script encountered warnings");
        
        AdvancedLogger.script(AdvancedLogger.LogLevel.DEBUG, "Python", "AnalyzeData.py", 
            "Python analysis script debug information");
        
        System.out.println();
    }
    
    /**
     * Demonstrate performance tracking
     */
    private static void demonstratePerformanceTracking() {
        System.out.println("--- Demonstrating Performance Tracking ---");
        
        // Fast operation
        AdvancedLogger.startPerformanceTracking("FastOperation");
        try {
            Thread.sleep(100); // Simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AdvancedLogger.endPerformanceTracking("FastOperation");
        
        // Slow operation (will trigger warning)
        AdvancedLogger.startPerformanceTracking("SlowOperation");
        try {
            Thread.sleep(1500); // Simulate slow work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AdvancedLogger.endPerformanceTracking("SlowOperation");
        
        // Using lambda approach
        String result = AdvancedLogger.withPerformanceLogging("LambdaOperation", () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Lambda operation completed";
        });
        
        AdvancedLogger.info(CLASS_NAME, "demonstratePerformanceTracking", 
            "Lambda result: %s", result);
        
        System.out.println();
    }
    
    /**
     * Demonstrate method entry/exit tracing
     */
    private static void demonstrateMethodTracing() {
        System.out.println("--- Demonstrating Method Tracing ---");
        
        // Set log level to TRACE temporarily to show enter/exit logs
        AdvancedLogger.LogLevel originalLevel = LoggingConfiguration.getCurrentLogLevel();
        AdvancedLogger.setLogLevel(AdvancedLogger.LogLevel.TRACE);
        
        sampleMethodWithTracing("parameter1", 123);
        
        // Restore original log level
        AdvancedLogger.setLogLevel(originalLevel);
        
        System.out.println();
    }
    
    /**
     * Sample method to demonstrate tracing
     */
    private static String sampleMethodWithTracing(String param1, int param2) {
        AdvancedLogger.enter(CLASS_NAME, "sampleMethodWithTracing", param1, param2);
        
        try {
            // Simulate some work
            String result = "Processed: " + param1 + " with value " + param2;
            
            AdvancedLogger.debug(CLASS_NAME, "sampleMethodWithTracing", 
                "Processing completed for: %s", param1);
            
            AdvancedLogger.exit(CLASS_NAME, "sampleMethodWithTracing", result);
            return result;
            
        } catch (Exception e) {
            AdvancedLogger.exception(CLASS_NAME, "sampleMethodWithTracing", e, 
                "Error processing parameters: %s, %d", param1, param2);
            throw e;
        }
    }
    
    /**
     * Demonstrate conditional logging
     */
    private static void demonstrateConditionalLogging() {
        System.out.println("--- Demonstrating Conditional Logging ---");
        
        boolean debugMode = true;
        boolean productionMode = false;
        
        // Log only if condition is true
        AdvancedLogger.logIf(debugMode, AdvancedLogger.LogLevel.DEBUG, 
            AdvancedLogger.Category.DEBUG, CLASS_NAME, "demonstrateConditionalLogging", 
            "This message appears only in debug mode");
        
        AdvancedLogger.logIf(productionMode, AdvancedLogger.LogLevel.INFO, 
            AdvancedLogger.Category.GENERAL, CLASS_NAME, "demonstrateConditionalLogging", 
            "This message would appear only in production mode");
        
        // Frequency-controlled logging
        for (int i = 0; i < 10; i++) {
            AdvancedLogger.logEveryN(3, AdvancedLogger.LogLevel.INFO, 
                AdvancedLogger.Category.GENERAL, CLASS_NAME, "demonstrateConditionalLogging", 
                "This message appears every 3rd call - iteration: %d", i);
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrate state logging
     */
    private static void demonstrateStateLogging() {
        System.out.println("--- Demonstrating State Logging ---");
        
        // Create sample object
        SampleObject obj = new SampleObject("TestName", 42, true);
        
        // Log object state
        AdvancedLogger.logState(CLASS_NAME, "demonstrateStateLogging", "sampleObject", obj);
        
        // Log null object
        AdvancedLogger.logState(CLASS_NAME, "demonstrateStateLogging", "nullObject", null);
        
        System.out.println();
    }
    
    /**
     * Show logging statistics
     */
    private static void showLoggingStatistics() {
        System.out.println("--- Logging Statistics ---");
        String stats = AdvancedLogger.getLoggingStats();
        System.out.println(stats);
        
        AdvancedLogger.info(CLASS_NAME, "showLoggingStatistics", "Statistics: %s", stats);
    }
    
    /**
     * Sample class for state logging demonstration
     */
    private static class SampleObject {
        private String name;
        private int value;
        private boolean active;
        
        public SampleObject(String name, int value, boolean active) {
            this.name = name;
            this.value = value;
            this.active = active;
        }
        
        // Getters would be here in a real class
    }
}
