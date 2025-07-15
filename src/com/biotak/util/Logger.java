package com.biotak.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple logger utility to write messages to both the console and a log file.
 */
public final class Logger {
    private static final String LOG_FILE_PATH = "C:/Users/fatemeh/IdeaProject/Biotak/biotak_log.txt";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Log levels
    public enum LogLevel {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3);
        
        private final int level;
        
        LogLevel(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    // Current log level - set to WARN by default to reduce excessive logging
    private static LogLevel currentLogLevel = LogLevel.WARN;

    private Logger() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Sets the current log level.
     * @param level The log level to set.
     */
    public static void setLogLevel(LogLevel level) {
        currentLogLevel = level;
    }

    /**
     * Logs a message to the console and appends it to the project's log file.
     * Uses INFO level by default.
     * @param message The message to log.
     */
    public static void log(String message) {
        log(LogLevel.INFO, message);
    }
    
    /**
     * Logs a message with the specified log level.
     * @param level The log level.
     * @param message The message to log.
     */
    public static void log(LogLevel level, String message) {
        // Skip if the message's level is below the current log level
        if (level.getLevel() < currentLogLevel.getLevel()) {
            return;
        }
        
        String timestamp = DATE_FORMAT.format(new Date());
        String logEntry = timestamp + " - " + level.name() + ": " + message;

        // Print to standard output
        System.out.println(logEntry);

        // Append to the log file
        try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
    
    /**
     * Debug level logging.
     * @param message The message to log.
     */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    /**
     * Info level logging.
     * @param message The message to log.
     */
    public static void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    /**
     * Warning level logging.
     * @param message The message to log.
     */
    public static void warn(String message) {
        log(LogLevel.WARN, message);
    }
    
    /**
     * Error level logging.
     * @param message The message to log.
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }
} 