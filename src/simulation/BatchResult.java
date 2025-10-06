package simulation;

import java.time.LocalDate;

/**
 * Wraps a simulation result with its execution date and strategy context
 * Enables aggregation and comparison across multiple dates/strategies
 */
public class BatchResult {
    private final LocalDate date;
    private final String strategyName;
    private final SimulationResult simulationResult;

    public BatchResult(LocalDate date, String strategyName, SimulationResult simulationResult) {
        this.date = date;
        this.strategyName = strategyName;
        this.simulationResult = simulationResult;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public SimulationResult getSimulationResult() {
        return simulationResult;
    }

    @Override
    public String toString() {
        return String.format("BatchResult{date=%s, strategy=%s}", date, strategyName);
    }
}