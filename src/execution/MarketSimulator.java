package execution;

import data.Fill;
import data.Order;
import data.TickData;
import events.ExecutionEventListener;
import strategy.ExecutionStrategy;
import util.Side;
import util.SimulationLogger;
import util.VwapCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MarketSimulator {
    private static final double DEFAULT_MAX_PARTICIPATION = 0.5;
    private static final double DEFAULT_DEPTH_FACTOR = 0.5;
    private static final long DEFAULT_LATENCY_MS = 0;

    private static final double TIER1_AT_TP = 0.40;
    private static final double TIER2_MILDLY_AGGRESSIVE = 0.50;
    private static final double TIER3_AT_TOUCH = 0.60;

    private final double maxParticipationRate;
    private final double depthFactor;
    private final long latencyMs;
    private final Random random;
    private final List<ExecutionEventListener> listeners = new ArrayList<>();

    public MarketSimulator() {
        this(DEFAULT_MAX_PARTICIPATION, DEFAULT_DEPTH_FACTOR, DEFAULT_LATENCY_MS);
    }

    public MarketSimulator(double maxParticipationRate) {
        this(maxParticipationRate, DEFAULT_DEPTH_FACTOR, DEFAULT_LATENCY_MS);
    }

    public MarketSimulator(double maxParticipationRate, double depthFactor) {
        this(maxParticipationRate, depthFactor, DEFAULT_LATENCY_MS);
    }

    public MarketSimulator(double maxParticipationRate, double depthFactor, long latencyMs) {
        this.maxParticipationRate = maxParticipationRate;
        this.depthFactor = depthFactor;
        this.latencyMs = latencyMs;
        this.random = new Random();
    }

    public MarketSimulator(double maxParticipationRate, double depthFactor, long latencyMs, long seed) {
        this.maxParticipationRate = maxParticipationRate;
        this.depthFactor = depthFactor;
        this.latencyMs = latencyMs;
        this.random = new Random(seed);
    }

    /**
     * Run tick-by-tick simulation of strategy execution
     * Orders from previous tick are executed first, then new orders are generated
     * This prevents look-ahead bias and simulates realistic execution latency
     */
    public List<Fill> simulate(ExecutionStrategy strategy, Order parentOrder, List<TickData> marketData) {
        SimulationLogger.log("\n[MARKET_SIMULATOR] Starting simulation");
        SimulationLogger.log("  Max participation rate: " + (maxParticipationRate * 100) + "%");
        SimulationLogger.log("  Execution latency: " + latencyMs + "ms");

        strategy.initialize(parentOrder, marketData);

        List<Fill> fills = new ArrayList<>();
        OrderDecision previousDecision = null;
        long previousDecisionTime = 0;

        // Notify execution start
        MarketState initialState = new MarketState(marketData.get(0));
        notifyExecutionStart(parentOrder, initialState);

        for (TickData tick : marketData) {
            MarketState state = new MarketState(tick);
            long currentTime = state.getCurrentTimeMs();

            // Notify tick
            notifyTick(state);

            if (previousDecision != null && !previousDecision.isNoAction()) {
                long timeSinceDecision = currentTime - previousDecisionTime;

                if (timeSinceDecision >= latencyMs) {
                    SimulationLogger.log(String.format(
                        "\n[EXECUTE] Processing order from previous tick (latency: %dms)",
                        timeSinceDecision
                    ));

                    Fill fill = executeOrder(previousDecision, state, parentOrder.getSide());

                    if (fill != null) {
                        fills.add(fill);

                        // Notify fill
                        notifyFill(fill, state);

                        strategy.onFill(fill);
                        if (fill.isPartial()) {
                            SimulationLogger.log("[PARTIAL_FILL] Filled " + fill.getQuantity() +
                                               " of " + fill.getRequestedQuantity());
                        }
                    }

                    previousDecision = null; // Clear after execution
                } else {
                    SimulationLogger.log(String.format(
                        "[LATENCY] Waiting for execution (elapsed: %dms, required: %dms)",
                        timeSinceDecision, latencyMs
                    ));
                }
            }

            OrderDecision decision = strategy.onTick(state);

            if (!decision.isNoAction()) {
                SimulationLogger.log("\n[DECISION] " + decision + " (queued for next tick)");

                // Notify decision
                notifyDecision(decision, state);

                previousDecision = decision;
                previousDecisionTime = currentTime;
            }

            if (strategy.isComplete()) {
                SimulationLogger.log("\n[COMPLETE] Strategy finished execution");
                break;
            }
        }

        // Warn if final order remains unexecuted
        if (previousDecision != null && !previousDecision.isNoAction()) {
            SimulationLogger.log("\n[WARNING] Final order not executed (no more ticks after latency)");
        }

        SimulationLogger.log("\n[MARKET_SIMULATOR] Simulation complete - " + fills.size() + " fills generated");

        // Notify execution complete
        MarketState finalState = new MarketState(marketData.get(marketData.size() - 1));
        notifyExecutionComplete(fills, finalState);

        return fills;
    }

    /**
     * Execute an order decision against market state
     * Orchestrates routing to appropriate execution method
     */
    private Fill executeOrder(OrderDecision decision, MarketState state, Side side) {
        ExecutionContext ctx = ExecutionContext.from(state, maxParticipationRate);

        if (!ctx.hasLiquidity()) {
            SimulationLogger.log("[REJECTED] Insufficient liquidity");
            return null;
        }

        FillResult result = routeOrderExecution(decision, ctx, side);

        if (result == null) {
            return null;
        }

        Fill fill = new Fill(
            decision.getOrderId(),
            state.getCurrentTime(),
            result.price,
            result.quantity,
            side,
            result.isPartial,
            decision.getQuantity()
        );

        SimulationLogger.logFill(fill);
        return fill;
    }

    /**
     * Route order to appropriate execution method based on type and aggressiveness tier
     * Uses trade-price-based tier system for deterministic fills
     */
    private FillResult routeOrderExecution(OrderDecision decision, ExecutionContext ctx, Side side) {
        if (decision.getType() == OrderDecision.Type.MARKET) {
            // Market orders execute at touch with tier 3 volume (60%)
            return executeTieredOrder(decision, ctx, side, 3);
        }

        double limitPrice = decision.getLimitPrice();
        double bid = ctx.getBid();
        double ask = ctx.getAsk();
        double tp = ctx.getTradePrice();
        double touch = side.getTouchPrice(bid, ask);

        // Non-competitive: BUY < bid or SELL > ask
        if ((side == Side.BUY && limitPrice < bid) || (side == Side.SELL && limitPrice > ask)) {
            SimulationLogger.log(String.format(
                "[ROUTING] Non-competitive limit order (price: %.4f, bid: %.4f, ask: %.4f) - no fill",
                limitPrice, bid, ask
            ));
            return null;
        }

        // Passive: bid <= BUY < tp or tp < SELL <= ask
        if (side.isPassiveLimit(limitPrice, bid, ask) && !isAtPrice(limitPrice, tp)) {
            return executePassiveLimitOrder(decision, ctx, side);
        }

        // Determine tier based on limit price position
        if (isAtPrice(limitPrice, tp)) {
            SimulationLogger.log("[ROUTING] At trade price - Tier 1");
            return executeTieredOrder(decision, ctx, side, 1);
        } else if (isBetween(limitPrice, tp, touch, side)) {
            SimulationLogger.log("[ROUTING] Mildly aggressive (between tp and touch) - Tier 2");
            return executeTieredOrder(decision, ctx, side, 2);
        } else if (isAtPrice(limitPrice, touch)) {
            SimulationLogger.log("[ROUTING] At touch - Tier 3");
            return executeTieredOrder(decision, ctx, side, 3);
        } else {
            SimulationLogger.log("[ROUTING] Aggressive (beyond touch) - Tier 4");
            return executeTieredOrder(decision, ctx, side, 4);
        }
    }

    /**
     * Check if price is approximately equal (within epsilon)
     */
    private boolean isAtPrice(double price1, double price2) {
        return Math.abs(price1 - price2) < 1e-5;
    }

    /**
     * Check if limitPrice is strictly between tp and touch
     */
    private boolean isBetween(double limitPrice, double tp, double touch, Side side) {
        if (side == Side.BUY) {
            return limitPrice > tp && limitPrice < touch;
        } else {
            return limitPrice < tp && limitPrice > touch;
        }
    }

    /**
     * Execute passive limit order - only fills when trade price crosses limit
     * No probabilistic fills - deterministic based on trade price
     */
    private FillResult executePassiveLimitOrder(OrderDecision decision, ExecutionContext ctx, Side side) {
        double limitPrice = decision.getLimitPrice();

        if (!side.isPassiveLimit(limitPrice, ctx.getBid(), ctx.getAsk())) {
            SimulationLogger.log("[LIMIT_NO_FILL] Non-competitive price");
            return null;
        }

        if (side.matchesTradePrice(limitPrice, ctx.getTradePrice())) {
            SimulationLogger.log("[LIMIT_FILL] Passive matched trade price -> fill at limit");
            int qty = (int) Math.min(decision.getQuantity(), Math.floor(ctx.getBaseVolume()));
            return new FillResult(limitPrice, qty, qty < decision.getQuantity());
        }

        SimulationLogger.log("[LIMIT_NO_FILL] Passive waiting for trade price to cross");
        return null;
    }

    /**
     * Execute tiered order based on aggressiveness level
     * Tiers determine available volume and fill sequentially across price levels
     *
     * @param tier 1=At TP (40%), 2=Mildly aggressive (50%), 3=At touch (60%), 4=Aggressive (60%+extra)
     */
    private FillResult executeTieredOrder(OrderDecision decision, ExecutionContext ctx, Side side, int tier) {
        double requestedQty = decision.getQuantity();
        double tp = ctx.getTradePrice();
        double touch = side.getTouchPrice(ctx.getBid(), ctx.getAsk());
        double tickVolume = ctx.getTickVolume();

        int tier1Vol = (int) Math.floor(tickVolume * TIER1_AT_TP);
        int tier2Vol = (int) Math.floor(tickVolume * TIER2_MILDLY_AGGRESSIVE);
        int tier3Vol = (int) Math.floor(tickVolume * TIER3_AT_TOUCH);

        int availableVolume;
        List<PriceLevel> levels = new ArrayList<>();

        switch (tier) {
            case 1: // At TP
                availableVolume = tier1Vol;
                levels.add(new PriceLevel(tier1Vol, tp));
                break;

            case 2: // Mildly aggressive (tp < limit < touch)
                availableVolume = tier2Vol;
                levels.add(new PriceLevel(tier1Vol, tp));
                levels.add(new PriceLevel(tier2Vol - tier1Vol, tp));
                break;

            case 3: // At touch
                availableVolume = tier3Vol;
                levels.add(new PriceLevel(tier1Vol, tp));
                levels.add(new PriceLevel(tier2Vol - tier1Vol, tp));
                levels.add(new PriceLevel(tier3Vol - tier2Vol, touch));
                break;

            case 4: // Aggressive (beyond touch)
                double limitPrice = decision.getLimitPrice();
                double priceDistance = Math.abs(limitPrice - touch);
                double aggressiveness = ctx.getSpread() > 0 ? priceDistance / ctx.getSpread() : 0;
                int extraVolume = (int) Math.floor(tier3Vol * aggressiveness * depthFactor);
                availableVolume = tier3Vol + extraVolume;

                levels.add(new PriceLevel(tier1Vol, tp));
                levels.add(new PriceLevel(tier2Vol - tier1Vol, tp));
                levels.add(new PriceLevel(tier3Vol - tier2Vol, touch));

                // Add extra depth with linear price progression
                if (extraVolume > 0) {
                    double depthPrice = VwapCalculator.calculateLinearVwap(touch, priceDistance);
                    levels.add(new PriceLevel(extraVolume, depthPrice));
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid tier: " + tier);
        }

        int executedQty = (int) Math.min(requestedQty, availableVolume);
        boolean isPartial = executedQty < requestedQty;

        double vwap = calculateSequentialVwap(levels, executedQty);

        SimulationLogger.log(String.format(
            "[TIER_%d_FILL] %sQty %d / %d available @ VWAP %.4f",
            tier, isPartial ? "Partial " : "", executedQty, availableVolume, vwap
        ));

        return new FillResult(vwap, executedQty, isPartial);
    }

    /**
     * Calculate VWAP by sequentially consuming price levels
     */
    private double calculateSequentialVwap(List<PriceLevel> levels, double totalQty) {
        double remainingQty = totalQty;
        double totalCost = 0.0;

        for (PriceLevel level : levels) {
            if (remainingQty <= 0) break;

            double qtyAtLevel = Math.min(remainingQty, level.volume);
            totalCost += qtyAtLevel * level.price;
            remainingQty -= qtyAtLevel;
        }

        return totalQty > 0 ? totalCost / totalQty : 0.0;
    }

    /**
     * Represents a price level with available volume
     */
    private static class PriceLevel {
        final double volume;
        final double price;

        PriceLevel(double volume, double price) {
            this.volume = volume;
            this.price = price;
        }
    }

    /**
     * Attach an event listener to observe execution lifecycle
     * @param listener Event listener implementation
     */
    public void addEventListener(ExecutionEventListener listener) {
        this.listeners.add(listener);
    }

    public double getMaxParticipationRate() {
        return maxParticipationRate;
    }

    // Event notification methods
    private void notifyExecutionStart(Order order, MarketState state) {
        for (ExecutionEventListener listener : listeners) {
            listener.onExecutionStart(order, state);
        }
    }

    private void notifyTick(MarketState state) {
        for (ExecutionEventListener listener : listeners) {
            listener.onTick(state);
        }
    }

    private void notifyDecision(OrderDecision decision, MarketState state) {
        for (ExecutionEventListener listener : listeners) {
            listener.onDecision(decision, state);
        }
    }

    private void notifyFill(Fill fill, MarketState state) {
        for (ExecutionEventListener listener : listeners) {
            listener.onFill(fill, state);
        }
    }

    private void notifyExecutionComplete(List<Fill> fills, MarketState state) {
        for (ExecutionEventListener listener : listeners) {
            listener.onExecutionComplete(fills, state);
        }
    }

    /**
     * Encapsulates the result of an order execution
     * Null indicates no fill occurred
     */
    private static class FillResult {
        final double price;
        final int quantity;
        final boolean isPartial;

        FillResult(double price, int quantity, boolean isPartial) {
            this.price = price;
            this.quantity = quantity;
            this.isPartial = isPartial;
        }
    }

    /**
     * Builder for MarketSimulator with fluent configuration
     */
    public static class Builder {
        private double maxParticipationRate = DEFAULT_MAX_PARTICIPATION;
        private double depthFactor = DEFAULT_DEPTH_FACTOR;
        private long latencyMs = DEFAULT_LATENCY_MS;
        private Long seed = null;

        public Builder maxParticipationRate(double maxParticipationRate) {
            this.maxParticipationRate = maxParticipationRate;
            return this;
        }

        public Builder depthFactor(double depthFactor) {
            this.depthFactor = depthFactor;
            return this;
        }

        public Builder latencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public MarketSimulator build() {
            if (seed != null) {
                return new MarketSimulator(maxParticipationRate, depthFactor, latencyMs, seed);
            } else {
                return new MarketSimulator(maxParticipationRate, depthFactor, latencyMs);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}