package com.biotak.monitor;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Interactive Monitoring Dashboard
 * 
 * Features:
 * - Command-line interface for monitoring control
 * - Real-time dashboard display
 * - Interactive commands
 * - Easy configuration
 */
public class MonitoringDashboard {
    
    private final PerformanceMonitor monitor;
    private final Scanner scanner;
    private volatile boolean running = true;
    
    public MonitoringDashboard() {
        this.monitor = new PerformanceMonitor();
        this.scanner = new Scanner(System.in);
        setupCustomAlerts();
    }
    
    private void setupCustomAlerts() {
        monitor.addAlertListener((type, message, snapshot) -> {
            String emoji = getAlertEmoji(type);
            System.out.println("\n" + emoji + " ALERT: " + message);
            System.out.println("📊 Current: Memory=" + String.format("%.1f%%", snapshot.memoryUsage) + 
                             ", Threads=" + snapshot.threadCount);
            System.out.print("\nMonitor> ");
        });
    }
    
    private String getAlertEmoji(PerformanceMonitor.AlertType type) {
        return switch (type) {
            case HIGH_MEMORY -> "🔴";
            case HIGH_CPU -> "🟠";
            case EXCESSIVE_GC -> "🟡";
            case HIGH_THREAD_COUNT -> "🔵";
            case MEMORY_LEAK -> "🆘";
            case LOW_EFFICIENCY -> "⚠️";
        };
    }
    
    public void start() {
        printWelcome();
        
        while (running) {
            System.out.print("Monitor> ");
            String input = scanner.nextLine().trim().toLowerCase();
            
            if (input.isEmpty()) continue;
            
            processCommand(input);
        }
        
        cleanup();
    }
    
    private void printWelcome() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║        🔍 PERFORMANCE MONITOR          ║");
        System.out.println("║              Dashboard v1.0            ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println("  start [interval]  - Start monitoring (default: 5s)");
        System.out.println("  stop             - Stop monitoring");
        System.out.println("  status           - Show current status");
        System.out.println("  report           - Show performance report");
        System.out.println("  history          - Show recent snapshots");
        System.out.println("  clear            - Clear history");
        System.out.println("  threshold <metric> <value> - Set threshold");
        System.out.println("  export [file]    - Export data to CSV");
        System.out.println("  dashboard        - Show live dashboard");
        System.out.println("  help             - Show this help");
        System.out.println("  exit             - Exit application");
        System.out.println();
    }
    
    private void processCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0];
        
        try {
            switch (command) {
                case "start" -> {
                    int interval = parts.length > 1 ? Integer.parseInt(parts[1]) : 5;
                    if (interval < 1 || interval > 300) {
                        System.out.println("❌ Interval must be between 1-300 seconds");
                        return;
                    }
                    monitor.startMonitoring(interval);
                }
                
                case "stop" -> monitor.stopMonitoring();
                
                case "status" -> showStatus();
                
                case "report" -> monitor.printPerformanceReport();
                
                case "history" -> showHistory();
                
                case "clear" -> monitor.clearHistory();
                
                case "threshold" -> {
                    if (parts.length != 3) {
                        System.out.println("❌ Usage: threshold <metric> <value>");
                        System.out.println("   Metrics: memory_usage, cpu_usage, gc_time, thread_count");
                        return;
                    }
                    double value = Double.parseDouble(parts[2]);
                    monitor.setThreshold(parts[1], value);
                }
                
                case "export" -> {
                    String filename = parts.length > 1 ? parts[1] : "performance_data.csv";
                    monitor.exportData(filename);
                }
                
                case "dashboard" -> showDashboard();
                
                case "help" -> printWelcome();
                
                case "exit", "quit" -> {
                    running = false;
                    System.out.println("👋 Goodbye!");
                }
                
                default -> System.out.println("❌ Unknown command: " + command + ". Type 'help' for available commands.");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format");
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
    
    private void showStatus() {
        System.out.println("\n📊 === MONITOR STATUS ===");
        System.out.println("Status: " + (monitor.isMonitoring() ? "🟢 Running" : "🔴 Stopped"));
        System.out.println("History size: " + monitor.getHistory().size() + " snapshots");
        
        if (!monitor.getHistory().isEmpty()) {
            var latest = monitor.getHistory().get(monitor.getHistory().size() - 1);
            System.out.println("Last update: " + latest.timestamp.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            System.out.println("Current Memory: " + String.format("%.1f%%", latest.memoryUsage));
            System.out.println("Current Threads: " + latest.threadCount);
        }
        System.out.println("========================\n");
    }
    
    private void showHistory() {
        var history = monitor.getHistory();
        if (history.isEmpty()) {
            System.out.println("📋 No performance data available");
            return;
        }
        
        System.out.println("\n📈 === RECENT PERFORMANCE SNAPSHOTS ===");
        int start = Math.max(0, history.size() - 10); // Show last 10
        
        for (int i = start; i < history.size(); i++) {
            var snapshot = history.get(i);
            System.out.printf("%2d. %s%n", i + 1, snapshot);
        }
        
        if (history.size() > 10) {
            System.out.println("... (showing last 10 of " + history.size() + " snapshots)");
        }
        System.out.println("====================================\n");
    }
    
    private void showDashboard() {
        if (!monitor.isMonitoring()) {
            System.out.println("⚠️  Start monitoring first with 'start' command");
            return;
        }
        
        System.out.println("📊 Live Dashboard - Press Enter to return to command mode\n");
        
        Thread dashboardThread = new Thread(() -> {
            while (monitor.isMonitoring() && !Thread.currentThread().isInterrupted()) {
                clearScreen();
                printDashboard();
                
                try {
                    Thread.sleep(2000); // Update every 2 seconds
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        
        dashboardThread.start();
        scanner.nextLine(); // Wait for Enter
        dashboardThread.interrupt();
        
        System.out.println("📊 Dashboard closed\n");
    }
    
    private void clearScreen() {
        // Clear screen for Windows/Unix
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[2J\033[H");
            }
        } catch (Exception e) {
            // Fallback: print empty lines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    
    private void printDashboard() {
        var history = monitor.getHistory();
        if (history.isEmpty()) {
            System.out.println("No data available yet...");
            return;
        }
        
        var latest = history.get(history.size() - 1);
        
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                    🔍 LIVE DASHBOARD                        │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.printf("│ Time: %-49s │%n", latest.timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        
        // Memory usage bar
        System.out.printf("│ Memory Usage: %5.1f%% │", latest.memoryUsage);
        printProgressBar((int) latest.memoryUsage, 100);
        System.out.println(" │");
        
        // CPU usage bar (if available)
        if (latest.cpuUsage >= 0) {
            System.out.printf("│ CPU Usage:    %5.1f%% │", latest.cpuUsage);
            printProgressBar((int) latest.cpuUsage, 100);
            System.out.println(" │");
        }
        
        System.out.printf("│ Threads:      %5d     │                              │%n", latest.threadCount);
        System.out.printf("│ Heap Used:    %5d MB  │                              │%n", latest.heapUsed / 1024 / 1024);
        System.out.printf("│ GC Count:     %5d     │                              │%n", latest.gcCount);
        
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        
        // Show trend (last 5 snapshots)
        if (history.size() >= 5) {
            System.out.println("│ Memory Trend (last 5):                                     │");
            System.out.print("│ ");
            for (int i = Math.max(0, history.size() - 5); i < history.size(); i++) {
                System.out.printf("%5.1f%% ", history.get(i).memoryUsage);
            }
            System.out.println("                        │");
        }
        
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println("\nPress Enter to return to command mode...");
    }
    
    private void printProgressBar(int current, int max) {
        int barLength = 30;
        int filled = (int) ((double) current / max * barLength);
        
        System.out.print("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                if (current > 80) System.out.print("█");
                else if (current > 60) System.out.print("▓");
                else System.out.print("▒");
            } else {
                System.out.print("░");
            }
        }
        System.out.print("]");
    }
    
    private void cleanup() {
        if (monitor.isMonitoring()) {
            System.out.println("🛑 Stopping monitor...");
            monitor.stopMonitoring();
        }
        
        // Auto-export on exit
        if (!monitor.getHistory().isEmpty()) {
            System.out.println("💾 Auto-exporting data...");
            monitor.exportData("performance_data_" + 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        }
        
        scanner.close();
    }
    
    public static void main(String[] args) {
        MonitoringDashboard dashboard = new MonitoringDashboard();
        
        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down monitoring dashboard...");
        }));
        
        dashboard.start();
    }
}
