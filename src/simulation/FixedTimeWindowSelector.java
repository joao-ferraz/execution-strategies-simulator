package simulation;

import data.TickData;

import java.util.List;

/**
 * Time window selector that returns a fixed, pre-determined time window
 * Used internally when time window has already been calculated
 */
class FixedTimeWindowSelector implements TimeWindowSelector {
    private final TimeWindow window;

    public FixedTimeWindowSelector(TimeWindow window) {
        this.window = window;
    }

    @Override
    public TimeWindow select(List<TickData> marketData) {
        return window;
    }

    @Override
    public String getDescription() {
        return "Fixed window: " + window;
    }
}
