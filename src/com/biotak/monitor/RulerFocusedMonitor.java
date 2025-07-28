package com.biotak.monitor;

import com.biotak.debug.AdvancedLogger;
import com.biotak.config.LoggingConfiguration;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * مانیتور تخصصی خط‌کش - نظارت لحظه‌ای بر فعالیت‌های Ruler
 * Ruler-Focused Monitor - Real-time monitoring of Ruler activities
 */
public class RulerFocusedMonitor {
    
    private static final String CLASS_NAME = "RulerFocusedMonitor";
    private static final AtomicBoolean isRunning = new AtomicBoolean(true);
    private static final AtomicInteger rulerEvents = new AtomicInteger(0);
    private static final AtomicInteger totalLogs = new AtomicInteger(0);
    private static ExecutorService executorService;
    private static ScheduledExecutorService scheduler;
    
    // Console colors
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BOLD = "\u001B[1m";
    
    public static void main(String[] args) {
        try {
            startRulerMonitor();
        } catch (Exception e) {
            System.err.println("Error starting ruler monitor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * شروع مانیتور خط‌کش
     */
    private static void startRulerMonitor() throws Exception {
        // Initialize logging with enhanced ruler tracking
        LoggingConfiguration.initialize();
        AdvancedLogger.setLogLevel(AdvancedLogger.LogLevel.DEBUG);
        
        printRulerHeader();
        
        // Create thread pool
        executorService = Executors.newFixedThreadPool(3);
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Start log file watcher for ruler events
        executorService.submit(RulerFocusedMonitor::watchRulerLogs);
        
        // Start ruler status updater
        scheduler.scheduleAtFixedRate(RulerFocusedMonitor::updateRulerStatus, 0, 2, TimeUnit.SECONDS);
        
        // Log monitoring start
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, "startRulerMonitor", 
            "🎯 Ruler-focused monitoring started - Ready for testing!");
        
        System.out.println(BOLD + GREEN + "🎯 RULER MONITOR ACTIVE - Start testing your ruler now!" + RESET);
        System.out.println(YELLOW + "⏳ Monitoring ruler events... (Press Ctrl+C to stop)" + RESET);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        
        // Keep running until interrupted
        try {
            while (isRunning.get()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }
    
    /**
     * چاپ هدر مانیتور خط‌کش
     */
    private static void printRulerHeader() {
        clearScreen();
        System.out.println(BOLD + CYAN + "╔══════════════════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + CYAN + "║                      📏 BIOTAK RULER MONITOR 📏                             ║" + RESET);
        System.out.println(BOLD + CYAN + "║                        مانیتور تخصصی خط‌کش بایوتک                            ║" + RESET);
        System.out.println(BOLD + CYAN + "╚══════════════════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println(BOLD + WHITE + "⏰ Started at: " + LocalDateTime.now().format(formatter) + RESET);
        System.out.println(BOLD + WHITE + "🎯 Focus: Ruler Events and Activities" + RESET);
        System.out.println(BOLD + WHITE + "📋 Log Level: DEBUG (Full Details)" + RESET);
        System.out.println();
        
        System.out.println(BOLD + YELLOW + "🔍 MONITORING TARGETS:" + RESET);
        System.out.println(GREEN + "  • Ruler Initialization & Setup" + RESET);
        System.out.println(GREEN + "  • Mouse Click Events (Start/End Points)" + RESET);
        System.out.println(GREEN + "  • Mouse Move Events (Dynamic Tracking)" + RESET);
        System.out.println(GREEN + "  • Ruler State Changes" + RESET);
        System.out.println(GREEN + "  • Distance Calculations" + RESET);
        System.out.println(GREEN + "  • Error Handling & Debugging" + RESET);
        System.out.println();
    }
    
    /**
     * مشاهده لاگ‌های خط‌کش
     */
    private static void watchRulerLogs() {
        try {
            String logDir = LoggingConfiguration.getLogDirectory();
            Path logPath = Paths.get(logDir);
            
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
            }
            
            // Watch for file system events
            WatchService watchService = FileSystems.getDefault().newWatchService();
            logPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            
            while (isRunning.get()) {
                WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path eventPath = (Path) event.context();
                        
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY && eventPath.toString().endsWith(".log")) {
                            readAndFilterRulerLogs(logPath.resolve(eventPath));
                        }
                    }
                    key.reset();
                }
            }
            
        } catch (Exception e) {
            System.err.println(RED + "❌ Ruler log watcher error: " + e.getMessage() + RESET);
        }
    }
    
    /**
     * خواندن و فیلتر کردن لاگ‌های خط‌کش
     */
    private static void readAndFilterRulerLogs(Path logFile) {
        try (BufferedReader reader = Files.newBufferedReader(logFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                totalLogs.incrementAndGet();
                
                // Filter and display ruler-related logs
                if (isRulerRelated(line)) {
                    rulerEvents.incrementAndGet();
                    displayRulerLogEntry(line);
                }
                // Also show critical errors that might affect ruler
                else if (line.contains("[ERROR]") || line.contains("[FATAL]")) {
                    displayRulerLogEntry("⚠️  CRITICAL: " + line);
                }
            }
        } catch (Exception e) {
            // Ignore file access errors during monitoring
        }
    }
    
    /**
     * بررسی ارتباط لاگ با خط‌کش
     */
    private static boolean isRulerRelated(String logEntry) {
        String lowerEntry = logEntry.toLowerCase();
        return lowerEntry.contains("ruler") ||
               lowerEntry.contains("[rul]") ||
               lowerEntry.contains("onclick") ||
               lowerEntry.contains("onmousemove") ||
               lowerEntry.contains("onmousedown") ||
               lowerEntry.contains("resize") ||
               lowerEntry.contains("distance") ||
               lowerEntry.contains("pips") ||
               lowerEntry.contains("bars") ||
               lowerEntry.contains("coordinate") ||
               lowerEntry.contains("translate") ||
               lowerEntry.contains("rulerstate") ||
               lowerEntry.contains("waiting_for_start") ||
               lowerEntry.contains("waiting_for_end") ||
               lowerEntry.contains("active") ||
               lowerEntry.contains("inactive");
    }
    
    /**
     * نمایش ورودی لاگ خط‌کش با رنگ‌بندی ویژه
     */
    private static void displayRulerLogEntry(String logEntry) {
        String timestamp = getCurrentTimestamp();
        String coloredEntry = logEntry;
        
        // Special color coding for ruler events
        if (logEntry.contains("[RUL]")) {
            coloredEntry = BOLD + CYAN + "📏 RULER: " + RESET + CYAN + logEntry + RESET;
        } else if (logEntry.contains("onClick")) {
            coloredEntry = BOLD + GREEN + "🖱️  CLICK: " + RESET + GREEN + logEntry + RESET;
        } else if (logEntry.contains("onMouseMove")) {
            coloredEntry = BOLD + BLUE + "🔄 MOVE: " + RESET + BLUE + logEntry + RESET;
        } else if (logEntry.contains("STATE")) {
            coloredEntry = BOLD + PURPLE + "🔀 STATE: " + RESET + PURPLE + logEntry + RESET;
        } else if (logEntry.contains("WAITING_FOR_START")) {
            coloredEntry = BOLD + YELLOW + "⏳ WAIT_START: " + RESET + YELLOW + logEntry + RESET;
        } else if (logEntry.contains("WAITING_FOR_END")) {
            coloredEntry = BOLD + YELLOW + "⏳ WAIT_END: " + RESET + YELLOW + logEntry + RESET;
        } else if (logEntry.contains("ACTIVE")) {
            coloredEntry = BOLD + GREEN + "✅ ACTIVE: " + RESET + GREEN + logEntry + RESET;
        } else if (logEntry.contains("ERROR") || logEntry.contains("FATAL")) {
            coloredEntry = BOLD + RED + "🔴 ERROR: " + RESET + RED + logEntry + RESET;
        } else if (logEntry.contains("distance") || logEntry.contains("pips")) {
            coloredEntry = BOLD + WHITE + "📐 MEASURE: " + RESET + WHITE + logEntry + RESET;
        }
        
        System.out.println(WHITE + "[" + timestamp + "] " + RESET + coloredEntry);
    }
    
    /**
     * به‌روزرسانی وضعیت خط‌کش
     */
    private static void updateRulerStatus() {
        if (isRunning.get()) {
            // Log current monitoring status
            if (rulerEvents.get() > 0) {
                AdvancedLogger.debug(CLASS_NAME, "updateRulerStatus", 
                    "📊 Ruler events captured: %d | Total logs: %d", 
                    rulerEvents.get(), totalLogs.get());
            }
        }
    }
    
    /**
     * پاک کردن صفحه
     */
    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[2J\033[H");
            }
        } catch (Exception e) {
            for (int i = 0; i < 30; i++) {
                System.out.println();
            }
        }
    }
    
    /**
     * دریافت زمان فعلی
     */
    private static String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        return LocalDateTime.now().format(formatter);
    }
    
    /**
     * خاموش کردن مانیتور
     */
    private static void shutdown() {
        System.out.println();
        System.out.println(YELLOW + "🔄 Shutting down Ruler Monitor..." + RESET);
        
        // Log final statistics
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, "shutdown", 
            "📊 Final Statistics - Ruler Events: %d | Total Logs: %d", 
            rulerEvents.get(), totalLogs.get());
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        try {
            if (executorService != null && !executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            
            if (scheduler != null && !scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println();
        System.out.println(BOLD + CYAN + "╔══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + CYAN + "║                   📏 RULER MONITOR REPORT 📏                 ║" + RESET);
        System.out.println(BOLD + CYAN + "╚══════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println(WHITE + "📊 Total Ruler Events Captured: " + BOLD + GREEN + rulerEvents.get() + RESET);
        System.out.println(WHITE + "📈 Total Log Entries Processed: " + BOLD + BLUE + totalLogs.get() + RESET);
        System.out.println(WHITE + "📁 Log Files Location: " + BOLD + YELLOW + LoggingConfiguration.getLogDirectory() + RESET);
        System.out.println();
        System.out.println(GREEN + "✅ Ruler Monitor shut down successfully!" + RESET);
        System.out.println(BOLD + CYAN + "🎯 Ready to analyze ruler test results! 🚀" + RESET);
    }
    
    // Add shutdown hook to ensure clean shutdown
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning.set(false);
            shutdown();
        }));
    }
}
