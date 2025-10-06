package data;

import util.Side;
import java.time.Instant;

public class Order {
    private final String symbol;
    private final int quantity;
    private final Side side;
    private final Instant startTime;
    private final Instant endTime;
    private final String orderId;

    public Order(String orderId, String symbol, int quantity, Side side, Instant startTime, Instant endTime) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.side = side;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Child order constructor (no time window)
    public Order(String orderId, String symbol, int quantity, Side side, Instant targetTime) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.side = side;
        this.startTime = targetTime;
        this.endTime = targetTime;
    }

    // Convenience constructor for String side (converts to enum)
    public Order(String orderId, String symbol, int quantity, String sideStr, Instant startTime, Instant endTime) {
        this(orderId, symbol, quantity, Side.from(sideStr), startTime, endTime);
    }

    // Convenience constructor for String side (no time window)
    public Order(String orderId, String symbol, int quantity, String sideStr, Instant targetTime) {
        this(orderId, symbol, quantity, Side.from(sideStr), targetTime);
    }

    public String getOrderId() {
        return orderId;
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

    // Convenience method for backward compatibility
    public String getSideString() {
        return side.getValue();
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", side='" + side + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
