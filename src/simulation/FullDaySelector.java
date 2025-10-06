package simulation;

import data.TickData;

import java.util.List;

/**
 * Selects the entire market data range as the execution window
 */
public class FullDaySelector implements TimeWindowSelector {

    @Override
    public TimeWindow select(List<TickData> marketData) {
        if (marketData == null || marketData.isEmpty()) {
            throw new IllegalArgumentException("Market data cannot be empty");
        }

        return new TimeWindow(
            marketData.get(174).getTimestamp(),
            marketData.get(3359).getTimestamp()
        );
    }

    public String getDescription() {
        return "FullDay";
    }
}