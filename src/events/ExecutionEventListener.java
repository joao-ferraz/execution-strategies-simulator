package events;

import data.Fill;
import data.Order;
import execution.MarketState;
import execution.OrderDecision;

import java.util.List;

/**
 * Listener interface for execution lifecycle events
 * Implement to collect metrics, logging, monitoring, etc.
 * All methods are default - implement only what you need
 */
public interface ExecutionEventListener {

    /**
     * Called when strategy starts execution
     * Use to capture arrival price, initialize benchmarks
     * @param parentOrder The parent order being executed
     * @param initialState Market state at execution start
     */
    default void onExecutionStart(Order parentOrder, MarketState initialState) {}

    /**
     * Called on every tick (before strategy decides)
     * Use to track market evolution, update running benchmarks
     * @param state Current market state
     */
    default void onTick(MarketState state) {}

    /**
     * Called when strategy makes a decision (not NO_ACTION)
     * Use to track decision price, timing
     * @param decision The order decision made by the strategy
     * @param state Market state when decision was made
     */
    default void onDecision(OrderDecision decision, MarketState state) {}

    /**
     * Called when a fill occurs
     * Use to track execution price vs market conditions
     * @param fill The fill that occurred
     * @param state Market state when fill occurred
     */
    default void onFill(Fill fill, MarketState state) {}

    /**
     * Called when execution completes (strategy.isComplete() or end of data)
     * Use to finalize metrics calculation
     * @param fills All fills that occurred during execution
     * @param finalState Market state at execution end
     */
    default void onExecutionComplete(List<Fill> fills, MarketState finalState) {}
}