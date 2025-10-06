package simulation;

import data.TickData;

import java.util.List;

public interface TimeWindowSelector {
    /**
     * Select a time window from market data
     * @param marketData Historical tick data
     * @return Selected time window for execution
     */
    TimeWindow select(List<TickData> marketData);

    /**
     * Get selector description for logging
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }
}