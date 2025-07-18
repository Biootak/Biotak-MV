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
    private static String LOG_FILE_PATH = "C:/Users/fatemeh/IdeaProject/Biotak/biotak_log.txt";
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
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
     * Allows the application to override the default log file location (e.g., user home).
     */
    public static void setLogFilePath(String path) {
        if (path != null && !path.isEmpty()) {
            LOG_FILE_PATH = path;
        }
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

        try {
            rotateIfNeeded();
            try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(logEntry);
            }
        } catch (IOException ioe) {
            System.err.println("Logger I/O Error: " + ioe.getMessage());
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

    // ----------------------  Helpers  ----------------------
    private static void rotateIfNeeded() throws IOException {
        java.io.File f = new java.io.File(LOG_FILE_PATH);
        if (!f.exists()) return;
        if (f.length() < MAX_SIZE_BYTES) return;

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        java.io.File rotated = new java.io.File(LOG_FILE_PATH + "." + ts + ".bak");
        boolean renamed = f.renameTo(rotated);
        if (!renamed) {
            // If rename fails, attempt to copy and truncate
            try (java.io.FileInputStream in = new java.io.FileInputStream(f);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(rotated)) {
                in.transferTo(out);
            }
            // Clear original file
            try (PrintWriter pw = new PrintWriter(f)) { pw.print(""); }
        }
    }
} 