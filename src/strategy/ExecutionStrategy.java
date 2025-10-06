package strategy;

import data.Fill;
import data.Order;
import data.TickData;
import execution.MarketState;
import execution.OrderDecision;

import java.util.List;

public interface ExecutionStrategy {

    /**
     * Initialize the strategy with parent order and market data
     * Called once before simulation starts
     * @param parentOrder The parent order to execute
     * @param marketData Full historical market data for analysis
     */
    void initialize(Order parentOrder, List<TickData> marketData);

    /**
     * Called on each market tick - strategy decides what to do
     * @param currentState Current market conditions
     * @return OrderDecision (market order, limit order, or no action)
     */
    OrderDecision onTick(MarketState currentState);

    /**
     * Called when an order is filled (full or partial)
     * Strategy should update internal state
     * @param fill The fill that occurred
     */
    void onFill(Fill fill);

    /**
     * Check if strategy has completed execution
     * @return true if all parent order quantity is executed
     */
    boolean isComplete();

    /**
     * Get strategy name for logging and reporting
     * @return Strategy identifier
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}