package execution;

/**
 * Encapsulates market state relevant for order execution
 * Immutable snapshot of market conditions at a specific tick
 * Provides convenient access to derived values like baseVolume
 */
public class ExecutionContext {
    private final double bid;
    private final double ask;
    private final double spread;
    private final double tradePrice;
    private final double tickVolume;
    private final double baseVolume;

    private ExecutionContext(double bid, double ask, double spread,
                            double tradePrice, double tickVolume, double baseVolume) {
        this.bid = bid;
        this.ask = ask;
        this.spread = spread;
        this.tradePrice = tradePrice;
        this.tickVolume = tickVolume;
        this.baseVolume = baseVolume;
    }

    /**
     * Create ExecutionContext from MarketState and participation rate
     * @param state Current market state
     * @param participationRate Maximum participation rate (e.g., 0.20 for 20%)
     * @return ExecutionContext with calculated base volume
     */
    public static ExecutionContext from(MarketState state, double participationRate) {
        double baseVolume = Math.floor(state.getVolume() * participationRate);
        return new ExecutionContext(
            state.getBid(),
            state.getAsk(),
            state.getSpread(),
            state.getCurrentTick().getTp(),
            state.getVolume(),
            baseVolume
        );
    }

    public double getBid() {
        return bid;
    }

    public double getAsk() {
        return ask;
    }

    public double getSpread() {
        return spread;
    }

    public double getTradePrice() {
        return tradePrice;
    }

    public double getTickVolume() {
        return tickVolume;
    }

    /**
     * Base available volume calculated from participation rate
     */
    public double getBaseVolume() {
        return baseVolume;
    }

    /**
     * Check if there is sufficient liquidity to execute
     */
    public boolean hasLiquidity() {
        return baseVolume > 0;
    }

    @Override
    public String toString() {
        return String.format("ExecutionContext{bid=%.4f, ask=%.4f, spread=%.4f, volume=%.0f, baseVolume=%.0f}",
            bid, ask, spread, tickVolume, baseVolume);
    }
}