package simulation;

import java.time.Duration;
import java.time.Instant;

public class TimeWindow {
    private final Instant startTime;
    private final Instant endTime;

    public TimeWindow(Instant startTime, Instant endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public long getDurationMs() {
        return Duration.between(startTime, endTime).toMillis();
    }

    @Override
    public String toString() {
        return "TimeWindow{" +
                "start=" + startTime +
                ", end=" + endTime +
                ", duration=" + (getDurationMs() / 1000) + "s" +
                '}';
    }
}