package util;

import java.util.List;

/**
 * Utility for calculating Volume-Weighted Average Price (VWAP)
 * Can be used across strategies, metrics, and execution simulation
 */
public class VwapCalculator {

    /**
     * Calculate VWAP from a collection of price-volume pairs
     * @param priceVolumePairs List of price-volume pairs
     * @return Weighted average price, or 0 if total volume is 0
     */
    public static double calculate(List<PriceVolumePair> priceVolumePairs) {
        double totalValue = 0;
        double totalVolume = 0;

        for (PriceVolumePair pair : priceVolumePairs) {
            totalValue += pair.price * pair.volume;
            totalVolume += pair.volume;
        }

        return totalVolume > 0 ? totalValue / totalVolume : 0;
    }

    /**
     * Calculate VWAP for two price levels (common case for book consumption)
     * @param price1 First price level
     * @param volume1 Volume at first price level
     * @param price2 Second price level
     * @param volume2 Volume at second price level
     * @return Weighted average price
     */
    public static double calculate(double price1, double volume1,
                                   double price2, double volume2) {
        double totalVolume = volume1 + volume2;
        return totalVolume > 0
            ? (price1 * volume1 + price2 * volume2) / totalVolume
            : 0;
    }

    /**
     * Calculate linear VWAP for continuous book consumption
     * Assumes linear price distribution from touchPrice to (touchPrice + depth)
     * Used when modeling orders that "walk up/down" the book
     *
     * For example: BUY order consuming from ask=100 to ask+depth=100.10
     * Average execution price = 100.05
     *
     * @param touchPrice Best bid/ask price (starting point)
     * @param depth Price distance consumed into book (always positive)
     * @return Average execution price for the consumed depth
     */
    public static double calculateLinearVwap(double touchPrice, double depth) {
        // For uniform linear distribution, average price is at midpoint
        return touchPrice + (depth / 2.0);
    }

    /**
     * Immutable price-volume pair for VWAP calculation
     * Represents a single execution or book level
     */
    public static class PriceVolumePair {
        public final double price;
        public final double volume;

        public PriceVolumePair(double price, double volume) {
            this.price = price;
            this.volume = volume;
        }

        @Override
        public String toString() {
            return String.format("%.2f@%.4f", volume, price);
        }
    }
}