package com.biotak.config;

import java.util.Properties;
import java.io.*;
import java.nio.file.*;

/**
 * Centralized Configuration Manager for Biotak
 * 
 * Features:
 * - Global settings management
 * - Performance tuning parameters
 * - Environment-specific configs
 * - Hot-reload capability
 * - Validation and defaults
 */
public class BiotakConfig {
    
    private static BiotakConfig instance;
    private Properties properties;
    private final String configFile = "biotak.properties";
    
    // Default configuration values
    public static class Defaults {
        // Performance settings
        public static final int DEFAULT_CACHE_SIZE = 1000;
        public static final int DEFAULT_THREAD_POOL_SIZE = 8;
        public static final double DEFAULT_MEMORY_THRESHOLD = 80.0;
        
        // Calculation settings
        public static final int DEFAULT_PRECISION = 5;
        public static final boolean DEFAULT_USE_FAST_MATH = true;
        public static final boolean DEFAULT_ENABLE_CACHING = true;
        
        // UI settings
        public static final boolean DEFAULT_SHOW_DEBUG = false;
        public static final int DEFAULT_REFRESH_RATE = 100;
        
        // Monitoring settings
        public static final int DEFAULT_MONITOR_INTERVAL = 5;
        public static final boolean DEFAULT_AUTO_EXPORT = true;
    }
    
    private BiotakConfig() {
        loadConfiguration();
    }
    
    public static synchronized BiotakConfig getInstance() {
        if (instance == null) {
            instance = new BiotakConfig();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        properties = new Properties();
        
        // Load default values first
        loadDefaults();
        
        // Try to load from file
        try {
            if (Files.exists(Paths.get(configFile))) {
                try (InputStream input = Files.newInputStream(Paths.get(configFile))) {
                    properties.load(input);
                    System.out.println("✅ Configuration loaded from: " + configFile);
                }
            } else {
                // Create default config file
                saveConfiguration();
                System.out.println("📝 Default configuration created: " + configFile);
            }
        } catch (IOException e) {
            System.err.println("⚠️  Error loading configuration, using defaults: " + e.getMessage());
        }
        
        validateConfiguration();
    }
    
    private void loadDefaults() {
        // Performance settings
        properties.setProperty("cache.size", String.valueOf(Defaults.DEFAULT_CACHE_SIZE));
        properties.setProperty("thread.pool.size", String.valueOf(Defaults.DEFAULT_THREAD_POOL_SIZE));
        properties.setProperty("memory.threshold", String.valueOf(Defaults.DEFAULT_MEMORY_THRESHOLD));
        
        // Calculation settings
        properties.setProperty("calculation.precision", String.valueOf(Defaults.DEFAULT_PRECISION));
        properties.setProperty("calculation.use.fast.math", String.valueOf(Defaults.DEFAULT_USE_FAST_MATH));
        properties.setProperty("calculation.enable.caching", String.valueOf(Defaults.DEFAULT_ENABLE_CACHING));
        
        // UI settings
        properties.setProperty("ui.show.debug", String.valueOf(Defaults.DEFAULT_SHOW_DEBUG));
        properties.setProperty("ui.refresh.rate", String.valueOf(Defaults.DEFAULT_REFRESH_RATE));
        
        // Monitoring settings
        properties.setProperty("monitor.interval", String.valueOf(Defaults.DEFAULT_MONITOR_INTERVAL));
        properties.setProperty("monitor.auto.export", String.valueOf(Defaults.DEFAULT_AUTO_EXPORT));
    }
    
    private void validateConfiguration() {
        // Validate cache size
        int cacheSize = getInt("cache.size");
        if (cacheSize < 100 || cacheSize > 10000) {
            System.err.println("⚠️  Invalid cache size: " + cacheSize + ", using default");
            properties.setProperty("cache.size", String.valueOf(Defaults.DEFAULT_CACHE_SIZE));
        }
        
        // Validate memory threshold
        double memoryThreshold = getDouble("memory.threshold");
        if (memoryThreshold < 50.0 || memoryThreshold > 95.0) {
            System.err.println("⚠️  Invalid memory threshold: " + memoryThreshold + ", using default");
            properties.setProperty("memory.threshold", String.valueOf(Defaults.DEFAULT_MEMORY_THRESHOLD));
        }
        
        // Add more validations as needed
    }
    
    public void saveConfiguration() {
        try (OutputStream output = Files.newOutputStream(Paths.get(configFile))) {
            properties.store(output, "Biotak Configuration - Auto-generated");
            System.out.println("💾 Configuration saved to: " + configFile);
        } catch (IOException e) {
            System.err.println("❌ Error saving configuration: " + e.getMessage());
        }
    }
    
    // Getter methods with type safety
    public String getString(String key) {
        return properties.getProperty(key);
    }
    
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public int getInt(String key) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Invalid integer value for " + key + ": " + properties.getProperty(key));
            return 0;
        }
    }
    
    public int getInt(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public double getDouble(String key) {
        try {
            return Double.parseDouble(properties.getProperty(key));
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Invalid double value for " + key + ": " + properties.getProperty(key));
            return 0.0;
        }
    }
    
    public double getDouble(String key, double defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    // Setter methods
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    public void setProperty(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }
    
    public void setProperty(String key, double value) {
        properties.setProperty(key, String.valueOf(value));
    }
    
    public void setProperty(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
    }
    
    // Hot reload capability
    public void reload() {
        System.out.println("🔄 Reloading configuration...");
        loadConfiguration();
    }
    
    // Configuration summary
    public void printConfiguration() {
        System.out.println("\n⚙️  === BIOTAK CONFIGURATION ===");
        System.out.println("Performance Settings:");
        System.out.println("  Cache Size: " + getInt("cache.size"));
        System.out.println("  Thread Pool Size: " + getInt("thread.pool.size"));
        System.out.println("  Memory Threshold: " + getDouble("memory.threshold") + "%");
        
        System.out.println("\nCalculation Settings:");
        System.out.println("  Precision: " + getInt("calculation.precision"));
        System.out.println("  Use Fast Math: " + getBoolean("calculation.use.fast.math"));
        System.out.println("  Enable Caching: " + getBoolean("calculation.enable.caching"));
        
        System.out.println("\nUI Settings:");
        System.out.println("  Show Debug: " + getBoolean("ui.show.debug"));
        System.out.println("  Refresh Rate: " + getInt("ui.refresh.rate") + "ms");
        
        System.out.println("\nMonitoring Settings:");
        System.out.println("  Monitor Interval: " + getInt("monitor.interval") + "s");
        System.out.println("  Auto Export: " + getBoolean("monitor.auto.export"));
        System.out.println("===============================\n");
    }
    
    // Get all properties for advanced usage
    public Properties getAllProperties() {
        return new Properties(properties);
    }
    
    // Environment-specific configuration
    public void loadEnvironmentConfig(String environment) {
        String envConfigFile = "biotak-" + environment + ".properties";
        
        try {
            if (Files.exists(Paths.get(envConfigFile))) {
                try (InputStream input = Files.newInputStream(Paths.get(envConfigFile))) {
                    Properties envProps = new Properties();
                    envProps.load(input);
                    
                    // Merge environment-specific properties
                    for (String key : envProps.stringPropertyNames()) {
                        properties.setProperty(key, envProps.getProperty(key));
                    }
                    
                    System.out.println("✅ Environment configuration loaded: " + envConfigFile);
                    validateConfiguration();
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️  Error loading environment config: " + e.getMessage());
        }
    }
}
