package data;

import util.Side;
import java.time.Duration;
import java.time.Instant;

/**
 * Template for creating orders with relative duration
 * Decouples order specification from absolute execution times
 */
public class OrderTemplate {
    private final String symbol;
    private final int quantity;
    private final Side side;
    private final Duration duration;

    private OrderTemplate(String symbol, int quantity, Side side, Duration duration) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.side = side;
        this.duration = duration;
    }

    /**
     * Materialize template into concrete Order with absolute times
     * @param orderId Order identifier
     * @param startTime Absolute start time
     * @return Concrete Order instance
     */
    public Order materialize(String orderId, Instant startTime) {
        Instant endTime = startTime.plus(duration);
        return new Order(
            orderId,
            symbol,
            quantity,
            side,
            startTime,
            endTime
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public Side getSide() {
        return side;
    }

    public String getSideString() {
        return side.getValue();
    }

    public Duration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return String.format("OrderTemplate{%s %s %d over %s}",
            side, symbol, quantity, formatDuration(duration));
    }

    private String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        if (hours > 0) {
            return minutes > 0 ? hours + "h " + minutes + "m" : hours + "h";
        }
        return minutes + "m";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String symbol;
        private int quantity;
        private Side side;
        private Duration duration;

        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder side(Side side) {
            this.side = side;
            return this;
        }

        // Convenience method for String side
        public Builder side(String sideStr) {
            this.side = Side.from(sideStr);
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Convenience method for duration in hours
         */
        public Builder durationHours(long hours) {
            this.duration = Duration.ofHours(hours);
            return this;
        }

        /**
         * Convenience method for duration in minutes
         */
        public Builder durationMinutes(long minutes) {
            this.duration = Duration.ofMinutes(minutes);
            return this;
        }

        public OrderTemplate build() {
            if (symbol == null || symbol.isEmpty()) {
                throw new IllegalStateException("Symbol is required");
            }
            if (quantity <= 0) {
                throw new IllegalStateException("Quantity must be positive");
            }
            if (side == null) {
                throw new IllegalStateException("Side is required");
            }
            if (duration == null || duration.isNegative() || duration.isZero()) {
                throw new IllegalStateException("Duration must be positive");
            }
            return new OrderTemplate(symbol, quantity, side, duration);
        }
    }
}