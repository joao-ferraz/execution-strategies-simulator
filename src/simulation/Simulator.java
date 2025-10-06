package simulation;

import data.Fill;
import data.Order;
import data.TickData;
import execution.MarketSimulator;
import ingestion.MarketDataReader;
import ingestion.MarketDataReaderFactory;
import metrics.ExecutionMetrics;
import metrics.MetricsCollector;
import strategy.ExecutionStrategy;
import util.SimulationLogger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Simulator {
    private final SimulationConfig config;
    private List<TickData> marketData;

    private Simulator(SimulationConfig config) {
        this.config = config;
    }

    /**
     * Run simulation for all configured strategies
     */
    public List<SimulationResult> runAll() {
        // Initialize logger with verbose configuration
        SimulationLogger.setVerbose(config.isVerbose());

        SimulationLogger.logSection("SIMULATION START");

        // Load market data once
        marketData = config.getDataReader().readTickData();

        if (marketData.isEmpty()) {
            throw new RuntimeException("No market data available");
        }

        SimulationLogger.log("Loaded " + marketData.size() + " ticks");
        SimulationLogger.log("Time range: " + marketData.get(0).getTimestamp() +
                           " to " + marketData.get(marketData.size() - 1).getTimestamp());

        List<SimulationResult> results = new ArrayList<>();

        for (ExecutionStrategy strategy : config.getStrategies()) {
            SimulationLogger.logSection("STRATEGY: " + strategy.getClass().getSimpleName());
            SimulationResult result = runSingle(strategy);
            results.add(result);
        }

        SimulationLogger.logSection("SIMULATION COMPLETE");

        return results;
    }

    /**
     * Run simulation for a single strategy
     */
    private SimulationResult runSingle(ExecutionStrategy strategy) {
        long startTime = System.currentTimeMillis();

        // Apply time window selection to parent order
        TimeWindow window = config.getTimeWindowSelector().select(marketData);

        SimulationLogger.log("Time window: " + config.getTimeWindowSelector().getDescription());
        SimulationLogger.log("  " + window);

        Order adjustedParentOrder = new Order(
            config.getParentOrder().getOrderId(),
            config.getParentOrder().getSymbol(),
            config.getParentOrder().getQuantity(),
            config.getParentOrder().getSide(),
            window.getStartTime(),
            window.getEndTime()
        );

        // Execute strategy using MarketSimulator
        MarketSimulator marketSimulator = MarketSimulator.builder().build();

        // Attach metrics collector
        MetricsCollector metricsCollector = new MetricsCollector();
        marketSimulator.addEventListener(metricsCollector);

        List<Fill> fills = marketSimulator.simulate(strategy, adjustedParentOrder, marketData);

        // Calculate execution metrics
        ExecutionMetrics executionMetrics = metricsCollector.calculateMetrics();

        long executionTime = System.currentTimeMillis() - startTime;

        return new SimulationResult(strategy, fills, executionMetrics, executionTime);
    }

    /**
     * Builder for easy configuration
     */
    public static class Builder {
        private final SimulationConfig.Builder configBuilder;

        public Builder() {
            this.configBuilder = new SimulationConfig.Builder();
        }

        public Builder withStrategy(ExecutionStrategy strategy) {
            configBuilder.withStrategy(strategy);
            return this;
        }

        public Builder withStrategies(List<ExecutionStrategy> strategies) {
            configBuilder.withStrategies(strategies);
            return this;
        }

        public Builder withStrategies(ExecutionStrategy... strategies) {
            configBuilder.withStrategies(List.of(strategies));
            return this;
        }

        public Builder withDataReader(MarketDataReader dataReader) {
            configBuilder.withDataReader(dataReader);
            return this;
        }

        public Builder withDataSource(String sourceType, String source) {
            MarketDataReader reader = MarketDataReaderFactory.createReader(sourceType, source);
            configBuilder.withDataReader(reader);
            return this;
        }

        public Builder withParentOrder(Order parentOrder) {
            configBuilder.withParentOrder(parentOrder);
            return this;
        }

        public Builder withParentOrder(String orderId, String symbol, int quantity, String side, Instant start, Instant end) {
            Order order = new Order(orderId, symbol, quantity, side, start, end);
            configBuilder.withParentOrder(order);
            return this;
        }

        /**
         * Convenience method: create parent order without time (will use TimeWindowSelector)
         */
        public Builder withOrder(String orderId, String symbol, int quantity, String side) {
            // Placeholder instants - will be replaced by TimeWindowSelector
            Order order = new Order(orderId, symbol, quantity, side, Instant.EPOCH, Instant.EPOCH);
            configBuilder.withParentOrder(order);
            return this;
        }

        public Builder withTimeWindow(TimeWindowSelector selector) {
            configBuilder.withTimeWindow(selector);
            return this;
        }

        public Builder calculateMetrics(String... metrics) {
            configBuilder.calculateMetrics(metrics);
            return this;
        }

        public Builder verbose(boolean verbose) {
            configBuilder.verbose(verbose);
            return this;
        }

        public Simulator build() {
            return new Simulator(configBuilder.build());
        }

        /**
         * Build and run immediately
         */
        public List<SimulationResult> run() {
            return build().runAll();
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
