package simulation;

import data.TickData;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Selects a time window starting from market open
 * Duration comes from OrderTemplate
 */
public class MarketOpenSelector implements TimeWindowSelector {

    public MarketOpenSelector() {
        // No parameters needed - just selects start time
    }

    @Override
    public TimeWindow select(List<TickData> marketData) {
        if (marketData == null || marketData.isEmpty()) {
            throw new IllegalArgumentException("Market data cannot be empty");
        }

        // Return start time only - duration will be added by caller
        Instant start = marketData.get(0).getTimestamp();

        // Temporary: return dummy window, will be replaced by caller with order duration
        return new TimeWindow(start, start);
    }

    @Override
    public String getDescription() {
        return "Market open";
    }
}