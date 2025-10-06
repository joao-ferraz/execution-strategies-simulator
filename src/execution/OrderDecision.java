package execution;

public class OrderDecision {

    public enum Type {
        MARKET,
        LIMIT,
        NO_ACTION
    }

    private final Type type;
    private final int quantity;
    private final Double limitPrice;  // null for market orders
    private final String orderId;

    private OrderDecision(Type type, int quantity, Double limitPrice, String orderId) {
        this.type = type;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.orderId = orderId;
    }

    public static OrderDecision noAction() {
        return new OrderDecision(Type.NO_ACTION, 0, null, null);
    }

    public static OrderDecision marketOrder(String orderId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive");
        }
        return new OrderDecision(Type.MARKET, quantity, null, orderId);
    }

    public static OrderDecision limitOrder(String orderId, int quantity, double limitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive");
        }
        if (limitPrice <= 0) {
            throw new IllegalArgumentException("Limit price must be positive");
        }
        return new OrderDecision(Type.LIMIT, quantity, limitPrice, orderId);
    }

    public Type getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public Double getLimitPrice() {
        return limitPrice;
    }

    public String getOrderId() {
        return orderId;
    }

    public boolean isNoAction() {
        return type == Type.NO_ACTION;
    }

    @Override
    public String toString() {
        if (type == Type.NO_ACTION) {
            return "OrderDecision{NO_ACTION}";
        }
        return "OrderDecision{" +
                "type=" + type +
                ", orderId='" + orderId + '\'' +
                ", quantity=" + quantity +
                (limitPrice != null ? ", limitPrice=" + limitPrice : "") +
                '}';
    }
}