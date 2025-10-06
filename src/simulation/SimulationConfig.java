package simulation;

import data.Order;
import ingestion.MarketDataReader;
import strategy.ExecutionStrategy;

import java.util.ArrayList;
import java.util.List;

public class SimulationConfig {
    private final List<ExecutionStrategy> strategies;
    private final MarketDataReader dataReader;
    private final Order parentOrder;
    private final TimeWindowSelector timeWindowSelector;
    private final List<String> metrics;
    private final boolean verbose;

    private SimulationConfig(Builder builder) {
        this.strategies = builder.strategies;
        this.dataReader = builder.dataReader;
        this.parentOrder = builder.parentOrder;
        this.timeWindowSelector = builder.timeWindowSelector;
        this.metrics = builder.metrics;
        this.verbose = builder.verbose;
    }

    public List<ExecutionStrategy> getStrategies() {
        return strategies;
    }

    public MarketDataReader getDataReader() {
        return dataReader;
    }

    public Order getParentOrder() {
        return parentOrder;
    }

    public TimeWindowSelector getTimeWindowSelector() {
        return timeWindowSelector;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public static class Builder {
        private List<ExecutionStrategy> strategies = new ArrayList<>();
        private MarketDataReader dataReader;
        private Order parentOrder;
        private TimeWindowSelector timeWindowSelector = new FullDaySelector();
        private List<String> metrics = new ArrayList<>();
        private boolean verbose = false;

        public Builder withStrategy(ExecutionStrategy strategy) {
            this.strategies.add(strategy);
            return this;
        }

        public Builder withStrategies(List<ExecutionStrategy> strategies) {
            this.strategies.addAll(strategies);
            return this;
        }

        public Builder withDataReader(MarketDataReader dataReader) {
            this.dataReader = dataReader;
            return this;
        }

        public Builder withParentOrder(Order parentOrder) {
            this.parentOrder = parentOrder;
            return this;
        }

        public Builder withTimeWindow(TimeWindowSelector selector) {
            this.timeWindowSelector = selector;
            return this;
        }

        public Builder calculateMetric(String metric) {
            this.metrics.add(metric);
            return this;
        }

        public Builder calculateMetrics(String... metrics) {
            for (String metric : metrics) {
                this.metrics.add(metric);
            }
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public SimulationConfig build() {
            if (strategies.isEmpty()) {
                throw new IllegalStateException("At least one strategy must be specified");
            }
            if (dataReader == null) {
                throw new IllegalStateException("Data reader must be specified");
            }
            if (parentOrder == null) {
                throw new IllegalStateException("Parent order must be specified");
            }
            if (metrics.isEmpty()) {
                // Default metrics
                metrics.add("VWAP");
                metrics.add("AVG_PRICE");
                metrics.add("TOTAL_QTY");
            }
            return new SimulationConfig(this);
        }
    }
}
