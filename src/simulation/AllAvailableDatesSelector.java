package simulation;

import ingestion.MarketDataManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Selects all available dates
 */
public class AllAvailableDatesSelector implements DateSelector {

    @Override
    public List<LocalDate> selectDates(List<LocalDate> availableDates, MarketDataManager dataManager) {
        return new ArrayList<>(availableDates);
    }

    @Override
    public String getDescription() {
        return "All available dates";
    }
}