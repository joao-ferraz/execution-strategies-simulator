package simulation;

import ingestion.MarketDataManager;

/**
 * Shared context for simulations
 * Holds configuration and resources used across multiple simulations
 */
public class SimulationContext {
    private final MarketDataManager dataManager;
    private final boolean verbose;
    private final boolean parallel;

    private static SimulationContext defaultContext;

    public SimulationContext(MarketDataManager dataManager, boolean verbose, boolean parallel) {
        this.dataManager = dataManager;
        this.verbose = verbose;
        this.parallel = parallel;
    }

    /**
     * Get default context with standard configuration
     */
    public static SimulationContext getDefault() {
        if (defaultContext == null) {
            defaultContext = new SimulationContext(
                new MarketDataManager("../market_data"),
                false,
                false  // Sequential by default
            );
        }
        return defaultContext;
    }

    /**
     * Set default context for all simulations
     */
    public static void setDefault(SimulationContext ctx) {
        defaultContext = ctx;
    }

    public MarketDataManager getDataManager() {
        return dataManager;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isParallel() {
        return parallel;
    }

    /**
     * Builder for custom context configuration
     */
    public static class Builder {
        private MarketDataManager dataManager = new MarketDataManager("../market_data");
        private boolean verbose = false;
        private boolean parallel = false;

        public Builder dataManager(MarketDataManager dataManager) {
            this.dataManager = dataManager;
            return this;
        }

        public Builder dataDirectory(String directory) {
            this.dataManager = new MarketDataManager(directory);
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder parallel(boolean parallel) {
            this.parallel = parallel;
            return this;
        }

        public SimulationContext build() {
            return new SimulationContext(dataManager, verbose, parallel);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
