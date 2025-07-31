package com.biotak.debug;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.nio.file.Files;

/**
 * سیستم لاگینگ حرفه‌ای برای دیباگ سریع و دقیق
 * Advanced Professional Logging System for Quick and Accurate Debugging
 */
public final class AdvancedLogger {
    
    // =========================== CONFIGURATION ===========================
    private static final String LOG_DIR = "C:/Users/fatemeh/IdeaProject/Biotak/logs/";
    private static final String MAIN_LOG_FILE = LOG_DIR + "biotak_main.log";
    private static final String ERROR_LOG_FILE = LOG_DIR + "biotak_errors.log";
    private static final String DEBUG_LOG_FILE = LOG_DIR + "biotak_debug.log";
    private static final String PERFORMANCE_LOG_FILE = LOG_DIR + "biotak_performance.log";
    private static final String RULER_LOG_FILE = LOG_DIR + "biotak_ruler.log";
    private static final String SCRIPT_LOG_FILE = LOG_DIR + "biotak_scripts.log";
    private static final String POWERSHELL_LOG_FILE = LOG_DIR + "biotak_powershell.log";
    private static final String BATCH_LOG_FILE = LOG_DIR + "biotak_batch.log";
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_BACKUP_FILES = 5;
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    // =========================== LOG LEVELS ===========================
    public enum LogLevel {
        TRACE(0, "TRACE", "\u001B[37m"),   // سفید - White
        DEBUG(1, "DEBUG", "\u001B[36m"),   // آبی فیروزه‌ای - Cyan  
        INFO(2, "INFO", "\u001B[32m"),     // سبز - Green
        WARN(3, "WARN", "\u001B[33m"),     // زرد - Yellow
        ERROR(4, "ERROR", "\u001B[31m"),   // قرمز - Red
        FATAL(5, "FATAL", "\u001B[35m");   // بنفش - Magenta
        
        private final int level;
        private final String name;
        private final String colorCode;
        
        LogLevel(int level, String name, String colorCode) {
            this.level = level;
            this.name = name;
            this.colorCode = colorCode;
        }
        
        public int getLevel() { return level; }
        public String getName() { return name; }
        public String getColorCode() { return colorCode; }
    }
    
    // =========================== CATEGORIES ===========================
    public enum Category {
        GENERAL("GEN", MAIN_LOG_FILE),
        RULER("RUL", RULER_LOG_FILE),
        UI("UI", MAIN_LOG_FILE),
        CALCULATION("CALC", MAIN_LOG_FILE),
        PERFORMANCE("PERF", PERFORMANCE_LOG_FILE),
        ERROR("ERR", ERROR_LOG_FILE),
        DEBUG("DBG", DEBUG_LOG_FILE),
        SCRIPT("SCR", SCRIPT_LOG_FILE),
        POWERSHELL("PS", POWERSHELL_LOG_FILE),
        BATCH("BAT", BATCH_LOG_FILE);
        
        private final String code;
        private final String defaultFile;
        
        Category(String code, String defaultFile) {
            this.code = code;
            this.defaultFile = defaultFile;
        }
        
        public String getCode() { return code; }
        public String getDefaultFile() { return defaultFile; }
    }
    
    // =========================== STATE MANAGEMENT ===========================
    private static volatile LogLevel currentLogLevel = LogLevel.INFO;
    private static volatile boolean colorOutput = true;
    private static volatile boolean fileOutput = true;
    private static volatile boolean consoleOutput = true;
    
    // Performance tracking
    private static final Map<String, PerformanceTracker> performanceTrackers = new ConcurrentHashMap<>();
    private static final AtomicLong logCounter = new AtomicLong(0);
    
    // Thread safety
    private static final Object LOCK = new Object();
    
    static {
        // Initialize log directory
        createLogDirectory();
    }
    
    // =========================== CORE LOGGING METHODS ===========================
    
    /**
     * Main logging method - همه متدهای لاگینگ به اینجا می‌رسند
     */
    public static void log(LogLevel level, Category category, String className, String methodName, String message, Object... args) {
        if (level.getLevel() < currentLogLevel.getLevel()) {
            return;
        }
        
        try {
            String formattedMessage = formatMessage(level, category, className, methodName, message, args);
            
            if (consoleOutput) {
                System.out.println(formattedMessage);
            }
            
            if (fileOutput) {
                writeToFile(category.getDefaultFile(), formattedMessage);
                
                // Write errors to error log as well
                if (level == LogLevel.ERROR || level == LogLevel.FATAL) {
                    writeToFile(ERROR_LOG_FILE, formattedMessage);
                }
            }
            
            logCounter.incrementAndGet();
            
        } catch (Exception e) {
            System.err.println("LOGGING ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // =========================== CONVENIENCE METHODS ===========================
    
    public static void trace(String className, String methodName, String message, Object... args) {
        log(LogLevel.TRACE, Category.GENERAL, className, methodName, message, args);
    }
    
    public static void debug(String className, String methodName, String message, Object... args) {
        log(LogLevel.DEBUG, Category.DEBUG, className, methodName, message, args);
    }
    
    public static void info(String className, String methodName, String message, Object... args) {
        log(LogLevel.INFO, Category.GENERAL, className, methodName, message, args);
    }
    
    public static void warn(String className, String methodName, String message, Object... args) {
        log(LogLevel.WARN, Category.GENERAL, className, methodName, message, args);
    }
    
    public static void error(String className, String methodName, String message, Object... args) {
        log(LogLevel.ERROR, Category.ERROR, className, methodName, message, args);
    }
    
    public static void fatal(String className, String methodName, String message, Object... args) {
        log(LogLevel.FATAL, Category.ERROR, className, methodName, message, args);
    }
    
    // =========================== SPECIALIZED LOGGING METHODS ===========================
    
    /**
     * Ruler-specific logging - برای دیباگ مشکلات خط‌کش
     */
    public static void ruler(LogLevel level, String methodName, String message, Object... args) {
        log(level, Category.RULER, "RulerSystem", methodName, message, args);
    }
    
    /**
     * Performance logging - برای مانیتورینگ عملکرد
     */
    public static void performance(String className, String methodName, String message, Object... args) {
        log(LogLevel.INFO, Category.PERFORMANCE, className, methodName, message, args);
    }
    
    /**
     * PowerShell script logging - برای لاگ اسکریپت‌های پاورشل
     */
    public static void powershell(LogLevel level, String scriptName, String message, Object... args) {
        log(level, Category.POWERSHELL, "PowerShell", scriptName, message, args);
    }
    
    /**
     * Batch script logging - برای لاگ اسکریپت‌های بچ
     */
    public static void batch(LogLevel level, String scriptName, String message, Object... args) {
        log(level, Category.BATCH, "Batch", scriptName, message, args);
    }
    
    /**
     * General script logging - برای لاگ عمومی اسکریپت‌ها
     */
    public static void script(LogLevel level, String scriptType, String scriptName, String message, Object... args) {
        log(level, Category.SCRIPT, scriptType, scriptName, message, args);
    }
    
    /**
     * Method entry logging - شروع متد
     */
    public static void enter(String className, String methodName, Object... params) {
        if (currentLogLevel.getLevel() <= LogLevel.TRACE.getLevel()) {
            String paramStr = params.length > 0 ? " | Params: " + Arrays.toString(params) : "";
            log(LogLevel.TRACE, Category.DEBUG, className, methodName, ">>> ENTER" + paramStr);
        }
    }
    
    /**
     * Method exit logging - خروج از متد
     */
    public static void exit(String className, String methodName, Object result) {
        if (currentLogLevel.getLevel() <= LogLevel.TRACE.getLevel()) {
            String resultStr = result != null ? " | Result: " + result : "";
            log(LogLevel.TRACE, Category.DEBUG, className, methodName, "<<< EXIT" + resultStr);
        }
    }
    
    /**
     * Exception logging - لاگ استثناها
     */
    public static void exception(String className, String methodName, Throwable throwable, String message, Object... args) {
        String fullMessage = String.format(message, args) + " | Exception: " + throwable.getMessage();
        log(LogLevel.ERROR, Category.ERROR, className, methodName, fullMessage);
        
        // Write full stack trace to file
        if (fileOutput) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                writeToFile(ERROR_LOG_FILE, sw.toString());
            } catch (Exception e) {
                System.err.println("Failed to write stack trace: " + e.getMessage());
            }
        }
    }
    
    // =========================== PERFORMANCE TRACKING ===========================
    
    /**
     * Start performance tracking
     */
    public static void startPerformanceTracking(String operation) {
        performanceTrackers.put(operation, new PerformanceTracker(operation));
        performance("PerformanceTracker", "start", "Started tracking: %s", operation);
    }
    
    /**
     * End performance tracking
     */
    public static void endPerformanceTracking(String operation) {
        PerformanceTracker tracker = performanceTrackers.remove(operation);
        if (tracker != null) {
            long duration = tracker.end();
            performance("PerformanceTracker", "end", "Operation '%s' took %d ms", operation, duration);
            
            // اگر عملیات خیلی طول کشید، هشدار بده
            if (duration > 1000) { // بیش از 1 ثانیه
                warn("PerformanceTracker", "end", "SLOW OPERATION: '%s' took %d ms", operation, duration);
            }
        }
    }
    
    /**
     * Log with performance measurement
     */
    public static <T> T withPerformanceLogging(String operation, Supplier<T> supplier) {
        startPerformanceTracking(operation);
        try {
            return supplier.get();
        } finally {
            endPerformanceTracking(operation);
        }
    }
    
    // =========================== CONDITIONAL LOGGING ===========================
    
    /**
     * Log only if condition is true
     */
    public static void logIf(boolean condition, LogLevel level, Category category, String className, String methodName, String message, Object... args) {
        if (condition) {
            log(level, category, className, methodName, message, args);
        }
    }
    
    /**
     * Log with frequency control - فقط هر n بار لاگ کن
     */
    private static final Map<String, AtomicLong> frequencyCounters = new ConcurrentHashMap<>();
    
    public static void logEveryN(int n, LogLevel level, Category category, String className, String methodName, String message, Object... args) {
        String key = className + "." + methodName + "." + message;
        AtomicLong counter = frequencyCounters.computeIfAbsent(key, k -> new AtomicLong(0));
        
        if (counter.incrementAndGet() % n == 0) {
            log(level, category, className, methodName, message + " [Count: " + counter.get() + "]", args);
        }
    }
    
    // =========================== STATE INSPECTION ===========================
    
    /**
     * Log object state - وضعیت آبجکت را لاگ کن
     */
    public static void logState(String className, String methodName, String objectName, Object object) {
        if (object == null) {
            debug(className, methodName, "State of %s: NULL", objectName);
            return;
        }
        
        StringBuilder state = new StringBuilder();
        state.append("State of ").append(objectName).append(": ");
        
        try {
            // Use reflection to get field values
            java.lang.reflect.Field[] fields = object.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(object);
                state.append(field.getName()).append("=").append(value).append(", ");
            }
        } catch (Exception e) {
            state.append("Error reading state: ").append(e.getMessage());
        }
        
        debug(className, methodName, state.toString());
    }
    
    // =========================== CONFIGURATION METHODS ===========================
    
    public static void setLogLevel(LogLevel level) {
        currentLogLevel = level;
        info("AdvancedLogger", "setLogLevel", "Log level changed to: %s", level.getName());
    }
    
    public static void setColorOutput(boolean enabled) {
        colorOutput = enabled;
    }
    
    public static void setFileOutput(boolean enabled) {
        fileOutput = enabled;
    }
    
    public static void setConsoleOutput(boolean enabled) {
        consoleOutput = enabled;
    }
    
    // =========================== UTILITY METHODS ===========================
    
    /**
     * Format log message
     */
    private static String formatMessage(LogLevel level, Category category, String className, String methodName, String message, Object... args) {
        String timestamp = TIMESTAMP_FORMAT.format(new Date());
        String threadName = Thread.currentThread().getName();
        String formattedMessage = args.length > 0 ? String.format(message, args) : message;
        
        String colorStart = colorOutput ? level.getColorCode() : "";
        String colorEnd = colorOutput ? "\u001B[0m" : "";
        
        return String.format("%s%s [%s] [%s] [%s] %s.%s() - %s%s",
                colorStart,
                timestamp,
                level.getName(),
                category.getCode(),
                threadName,
                className,
                methodName,
                formattedMessage,
                colorEnd);
    }
    
    /**
     * Write to file with rotation
     */
    private static void writeToFile(String filename, String message) {
        synchronized (LOCK) {
            try {
                File file = new File(filename);
                
                // Check if rotation is needed
                if (file.exists() && file.length() > MAX_FILE_SIZE) {
                    rotateLogFile(file);
                }
                
                try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                    writer.println(message);
                }
                
            } catch (IOException e) {
                System.err.println("Failed to write to log file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Rotate log file
     */
    private static void rotateLogFile(File logFile) {
        try {
            String baseName = logFile.getAbsolutePath();
            String timestamp = FILE_DATE_FORMAT.format(new Date());
            String rotatedName = baseName + "." + timestamp;
            
            File rotatedFile = new File(rotatedName);
            boolean renamed = logFile.renameTo(rotatedFile);
            
            if (!renamed) {
                // If rename fails, copy and delete
                Files.copy(logFile.toPath(), rotatedFile.toPath());
                logFile.delete();
            }
            
            // Clean up old backup files
            cleanupOldBackups(baseName);
            
        } catch (Exception e) {
            System.err.println("Failed to rotate log file: " + e.getMessage());
        }
    }
    
    /**
     * Clean up old backup files
     */
    private static void cleanupOldBackups(String baseName) {
        try {
            File dir = new File(baseName).getParentFile();
            String fileName = new File(baseName).getName();
            
            File[] backups = dir.listFiles((d, name) -> 
                name.startsWith(fileName + ".") && name.matches(".*\\.\\d{8}_\\d{6}"));
            
            if (backups != null && backups.length > MAX_BACKUP_FILES) {
                Arrays.sort(backups, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                
                for (int i = 0; i < backups.length - MAX_BACKUP_FILES; i++) {
                    backups[i].delete();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup old backups: " + e.getMessage());
        }
    }
    
    /**
     * Create log directory
     */
    private static void createLogDirectory() {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                boolean created = logDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create log directory: " + LOG_DIR);
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating log directory: " + e.getMessage());
        }
    }
    
    // =========================== UTILITY CLASSES ===========================
    
    /**
     * Performance tracker
     */
    private static class PerformanceTracker {
        private final String operation;
        private final long startTime;
        
        public PerformanceTracker(String operation) {
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
        }
        
        public long end() {
            return System.currentTimeMillis() - startTime;
        }
    }
    
    // =========================== STATUS METHODS ===========================
    
    /**
     * Get logging statistics
     */
    public static String getLoggingStats() {
        return String.format("Logs written: %d, Active trackers: %d, Current level: %s", 
                logCounter.get(), 
                performanceTrackers.size(), 
                currentLogLevel.getName());
    }
    
    /**
     * Clear all performance trackers
     */
    public static void clearPerformanceTrackers() {
        performanceTrackers.clear();
        performance("AdvancedLogger", "clearPerformanceTrackers", "All performance trackers cleared");
    }
}
