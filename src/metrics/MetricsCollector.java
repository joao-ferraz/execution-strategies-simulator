package metrics;

import data.Fill;
import data.Order;
import execution.MarketState;
import execution.OrderDecision;
import events.ExecutionEventListener;
import util.Side;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects execution context and calculates performance metrics
 * Implements ExecutionEventListener to observe execution lifecycle
 */
public class MetricsCollector implements ExecutionEventListener {

    // Arrival context
    private Order parentOrder;
    private double arrivalPrice;
    private double arrivalMid;
    private long arrivalTime;

    // Decision tracking
    private final List<DecisionContext> decisions = new ArrayList<>();

    // Market benchmarks
    private double marketVWAP = 0.0;
    private double marketMidVWAP = 0.0;
    private double totalMarketValue = 0.0;
    private double totalMarketVolume = 0.0;
    private double totalMidValue = 0.0;
    private long totalMidTime = 0;
    private double lastMid = 0.0;
    private long lastTickTime = 0;

    // Fill tracking with market context
    private final List<FillContext> fillContexts = new ArrayList<>();

    @Override
    public void onExecutionStart(Order parentOrder, MarketState initialState) {
        this.parentOrder = parentOrder;
        this.arrivalTime = initialState.getCurrentTimeMs();
        this.arrivalMid = initialState.getMidPrice();

        this.arrivalPrice = parentOrder.getSide().getTouchPrice(initialState.getBid(), initialState.getAsk());

        this.lastMid = arrivalMid;
        this.lastTickTime = arrivalTime;
    }

    @Override
    public void onTick(MarketState state) {
        double volume = state.getVolume();
        double tradePrice = state.getCurrentTick().getTp();

        if (volume > 0 && tradePrice > 0) {
            totalMarketValue += tradePrice * volume;
            totalMarketVolume += volume;
            marketVWAP = totalMarketValue / totalMarketVolume;
        }

        long currentTime = state.getCurrentTimeMs();
        if (lastTickTime > 0) {
            long timeDelta = currentTime - lastTickTime;
            totalMidValue += lastMid * timeDelta;
            totalMidTime += timeDelta;
            marketMidVWAP = totalMidValue / totalMidTime;
        }

        lastMid = state.getMidPrice();
        lastTickTime = currentTime;
    }

    @Override
    public void onDecision(OrderDecision decision, MarketState state) {

        boolean isAmendment = decisions.stream()
                .anyMatch(d -> d.orderId.equals(decision.getOrderId()));

        decisions.add(new DecisionContext(
            decision.getOrderId(),
            state.getCurrentTime(),
            state.getMidPrice(),
            decision.getQuantity(),
            isAmendment
        ));
    }

    @Override
    public void onFill(Fill fill, MarketState state) {
        fillContexts.add(new FillContext(
            fill,
            state.getMidPrice(),
            state.getSpread(),
            state.getCurrentTime()
        ));
    }

    @Override
    public void onExecutionComplete(List<Fill> fills, MarketState finalState) {
        // All data collected - ready to calculate metrics
    }

    /**
     * Calculate comprehensive execution metrics
     * Call after execution completes
     */
    public ExecutionMetrics calculateMetrics() {
        if (parentOrder == null) {
            throw new IllegalStateException("No execution data collected");
        }

        double executionVWAP = calculateExecutionVWAP();
        double totalFilled = calculateTotalFilled();
        Side side = parentOrder.getSide();

        double arrivalSlippage = calculateSlippage(executionVWAP, arrivalPrice, side);

        double decisionSlippage = calculateDecisionSlippage(side);

        double vwapSlippage = calculateSlippage(executionVWAP, marketVWAP, side);

        double midSlippage = calculateSlippage(executionVWAP, marketMidVWAP, side);

        double implementationShortfall = calculateImplementationShortfall(executionVWAP, side);

        double fillRatio = totalFilled / parentOrder.getQuantity();

        double fillEfficiency = calculateFillEfficiency();

        double immediateExecutionRatio = calculateImmediateExecutionRatio();

        double avgAmendmentsPerOrder = calculateAvgAmendmentsPerOrder();

        return new ExecutionMetrics(
            arrivalSlippage,
            decisionSlippage,
            vwapSlippage,
            midSlippage,
            implementationShortfall,
            fillRatio,
            fillEfficiency,
            immediateExecutionRatio,
            avgAmendmentsPerOrder,
            arrivalPrice,
            executionVWAP,
            marketVWAP,
            marketMidVWAP
        );
    }

    private double calculateExecutionVWAP() {
        double totalValue = 0.0;
        double totalVolume = 0.0;
        for (FillContext fc : fillContexts) {
            totalValue += fc.fill.getPrice() * fc.fill.getQuantity();
            totalVolume += fc.fill.getQuantity();
        }
        return totalVolume > 0 ? totalValue / totalVolume : 0.0;
    }

    private double calculateTotalFilled() {
        double total = 0.0;
        for (FillContext fc : fillContexts) {
            total += fc.fill.getQuantity();
        }
        return total;
    }

    private double calculateSlippage(double executionPrice, double referencePrice, Side side) {
        if (referencePrice == 0) return 0.0;

        // Slippage in bps (positive = cost, negative = improvement)
        double diff = executionPrice - referencePrice;
        if (side == Side.SELL) diff = -diff; // Invert for sells

        return (diff / referencePrice) * 10000; // bps
    }

    private double calculateDecisionSlippage(Side side) {
        if (decisions.isEmpty()) return 0.0;

        double totalSlippage = 0.0;
        int count = 0;

        for (DecisionContext dc : decisions) {
            if (dc.isAmendment) continue;  // Skip amendments

            double totalValue = 0.0;
            double totalQty = 0.0;

            for (FillContext fc : fillContexts) {
                if (fc.fill.getOrderId().equals(dc.orderId)) {
                    totalValue += fc.fill.getPrice() * fc.fill.getQuantity();
                    totalQty += fc.fill.getQuantity();
                }
            }

            if (totalQty > 0) {
                double fillVWAP = totalValue / totalQty;
                double slippage = calculateSlippage(fillVWAP, dc.marketMid, side);
                totalSlippage += slippage;
                count++;
            }
        }

        return count > 0 ? totalSlippage / count : 0.0;
    }

    private double calculateImplementationShortfall(double executionVWAP, Side side) {
        // IS = (executionVWAP - arrivalPrice) in bps
        return calculateSlippage(executionVWAP, arrivalPrice, side);
    }

    /**
     * Calculate fill efficiency: totalFilled / totalRequested
     * Penalizes order amendments based on quantity re-requested
     */
    private double calculateFillEfficiency() {
        if (decisions.isEmpty()) return 1.0;

        double totalRequested = decisions.stream()
                .mapToDouble(d -> d.quantity)
                .sum();

        double totalFilled = fillContexts.stream()
                .mapToDouble(fc -> fc.fill.getQuantity())
                .sum();

        return totalRequested > 0 ? totalFilled / totalRequested : 1.0;
    }

    /**
     * Calculate immediate execution ratio: orders filled immediately / total orders
     * An order is "immediate" if it has exactly 1 fill and that fill is not partial
     */
    private double calculateImmediateExecutionRatio() {
        if (fillContexts.isEmpty()) return 1.0;

        // Group fills by orderId
        java.util.Map<String, java.util.List<Fill>> fillsByOrderId = fillContexts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        fc -> fc.fill.getOrderId(),
                        java.util.stream.Collectors.mapping(fc -> fc.fill, java.util.stream.Collectors.toList())
                ));

        long immediateOrders = fillsByOrderId.values().stream()
                .filter(fills -> fills.size() == 1 && !fills.get(0).isPartial())
                .count();

        return (double) immediateOrders / fillsByOrderId.size();
    }

    /**
     * Calculate average amendments per order: totalDecisions / uniqueOrderIds
     * Shows how many times on average each order needed to be amended/modified
     * Value of 1.0 means no amendments needed, >1.0 means orders required amendments
     */
    private double calculateAvgAmendmentsPerOrder() {
        if (decisions.isEmpty()) return 1.0;

        long uniqueOrders = decisions.stream()
                .map(d -> d.orderId)
                .distinct()
                .count();

        return (double) decisions.size() / Math.max(1, uniqueOrders);
    }

    /**
     * Context for a decision event
     */
    private static class DecisionContext {
        final String orderId;
        final Instant timestamp;
        final double marketMid;
        final double quantity;
        final boolean isAmendment;

        DecisionContext(String orderId, Instant timestamp, double marketMid, double quantity, boolean isAmendment) {
            this.orderId = orderId;
            this.timestamp = timestamp;
            this.marketMid = marketMid;
            this.quantity = quantity;
            this.isAmendment = isAmendment;
        }
    }

    /**
     * Context for a fill event
     */
    private static class FillContext {
        final Fill fill;
        final double marketMid;
        final double marketSpread;
        final Instant timestamp;

        FillContext(Fill fill, double marketMid, double marketSpread, Instant timestamp) {
            this.fill = fill;
            this.marketMid = marketMid;
            this.marketSpread = marketSpread;
            this.timestamp = timestamp;
        }
    }
}
