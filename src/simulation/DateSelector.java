package simulation;

import ingestion.MarketDataManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface for selecting dates from available market data
 * Enables testing across different market conditions (volatile days, liquid days, etc.)
 */
public interface DateSelector {

    /**
     * Select dates from available market data
     * @param availableDates All dates with available data
     * @param dataManager Access to market data for analysis
     * @return Selected dates for testing
     */
    List<LocalDate> selectDates(List<LocalDate> availableDates, MarketDataManager dataManager);

    /**
     * Human-readable description of selection criteria
     */
    String getDescription();
}