package util;

import data.Fill;
import data.Order;

public class SimulationLogger {
    private static boolean verbose = false;
    private static LogLevel currentLevel = LogLevel.INFO;

    public enum LogLevel {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3);

        private final int priority;

        LogLevel(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    /**
     * Configure verbose mode (shows all logs when true)
     */
    public static void setVerbose(boolean enabled) {
        verbose = enabled;
    }

    /**
     * Set minimum log level (future use)
     */
    public static void setLogLevel(LogLevel level) {
        currentLevel = level;
    }

    /**
     * Check if verbose mode is enabled
     */
    public static boolean isVerbose() {
        return verbose;
    }

    /**
     * Log a message (only if verbose is enabled)
     */
    public static void log(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }

    /**
     * Log an order with formatting
     */
    public static void logOrder(Order order) {
        if (verbose) {
            System.out.println("  [ORDER] " + order);
        }
    }

    /**
     * Log a fill with formatting
     */
    public static void logFill(Fill fill) {
        if (verbose) {
            System.out.println("  [FILL] " + fill);
        }
    }

    /**
     * Log section header
     */
    public static void logSection(String title) {
        if (verbose) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("  " + title);
            System.out.println("=".repeat(50));
        }
    }

    /**
     * Future: Debug level logging
     */
    public static void debug(String message) {
        if (verbose && currentLevel.getPriority() <= LogLevel.DEBUG.getPriority()) {
            System.out.println("[DEBUG] " + message);
        }
    }

    /**
     * Future: Info level logging
     */
    public static void info(String message) {
        if (currentLevel.getPriority() <= LogLevel.INFO.getPriority()) {
            System.out.println("[INFO] " + message);
        }
    }

    /**
     * Future: Warning level logging
     */
    public static void warn(String message) {
        if (currentLevel.getPriority() <= LogLevel.WARN.getPriority()) {
            System.out.println("[WARN] " + message);
        }
    }

    /**
     * Future: Error level logging (always shown)
     */
    public static void error(String message) {
        System.err.println("[ERROR] " + message);
    }
}
