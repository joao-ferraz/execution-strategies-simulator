package data;

import util.Side;
import java.time.Instant;

public class Fill {
    private final String orderId;
    private final Instant timestamp;
    private final double price;
    private final int quantity;
    private final Side side;
    private final boolean isPartial;
    private final int requestedQuantity;

    public Fill(String orderId, Instant timestamp, double price, int quantity, Side side) {
        this(orderId, timestamp, price, quantity, side, false, quantity);
    }

    public Fill(String orderId, Instant timestamp, double price, int quantity, Side side,
                boolean isPartial, int requestedQuantity) {
        this.orderId = orderId;
        this.timestamp = timestamp;
        this.price = price;
        this.quantity = quantity;
        this.side = side;
        this.isPartial = isPartial;
        this.requestedQuantity = requestedQuantity;
    }

    // Convenience constructor for String side
    public Fill(String orderId, Instant timestamp, double price, int quantity, String sideStr) {
        this(orderId, timestamp, price, quantity, Side.from(sideStr), false, quantity);
    }

    // Convenience constructor for String side with partial
    public Fill(String orderId, Instant timestamp, double price, int quantity, String sideStr,
                boolean isPartial, int requestedQuantity) {
        this(orderId, timestamp, price, quantity, Side.from(sideStr), isPartial, requestedQuantity);
    }

    public String getOrderId() {
        return orderId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public double getPrice() {
        return price;
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

    public boolean isPartial() {
        return isPartial;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getRemainingQuantity() {
        return requestedQuantity - quantity;
    }

    @Override
    public String toString() {
        return "Fill{" +
                "orderId='" + orderId + '\'' +
                ", timestamp=" + timestamp +
                ", price=" + price +
                ", quantity=" + quantity +
                (isPartial ? " (PARTIAL, requested=" + requestedQuantity + ")" : "") +
                ", side='" + side + '\'' +
                '}';
    }
}
