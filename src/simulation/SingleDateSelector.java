package simulation;

import ingestion.MarketDataManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Selects a single specific date
 */
public class SingleDateSelector implements DateSelector {
    private final LocalDate date;

    public SingleDateSelector(String dateStr) {
        this.date = LocalDate.parse(dateStr);
    }

    public SingleDateSelector(LocalDate date) {
        this.date = date;
    }

    @Override
    public List<LocalDate> selectDates(List<LocalDate> availableDates, MarketDataManager dataManager) {
        if (!availableDates.contains(date)) {
            throw new IllegalArgumentException("Date not available: " + date);
        }
        return List.of(date);
    }

    @Override
    public String getDescription() {
        return "Single date: " + date;
    }
}