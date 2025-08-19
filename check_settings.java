import com.biotak.config.LoggingConfiguration;
import com.biotak.debug.AdvancedLogger;

public class check_settings {
    public static void main(String[] args) {
        System.out.println("=== Checking Settings Configuration ===");
        
        // Initialize logging
        LoggingConfiguration.initialize();
        System.out.println("Current log level from config: " + LoggingConfiguration.getCurrentLogLevel().getName());
        
        // Test different log level values
        String[] testLevels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
        
        for (String levelName : testLevels) {
            try {
                AdvancedLogger.LogLevel level = AdvancedLogger.LogLevel.valueOf(levelName);
                System.out.println(levelName + " -> Level value: " + level.getLevel() + ", Name: " + level.getName());
            } catch (IllegalArgumentException e) {
                System.out.println(levelName + " -> INVALID");
            }
        }
        
        System.out.println("\n=== Testing Level Comparison ===");
        AdvancedLogger.LogLevel currentLevel = AdvancedLogger.LogLevel.INFO;
        AdvancedLogger.LogLevel debugLevel = AdvancedLogger.LogLevel.DEBUG;
        
        System.out.println("Current level (INFO): " + currentLevel.getLevel());
        System.out.println("DEBUG level: " + debugLevel.getLevel());
        System.out.println("DEBUG < INFO? " + (debugLevel.getLevel() < currentLevel.getLevel()));
        System.out.println("Should DEBUG messages appear? " + !(debugLevel.getLevel() < currentLevel.getLevel()));
    }
}
