package strategy;

import data.Fill;
import data.Order;
import data.TickData;
import execution.MarketState;
import execution.OrderDecision;
import util.SimulationLogger;

import java.util.List;

@Strategy(
    name = "TWAP",
    description = "Time-Weighted Average Price execution - splits order into equal time slices",
    parameters = {
        @Parameter(
            name = "numberOfSlices",
            type = int.class,
            description = "Number of time slices to split the order into"
        ),
        @Parameter(
            name = "orderType",
            type = TwapStrategy.OrderType.class,
            description = "Order type: MARKET, LIMIT_AGGRESSIVE, or LIMIT_PASSIVE",
            defaultValue = "MARKET"
        ),
        @Parameter(
            name = "aggressivenessBps",
            type = double.class,
            description = "For LIMIT_AGGRESSIVE: basis points beyond bid/ask (e.g., 10 = 0.1%)",
            defaultValue = "0"
        )
    }
)
public class TwapStrategy implements ExecutionStrategy {

    /**
     * Order type for TWAP execution
     */
    public enum OrderType {
        MARKET,
        LIMIT_AGGRESSIVE,
        LIMIT_PASSIVE
    }

    private final int numberOfSlices;
    private final OrderType orderType;
    private final double aggressivenessBps;

    // Strategy state
    private Order parentOrder;
    private int remainingQuantity;
    private int currentSlice = 0;
    private long sliceIntervalMs;
    private long nextSliceTimeMs;

    private int expectedQtyThisSlice = 0;
    private int filledQtyThisSlice = 0;

    /**
     * Create TWAP strategy with market orders (default)
     */
    public TwapStrategy(int numberOfSlices) {
        this(numberOfSlices, OrderType.MARKET, 0);
    }

    /**
     * Create TWAP strategy with configurable order type
     * @param numberOfSlices Number of time slices to split order
     * @param orderType Type of orders to send (MARKET, LIMIT_AGGRESSIVE, LIMIT_PASSIVE)
     * @param aggressivenessBps For LIMIT_AGGRESSIVE: basis points beyond bid/ask (e.g., 10 = 0.1%)
     */
    public TwapStrategy(int numberOfSlices, OrderType orderType, double aggressivenessBps) {
        if (numberOfSlices <= 0) {
            throw new IllegalArgumentException("Number of slices must be positive");
        }
        this.numberOfSlices = numberOfSlices;
        this.orderType = orderType;
        this.aggressivenessBps = aggressivenessBps;
    }

    @Override
    public void initialize(Order parentOrder, List<TickData> marketData) {
        this.parentOrder = parentOrder;
        this.remainingQuantity = parentOrder.getQuantity();
        this.currentSlice = 0;

        long startTimeMs = parentOrder.getStartTime().toEpochMilli();
        long endTimeMs = parentOrder.getEndTime().toEpochMilli();
        long totalDuration = endTimeMs - startTimeMs;

        if (totalDuration <= 0) {
            throw new IllegalArgumentException("Parent order end time must be after start time");
        }

        this.sliceIntervalMs = totalDuration / numberOfSlices;
        this.nextSliceTimeMs = startTimeMs + (sliceIntervalMs / 2); // First slice at midpoint

        SimulationLogger.log("\n[TWAP] Initialized");
        SimulationLogger.log("  Parent Order: " + parentOrder.getOrderId() +
                           " | Ticker: " + parentOrder.getSymbol() +
                           " | Qty: " + parentOrder.getQuantity() +
                           " | Side: " + parentOrder.getSide());
        SimulationLogger.log("  Slices: " + numberOfSlices);
        SimulationLogger.log("  Slice interval: " + (sliceIntervalMs / 1000) + "s");
        SimulationLogger.log("  Quantity per slice: " + (parentOrder.getQuantity() / numberOfSlices));
    }

    @Override
    public OrderDecision onTick(MarketState currentState) {

        if (remainingQuantity <= 0 || currentSlice >= numberOfSlices) {
            return OrderDecision.noAction();
        }

        if (currentState.getCurrentTimeMs() >= nextSliceTimeMs) {

            if (expectedQtyThisSlice == 0) {
                // Distribute quantity evenly, with remainder going to first slices
                int baseQtyPerSlice = parentOrder.getQuantity() / numberOfSlices;
                int remainder = parentOrder.getQuantity() % numberOfSlices;

                // First 'remainder' slices get 1 extra share
                if (currentSlice < remainder) {
                    expectedQtyThisSlice = baseQtyPerSlice + 1;
                } else {
                    expectedQtyThisSlice = baseQtyPerSlice;
                }
                filledQtyThisSlice = 0;

                SimulationLogger.log("\n[TWAP] Slice " + (currentSlice + 1) + "/" + numberOfSlices +
                                   " | Expected qty: " + expectedQtyThisSlice +
                                   " | Remaining total: " + remainingQuantity);
            }

            int qtyToOrder = expectedQtyThisSlice - filledQtyThisSlice;
            String orderId = parentOrder.getOrderId() + "_TWAP_" + (currentSlice + 1);

            if (filledQtyThisSlice > 0) {
                SimulationLogger.log("[TWAP] Retrying slice - already filled: " + filledQtyThisSlice +
                                   " | Ordering: " + qtyToOrder);
            }

            switch (orderType) {
                case MARKET:
                    return OrderDecision.marketOrder(orderId, qtyToOrder);

                case LIMIT_AGGRESSIVE:
                    double aggressivePrice = calculateAggressivePrice(currentState);
                    return OrderDecision.limitOrder(orderId, qtyToOrder, aggressivePrice);

                case LIMIT_PASSIVE:
                    double passivePrice = calculatePassivePrice(currentState);
                    return OrderDecision.limitOrder(orderId, qtyToOrder, passivePrice);

                default:
                    throw new IllegalStateException("Unknown order type: " + orderType);
            }
        }

        return OrderDecision.noAction();
    }

    @Override
    public void onFill(Fill fill) {
        remainingQuantity -= fill.getQuantity();
        filledQtyThisSlice += fill.getQuantity();

        SimulationLogger.log("[TWAP] Fill received: " + fill.getQuantity() +
                           " | Slice progress: " + filledQtyThisSlice + "/" + expectedQtyThisSlice +
                           " | Remaining total: " + remainingQuantity);

        if (!fill.isPartial()) {
            SimulationLogger.log("[TWAP] Slice " + (currentSlice + 1) + " complete");
            currentSlice++;
            nextSliceTimeMs += sliceIntervalMs;
            expectedQtyThisSlice = 0;
            filledQtyThisSlice = 0;
        } else {
            SimulationLogger.log("[TWAP] Partial fill - will retry on next tick");
        }
    }

    @Override
    public boolean isComplete() {
        return remainingQuantity <= 0 || currentSlice >= numberOfSlices;
    }

    /**
     * Calculate aggressive limit price that crosses spread
     * BUY: ask * (1 + aggressivenessBps/10000)
     * SELL: bid * (1 - aggressivenessBps/10000)
     */
    private double calculateAggressivePrice(MarketState state) {
        double aggressiveFactor = aggressivenessBps / 10000.0;

        if (parentOrder.getSide() == util.Side.BUY) {
            return state.getAsk() * (1 + aggressiveFactor);
        } else {
            return state.getBid() * (1 - aggressiveFactor);
        }
    }

    /**
     * Calculate passive limit price at best bid/ask
     * BUY: bid (join best bid)
     * SELL: ask (join best ask)
     */
    private double calculatePassivePrice(MarketState state) {
        if (parentOrder.getSide() == util.Side.BUY) {
            return state.getBid();
        } else {
            return state.getAsk();
        }
    }

    @Override
    public String getName() {
        switch (orderType) {
            case MARKET:
                return "TWAP(" + numberOfSlices + ")";
            case LIMIT_AGGRESSIVE:
                return "TWAP(" + numberOfSlices + ", Aggressive " + aggressivenessBps + "bps)";
            case LIMIT_PASSIVE:
                return "TWAP(" + numberOfSlices + ", Passive)";
            default:
                return "TWAP(" + numberOfSlices + ")";
        }
    }
}