package simulation;

import data.Fill;
import metrics.ExecutionMetrics;
import strategy.ExecutionStrategy;

import java.time.LocalDate;
import java.util.List;

public class SimulationResult {
    private final String strategyName;
    private final ExecutionStrategy strategy;
    private final List<Fill> fills;
    private final ExecutionMetrics executionMetrics;
    private final long executionTimeMs;

    // Metadata for filtering and grouping
    private final LocalDate date;
    private final String timeWindow;

    public SimulationResult(ExecutionStrategy strategy, List<Fill> fills,
                           ExecutionMetrics executionMetrics, long executionTimeMs) {
        this(strategy, fills, executionMetrics, executionTimeMs, null, null);
    }

    public SimulationResult(ExecutionStrategy strategy, List<Fill> fills,
                           ExecutionMetrics executionMetrics, long executionTimeMs,
                           LocalDate date, String timeWindow) {
        this.strategy = strategy;
        this.strategyName = strategy.getName();
        this.fills = fills;
        this.executionMetrics = executionMetrics;
        this.executionTimeMs = executionTimeMs;
        this.date = date;
        this.timeWindow = timeWindow;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public ExecutionStrategy getStrategy() {
        return strategy;
    }

    public List<Fill> getFills() {
        return fills;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public ExecutionMetrics getExecutionMetrics() {
        return executionMetrics;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTimeWindow() {
        return timeWindow;
    }

    /**
     * Convenience method to get metrics directly
     */
    public ExecutionMetrics metrics() {
        return executionMetrics;
    }

    public void printSummary() {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println(String.format("║     SIMULATION RESULT - %-25s ║", strategyName));
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.printf("Fills: %d | Simulation Time: %d ms\n", fills.size(), executionTimeMs);
        System.out.println("────────────────────────────────────────────────────");

        // Print execution metrics if available
        if (executionMetrics != null) {
            executionMetrics.printSummary();
        } else {
            System.out.println("\n[No execution metrics available]\n");
        }
    }
}
