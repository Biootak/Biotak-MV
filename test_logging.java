import com.biotak.config.LoggingConfiguration;
import com.biotak.debug.AdvancedLogger;

public class test_logging {
    public static void main(String[] args) {
        System.out.println("=== Testing Logging Configuration ===");
        
        // Initialize logging
        LoggingConfiguration.initialize();
        
        // Print current configuration
        LoggingConfiguration.printConfiguration();
        
        // Test logging at different levels
        System.out.println("\n=== Testing Log Output ===");
        AdvancedLogger.debug("TestClass", "main", "This is a DEBUG message - should NOT appear if level is INFO");
        AdvancedLogger.info("TestClass", "main", "This is an INFO message - should appear");
        AdvancedLogger.warn("TestClass", "main", "This is a WARN message - should appear");
        AdvancedLogger.error("TestClass", "main", "This is an ERROR message - should appear");
        
        System.out.println("\n=== Current Log Level ===");
        System.out.println("Current log level: " + LoggingConfiguration.getCurrentLogLevel().getName());
        
        System.out.println("\n=== Logging Stats ===");
        System.out.println(AdvancedLogger.getLoggingStats());
    }
}
