package com.biotak.monitor;

import com.biotak.debug.AdvancedLogger;
import com.biotak.config.LoggingConfiguration;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * سیستم مانیتور لاگ لحظه‌ای - نمایش زنده لاگ‌های BiotakTrigger
 * Real-time Log Monitor - Live display of BiotakTrigger logs
 */
public class RealTimeLogMonitor {
    
    private static final String CLASS_NAME = "RealTimeLogMonitor";
    private static final AtomicBoolean isRunning = new AtomicBoolean(true);
    private static final AtomicInteger logCounter = new AtomicInteger(0);
    private static ExecutorService executorService;
    private static ScheduledExecutorService scheduler;
    
    // Console colors for better visibility
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
            startRealTimeMonitor();
        } catch (Exception e) {
            System.err.println("Error starting real-time monitor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * شروع مانیتور لحظه‌ای
     */
    private static void startRealTimeMonitor() throws Exception {
        // Initialize logging system
        LoggingConfiguration.initialize();
        
        printHeader();
        printControls();
        
        // Create thread pool
        executorService = Executors.newFixedThreadPool(4);
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Start log file watcher
        Future<?> logWatcher = executorService.submit(RealTimeLogMonitor::watchLogFiles);
        
        // Start statistics updater
        scheduler.scheduleAtFixedRate(RealTimeLogMonitor::updateStatistics, 0, 5, TimeUnit.SECONDS);
        
        // Start activity simulator
        Future<?> simulator = executorService.submit(RealTimeLogMonitor::startActivitySimulator);
        
        // Start command handler
        Future<?> commandHandler = executorService.submit(RealTimeLogMonitor::handleUserCommands);
        
        // Wait for completion
        try {
            commandHandler.get();
        } catch (Exception e) {
            System.err.println("Command handler error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    /**
     * چاپ هدر مانیتور
     */
    private static void printHeader() {
        clearScreen();
        System.out.println(BOLD + CYAN + "╔══════════════════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + CYAN + "║                         🔥 BIOTAK REAL-TIME LOG MONITOR 🔥                  ║" + RESET);
        System.out.println(BOLD + CYAN + "║                              مانیتور لاگ لحظه‌ای بایوتک                        ║" + RESET);
        System.out.println(BOLD + CYAN + "╚══════════════════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
        
        // Show current time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println(BOLD + WHITE + "⏰ Started at: " + LocalDateTime.now().format(formatter) + RESET);
        System.out.println();
    }
    
    /**
     * چاپ کنترل‌های موجود
     */
    private static void printControls() {
        System.out.println(BOLD + YELLOW + "📋 AVAILABLE COMMANDS:" + RESET);
        System.out.println(GREEN + "  • 'q' or 'quit'     → Exit monitor" + RESET);
        System.out.println(GREEN + "  • 's' or 'stats'    → Show statistics" + RESET);
        System.out.println(GREEN + "  • 'c' or 'clear'    → Clear screen" + RESET);
        System.out.println(GREEN + "  • 'l' or 'level'    → Change log level" + RESET);
        System.out.println(GREEN + "  • 'p' or 'pause'    → Pause/Resume simulator" + RESET);
        System.out.println(GREEN + "  • 'h' or 'help'     → Show this help" + RESET);
        System.out.println();
        System.out.println(BOLD + PURPLE + "🔄 REAL-TIME LOGS (press Enter to start):" + RESET);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
    }
    
    /**
     * مشاهده فایل‌های لاگ
     */
    private static void watchLogFiles() {
        try {
            String logDir = LoggingConfiguration.getLogDirectory();
            Path logPath = Paths.get(logDir);
            
            // Create directory if it doesn't exist
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
            }
            
            AdvancedLogger.info(CLASS_NAME, "watchLogFiles", "Watching log directory: %s", logDir);
            
            // Watch for file system events
            WatchService watchService = FileSystems.getDefault().newWatchService();
            logPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            
            while (isRunning.get()) {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path eventPath = (Path) event.context();
                        
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY && eventPath.toString().endsWith(".log")) {
                            readLatestLogEntries(logPath.resolve(eventPath));
                        }
                    }
                    key.reset();
                }
            }
            
        } catch (Exception e) {
            System.err.println(RED + "❌ Log watcher error: " + e.getMessage() + RESET);
        }
    }
    
    /**
     * خواندن آخرین ورودی‌های لاگ
     */
    private static void readLatestLogEntries(Path logFile) {
        try (BufferedReader reader = Files.newBufferedReader(logFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                displayLogEntry(line);
                logCounter.incrementAndGet();
            }
        } catch (Exception e) {
            // Ignore file access errors during real-time monitoring
        }
    }
    
    /**
     * نمایش ورودی لاگ با رنگ‌بندی
     */
    private static void displayLogEntry(String logEntry) {
        String timestamp = getCurrentTimestamp();
        
        // Color coding based on log level
        String coloredEntry = logEntry;
        if (logEntry.contains("[ERROR]") || logEntry.contains("[FATAL]")) {
            coloredEntry = RED + "🔴 " + logEntry + RESET;
        } else if (logEntry.contains("[WARN]")) {
            coloredEntry = YELLOW + "⚠️  " + logEntry + RESET;
        } else if (logEntry.contains("[INFO]")) {
            coloredEntry = GREEN + "ℹ️  " + logEntry + RESET;
        } else if (logEntry.contains("[DEBUG]")) {
            coloredEntry = BLUE + "🔍 " + logEntry + RESET;
        } else if (logEntry.contains("[PERF]")) {
            coloredEntry = PURPLE + "⏱️  " + logEntry + RESET;
        } else if (logEntry.contains("[RUL]")) {
            coloredEntry = CYAN + "📏 " + logEntry + RESET;
        }
        
        System.out.println(WHITE + "[" + timestamp + "] " + RESET + coloredEntry);
    }
    
    /**
     * شروع شبیه‌ساز فعالیت
     */
    private static void startActivitySimulator() {
        AtomicBoolean simulatorPaused = new AtomicBoolean(false);
        
        AdvancedLogger.info(CLASS_NAME, "startActivitySimulator", "🚀 Activity simulator started");
        
        int cycle = 0;
        while (isRunning.get()) {
            try {
                if (!simulatorPaused.get()) {
                    simulateRealTimeActivity(cycle++);
                }
                
                // Random delay between 2-5 seconds
                Thread.sleep(2000 + (int)(Math.random() * 3000));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * شبیه‌سازی فعالیت لحظه‌ای
     */
    private static void simulateRealTimeActivity(int cycle) {
        try {
            // Simulate different BiotakTrigger activities
            switch (cycle % 6) {
                case 0 -> simulateCalculation(cycle);
                case 1 -> simulateRulerActivity(cycle);
                case 2 -> simulatePerformanceTracking(cycle);
                case 3 -> simulateHistoricalUpdate(cycle);
                case 4 -> simulateUserInteraction(cycle);
                case 5 -> simulateErrorHandling(cycle);
            }
            
        } catch (Exception e) {
            AdvancedLogger.error(CLASS_NAME, "simulateRealTimeActivity", "Simulation error: %s", e.getMessage());
        }
    }
    
    private static void simulateCalculation(int cycle) {
        AdvancedLogger.debug("BiotakTrigger", "calculate", "Real-time calculation cycle %d started", cycle);
        
        double mockPrice = 1.2400 + (Math.random() * 0.0100);
        double mockTH = 0.0015 + (Math.random() * 0.0005);
        
        AdvancedLogger.info("BiotakTrigger", "calculateTH", "TH calculated: %.5f at price %.5f", mockTH, mockPrice);
    }
    
    private static void simulateRulerActivity(int cycle) {
        if (cycle % 3 == 0) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, "updateRuler", "Ruler updated by user interaction");
            
            double pips = 15.5 + (Math.random() * 50);
            int bars = 5 + (int)(Math.random() * 20);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, "calculateDistance", 
                "Distance measured: %.1f pips over %d bars", pips, bars);
        }
    }
    
    private static void simulatePerformanceTracking(int cycle) {
        String operation = "LiveOperation" + cycle;
        AdvancedLogger.startPerformanceTracking(operation);
        
        try {
            // Simulate variable processing time
            Thread.sleep(50 + (int)(Math.random() * 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        AdvancedLogger.endPerformanceTracking(operation);
        
        // Occasionally simulate slow operations
        if (Math.random() < 0.1) {
            AdvancedLogger.startPerformanceTracking("SlowLiveOperation");
            try {
                Thread.sleep(1100 + (int)(Math.random() * 500));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            AdvancedLogger.endPerformanceTracking("SlowLiveOperation");
        }
    }
    
    private static void simulateHistoricalUpdate(int cycle) {
        double high = 1.2500 + (Math.random() * 0.0050);
        double low = 1.2350 - (Math.random() * 0.0050);
        
        AdvancedLogger.info("BiotakTrigger", "updateHistorical", "Historical range updated - High: %.5f, Low: %.5f", high, low);
    }
    
    private static void simulateUserInteraction(int cycle) {
        String[] interactions = {
            "Custom price moved to %.5f",
            "Panel minimized by user",
            "Log level changed to %s",
            "Ruler activated",
            "Settings updated"
        };
        
        String interaction = interactions[(int)(Math.random() * interactions.length)];
        
        if (interaction.contains("%.5f")) {
            double price = 1.2400 + (Math.random() * 0.0100);
            AdvancedLogger.info("BiotakTrigger", "onUserInteraction", interaction, price);
        } else if (interaction.contains("%s")) {
            String[] levels = {"DEBUG", "INFO", "WARN"};
            String level = levels[(int)(Math.random() * levels.length)];
            AdvancedLogger.info("BiotakTrigger", "onUserInteraction", interaction, level);
        } else {
            AdvancedLogger.info("BiotakTrigger", "onUserInteraction", interaction);
        }
    }
    
    private static void simulateErrorHandling(int cycle) {
        if (Math.random() < 0.15) { // 15% chance of warning/error
            if (Math.random() < 0.7) {
                AdvancedLogger.warn("BiotakTrigger", "validateData", "Data validation warning in cycle %d", cycle);
            } else {
                AdvancedLogger.error("BiotakTrigger", "processData", "Processing error detected in cycle %d", cycle);
            }
        }
    }
    
    /**
     * مدیریت دستورات کاربر
     */
    private static void handleUserCommands() {
        Scanner scanner = new Scanner(System.in);
        
        while (isRunning.get()) {
            try {
                String input = scanner.nextLine().trim().toLowerCase();
                
                switch (input) {
                    case "q", "quit", "exit" -> {
                        System.out.println(YELLOW + "🛑 Shutting down monitor..." + RESET);
                        isRunning.set(false);
                        return;
                    }
                    case "s", "stats", "statistics" -> showStatistics();
                    case "c", "clear" -> {
                        clearScreen();
                        printHeader();
                        System.out.println(GREEN + "🧹 Screen cleared!" + RESET);
                    }
                    case "l", "level" -> changeLogLevel(scanner);
                    case "p", "pause" -> {
                        System.out.println(YELLOW + "⏸️  Simulator paused (feature coming soon)" + RESET);
                    }
                    case "h", "help" -> printControls();
                    case "" -> { /* Empty input - continue monitoring */ }
                    default -> System.out.println(RED + "❓ Unknown command: " + input + " (type 'h' for help)" + RESET);
                }
                
            } catch (Exception e) {
                System.err.println(RED + "Command error: " + e.getMessage() + RESET);
            }
        }
    }
    
    /**
     * نمایش آمار
     */
    private static void showStatistics() {
        System.out.println();
        System.out.println(BOLD + CYAN + "═══════════════════════════════════════" + RESET);
        System.out.println(BOLD + CYAN + "           📊 LIVE STATISTICS            " + RESET);
        System.out.println(BOLD + CYAN + "═══════════════════════════════════════" + RESET);
        
        String stats = AdvancedLogger.getLoggingStats();
        System.out.println(WHITE + "🔢 " + stats + RESET);
        System.out.println(WHITE + "📈 Log entries displayed: " + logCounter.get() + RESET);
        System.out.println(WHITE + "⏰ Current time: " + getCurrentTimestamp() + RESET);
        System.out.println(WHITE + "📁 Log directory: " + LoggingConfiguration.getLogDirectory() + RESET);
        System.out.println(WHITE + "🎯 Current log level: " + LoggingConfiguration.getCurrentLogLevel() + RESET);
        
        System.out.println(BOLD + CYAN + "═══════════════════════════════════════" + RESET);
        System.out.println();
    }
    
    /**
     * تغییر سطح لاگ
     */
    private static void changeLogLevel(Scanner scanner) {
        System.out.println(YELLOW + "📝 Available log levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL" + RESET);
        System.out.print(WHITE + "Enter new log level: " + RESET);
        
        String level = scanner.nextLine().trim().toUpperCase();
        
        try {
            AdvancedLogger.LogLevel logLevel = AdvancedLogger.LogLevel.valueOf(level);
            AdvancedLogger.setLogLevel(logLevel);
            System.out.println(GREEN + "✅ Log level changed to: " + level + RESET);
        } catch (IllegalArgumentException e) {
            System.out.println(RED + "❌ Invalid log level: " + level + RESET);
        }
    }
    
    /**
     * به‌روزرسانی آمار
     */
    private static void updateStatistics() {
        if (isRunning.get()) {
            // Update statistics every 5 seconds
            String stats = AdvancedLogger.getLoggingStats();
            // Could display in status bar if needed
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
            // Fallback: print several new lines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    
    /**
     * دریافت زمان فعلی
     */
    private static String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }
    
    /**
     * خاموش کردن سیستم
     */
    private static void shutdown() {
        System.out.println(YELLOW + "🔄 Shutting down Real-Time Log Monitor..." + RESET);
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        try {
            if (executorService != null && !executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            
            if (scheduler != null && !scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println(GREEN + "✅ Real-Time Log Monitor shut down successfully!" + RESET);
        System.out.println(BOLD + CYAN + "Thank you for using Biotak Real-Time Log Monitor! 🚀" + RESET);
    }
}
