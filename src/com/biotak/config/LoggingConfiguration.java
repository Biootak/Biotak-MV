package com.biotak.config;

import com.biotak.debug.AdvancedLogger;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * مدیریت کانفیگ سیستم لاگینگ یکپارچه
 * Unified Logging System Configuration Manager
 */
public final class LoggingConfiguration {
    
    private static final String DEFAULT_CONFIG_FILE = "biotak.properties";
    private static final String DEFAULT_LOG_DIR = "C:/Users/Fatemehkh/MotiveWave Extensions/logs/";
    
    private static Properties properties;
    private static boolean initialized = false;
    
    // Default values
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    private static final boolean DEFAULT_CONSOLE_ENABLED = true;
    private static final boolean DEFAULT_FILE_ENABLED = true;
    private static final boolean DEFAULT_COLOR_ENABLED = true;
    private static final boolean DEFAULT_PERFORMANCE_TRACKING = true;
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int DEFAULT_MAX_BACKUP_FILES = 5;
    
    private LoggingConfiguration() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Initialize logging configuration
     */
    public static void initialize() {
        initialize(DEFAULT_CONFIG_FILE);
    }
    
    /**
     * Initialize logging configuration with custom config file
     */
    public static void initialize(String configFile) {
        if (initialized) {
            return;
        }
        
        properties = new Properties();
        
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
            System.out.println("Loaded logging configuration from: " + configFile);
        } catch (IOException e) {
            System.err.println("Failed to load config file: " + configFile + " - Using defaults");
            // Use default properties
            setDefaultProperties();
        }
        
        // Apply configuration to AdvancedLogger
        applyLoggingConfiguration();
        
        initialized = true;
        
        // Log initialization success
        AdvancedLogger.info("LoggingConfiguration", "initialize", 
            "Logging system initialized successfully with config: %s", configFile);
    }
    
    /**
     * Set default properties when config file is not available
     */
    private static void setDefaultProperties() {
        properties.setProperty("logging.level", DEFAULT_LOG_LEVEL);
        properties.setProperty("logging.console.enabled", String.valueOf(DEFAULT_CONSOLE_ENABLED));
        properties.setProperty("logging.file.enabled", String.valueOf(DEFAULT_FILE_ENABLED));
        properties.setProperty("logging.color.enabled", String.valueOf(DEFAULT_COLOR_ENABLED));
        properties.setProperty("logging.performance.tracking", String.valueOf(DEFAULT_PERFORMANCE_TRACKING));
        properties.setProperty("logging.max.file.size", String.valueOf(DEFAULT_MAX_FILE_SIZE));
        properties.setProperty("logging.max.backup.files", String.valueOf(DEFAULT_MAX_BACKUP_FILES));
        properties.setProperty("logging.dir", DEFAULT_LOG_DIR);
    }
    
    /**
     * Apply configuration to AdvancedLogger
     */
    private static void applyLoggingConfiguration() {
        try {
            // Set log level
            String logLevelStr = getProperty("logging.level", DEFAULT_LOG_LEVEL);
            AdvancedLogger.LogLevel logLevel = parseLogLevel(logLevelStr);
            AdvancedLogger.setLogLevel(logLevel);
            
            // Set console output
            boolean consoleEnabled = getBooleanProperty("logging.console.enabled", DEFAULT_CONSOLE_ENABLED);
            AdvancedLogger.setConsoleOutput(consoleEnabled);
            
            // Set file output
            boolean fileEnabled = getBooleanProperty("logging.file.enabled", DEFAULT_FILE_ENABLED);
            AdvancedLogger.setFileOutput(fileEnabled);
            
            // Set color output
            boolean colorEnabled = getBooleanProperty("logging.color.enabled", DEFAULT_COLOR_ENABLED);
            AdvancedLogger.setColorOutput(colorEnabled);
            
        } catch (Exception e) {
            System.err.println("Error applying logging configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parse log level string to LogLevel enum
     */
    private static AdvancedLogger.LogLevel parseLogLevel(String levelStr) {
        try {
            return AdvancedLogger.LogLevel.valueOf(levelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid log level: " + levelStr + " - Using INFO");
            return AdvancedLogger.LogLevel.INFO;
        }
    }
    
    /**
     * Get string property with default value
     */
    public static String getProperty(String key, String defaultValue) {
        if (properties == null) {
            initialize();
        }
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get boolean property with default value
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Get integer property with default value
     */
    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for " + key + ": " + value + " - Using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get long property with default value
     */
    public static long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid long value for " + key + ": " + value + " - Using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Reload configuration
     */
    public static void reload() {
        initialized = false;
        initialize();
    }
    
    /**
     * Get current log level
     */
    public static AdvancedLogger.LogLevel getCurrentLogLevel() {
        String levelStr = getProperty("logging.level", DEFAULT_LOG_LEVEL);
        return parseLogLevel(levelStr);
    }
    
    /**
     * Check if console logging is enabled
     */
    public static boolean isConsoleEnabled() {
        return getBooleanProperty("logging.console.enabled", DEFAULT_CONSOLE_ENABLED);
    }
    
    /**
     * Check if file logging is enabled
     */
    public static boolean isFileEnabled() {
        return getBooleanProperty("logging.file.enabled", DEFAULT_FILE_ENABLED);
    }
    
    /**
     * Check if color output is enabled
     */
    public static boolean isColorEnabled() {
        return getBooleanProperty("logging.color.enabled", DEFAULT_COLOR_ENABLED);
    }
    
    /**
     * Check if performance tracking is enabled
     */
    public static boolean isPerformanceTrackingEnabled() {
        return getBooleanProperty("logging.performance.tracking", DEFAULT_PERFORMANCE_TRACKING);
    }
    
    /**
     * Get log directory
     */
    public static String getLogDirectory() {
        return getProperty("logging.dir", DEFAULT_LOG_DIR);
    }
    
    /**
     * Get maximum file size
     */
    public static long getMaxFileSize() {
        return getLongProperty("logging.max.file.size", DEFAULT_MAX_FILE_SIZE);
    }
    
    /**
     * Get maximum backup files
     */
    public static int getMaxBackupFiles() {
        return getIntProperty("logging.max.backup.files", DEFAULT_MAX_BACKUP_FILES);
    }
    
    /**
     * Check if PowerShell logging is enabled
     */
    public static boolean isPowerShellLoggingEnabled() {
        return getBooleanProperty("logging.powershell.enabled", true);
    }
    
    /**
     * Check if Batch logging is enabled
     */
    public static boolean isBatchLoggingEnabled() {
        return getBooleanProperty("logging.batch.enabled", true);
    }
    
    /**
     * Check if script monitoring is enabled
     */
    public static boolean isScriptMonitoringEnabled() {
        return getBooleanProperty("logging.script.monitoring", true);
    }
    
    /**
     * Print current configuration
     */
    public static void printConfiguration() {
        if (!initialized) {
            initialize();
        }
        
        System.out.println("=== Biotak Logging Configuration ===");
        System.out.println("Log Level: " + getCurrentLogLevel().getName());
        System.out.println("Console Enabled: " + isConsoleEnabled());
        System.out.println("File Enabled: " + isFileEnabled());
        System.out.println("Color Enabled: " + isColorEnabled());
        System.out.println("Performance Tracking: " + isPerformanceTrackingEnabled());
        System.out.println("Log Directory: " + getLogDirectory());
        System.out.println("Max File Size: " + getMaxFileSize() + " bytes");
        System.out.println("Max Backup Files: " + getMaxBackupFiles());
        System.out.println("PowerShell Logging: " + isPowerShellLoggingEnabled());
        System.out.println("Batch Logging: " + isBatchLoggingEnabled());
        System.out.println("Script Monitoring: " + isScriptMonitoringEnabled());
        System.out.println("=====================================");
    }
}
