package simulation;

import data.TickData;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Selects a time window starting at a specific time of day
 * Duration comes from OrderTemplate
 */
public class CustomTimeSelector implements TimeWindowSelector {
    private final LocalTime startTime;

    public CustomTimeSelector(LocalTime startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        this.startTime = startTime;
    }

    @Override
    public TimeWindow select(List<TickData> marketData) {
        if (marketData == null || marketData.isEmpty()) {
            throw new IllegalArgumentException("Market data cannot be empty");
        }

        // Find first tick at or after target time
        Instant targetStart = null;
        for (TickData tick : marketData) {
            Instant tickTime = tick.getTimestamp();
            LocalTime tickLocalTime = tickTime.atZone(ZoneId.systemDefault()).toLocalTime();

            if (!tickLocalTime.isBefore(startTime)) {
                targetStart = tickTime;
                break;
            }
        }

        if (targetStart == null) {
            // Target time is after all ticks, use last tick
            targetStart = marketData.get(marketData.size() - 1).getTimestamp();
        }

        // Return start time only - duration will be added by caller
        return new TimeWindow(targetStart, targetStart);
    }

    @Override
    public String getDescription() {
        return String.format("Starting at %s", startTime);
    }
}