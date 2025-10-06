package util;

/**
 * Represents the side of an order or trade (BUY or SELL)
 * Provides utility methods to eliminate BUY/SELL branching logic
 */
public enum Side {
    BUY("BUY"),
    SELL("SELL");

    private final String value;

    Side(String value) {
        this.value = value;
    }

    /**
     * Convert string representation to Side enum
     */
    public static Side from(String value) {
        if ("BUY".equalsIgnoreCase(value)) {
            return BUY;
        } else if ("SELL".equalsIgnoreCase(value)) {
            return SELL;
        }
        throw new IllegalArgumentException("Invalid side: " + value + ". Must be BUY or SELL");
    }

    public String getValue() {
        return value;
    }

    /**
     * Get the touch price (best executable price) for this side
     * BUY: ask (lift the offer)
     * SELL: bid (hit the bid)
     */
    public double getTouchPrice(double bid, double ask) {
        return this == BUY ? ask : bid;
    }

    /**
     * Check if a limit order at given price is aggressive (crosses spread)
     * BUY: limit >= ask
     * SELL: limit <= bid
     */
    public boolean isAggressiveLimit(double limitPrice, double bid, double ask) {
        return this == BUY ? limitPrice >= ask : limitPrice <= bid;
    }

    /**
     * Check if a limit order is passive (at or inside spread)
     * BUY: bid <= limit < ask
     * SELL: bid < limit <= ask
     */
    public boolean isPassiveLimit(double limitPrice, double bid, double ask) {
        return this == BUY
            ? limitPrice >= bid && limitPrice < ask
            : limitPrice <= ask && limitPrice > bid;
    }

    /**
     * Check if a limit order is at touch
     */
    public boolean isAtTouch(double limitPrice, double bid, double ask){
        return Math.abs(getTouchPrice(bid, ask) - limitPrice) < 1e-5;
    }

    /**
     * Check if a limit order would be filled by the given trade price
     * BUY: trade price <= limit (market traded at or below our buy limit)
     * SELL: trade price >= limit (market traded at or above our sell limit)
     */
    public boolean matchesTradePrice(double limitPrice, double tradePrice) {
        if (tradePrice <= 0) return false;
        return this == BUY ? tradePrice <= limitPrice : tradePrice >= limitPrice;
    }

    @Override
    public String toString() {
        return value;
    }
}