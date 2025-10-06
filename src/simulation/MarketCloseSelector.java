package simulation;

import data.TickData;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Selects a time window ending at market close
 * Duration comes from OrderTemplate - this selector calculates start time as (close - duration)
 */
public class MarketCloseSelector implements TimeWindowSelector {
    private Duration orderDuration; // Set by caller before select()

    public MarketCloseSelector() {
        // No parameters needed - duration comes from order
    }

    /**
     * Set order duration (called by SimulationBatch before select())
     */
    public void setOrderDuration(Duration duration) {
        this.orderDuration = duration;
    }

    @Override
    public TimeWindow select(List<TickData> marketData) {
        if (marketData == null || marketData.isEmpty()) {
            throw new IllegalArgumentException("Market data cannot be empty");
        }
        if (orderDuration == null) {
            throw new IllegalStateException("Order duration not set - call setOrderDuration() first");
        }

        Instant end = marketData.get(marketData.size() - 1).getTimestamp();
        Instant start = end.minus(orderDuration);

        return new TimeWindow(start, end);
    }

    @Override
    public String getDescription() {
        return "Market close";
    }
}