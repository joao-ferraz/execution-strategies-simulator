package simulation;

import ingestion.MarketDataManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Randomly selects N days from available dates
 */
public class RandomDaysSelector implements DateSelector {
    private final int count;
    private final Random random;

    public RandomDaysSelector(int count) {
        this(count, new Random());
    }

    public RandomDaysSelector(int count, long seed) {
        this(count, new Random(seed));
    }

    private RandomDaysSelector(int count, Random random) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        this.count = count;
        this.random = random;
    }

    @Override
    public List<LocalDate> selectDates(List<LocalDate> availableDates, MarketDataManager dataManager) {
        if (availableDates.size() <= count) {
            return new ArrayList<>(availableDates);
        }

        List<LocalDate> shuffled = new ArrayList<>(availableDates);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, count);
    }

    @Override
    public String getDescription() {
        return count + " random days";
    }
}