package metrics;

/**
 * Comprehensive execution performance metrics
 * Contains slippage measurements and execution quality indicators
 */
public class ExecutionMetrics {
    // Slippage metrics (in basis points)
    private final double arrivalPriceSlippage;
    private final double decisionPriceSlippage;
    private final double vwapSlippage;
    private final double midPriceSlippage;

    // Execution quality
    private final double implementationShortfall;
    private final double fillRatio;
    private final double fillEfficiency;
    private final double immediateExecutionRatio;
    private final double avgAmendmentsPerOrder;

    // Reference prices (for context)
    private final double arrivalPrice;
    private final double executionVWAP;
    private final double marketVWAP;
    private final double marketMidVWAP;

    public ExecutionMetrics(double arrivalPriceSlippage, double decisionPriceSlippage,
                           double vwapSlippage, double midPriceSlippage,
                           double implementationShortfall, double fillRatio,
                           double fillEfficiency, double immediateExecutionRatio,
                           double avgAmendmentsPerOrder,
                           double arrivalPrice, double executionVWAP,
                           double marketVWAP, double marketMidVWAP) {
        this.arrivalPriceSlippage = arrivalPriceSlippage;
        this.decisionPriceSlippage = decisionPriceSlippage;
        this.vwapSlippage = vwapSlippage;
        this.midPriceSlippage = midPriceSlippage;
        this.implementationShortfall = implementationShortfall;
        this.fillRatio = fillRatio;
        this.fillEfficiency = fillEfficiency;
        this.immediateExecutionRatio = immediateExecutionRatio;
        this.avgAmendmentsPerOrder = avgAmendmentsPerOrder;
        this.arrivalPrice = arrivalPrice;
        this.executionVWAP = executionVWAP;
        this.marketVWAP = marketVWAP;
        this.marketMidVWAP = marketMidVWAP;
    }

    public void printSummary() {
        System.out.println("\n=== Execution Metrics ===");
        System.out.println(String.format("Arrival Price: %.4f", arrivalPrice));
        System.out.println(String.format("Execution VWAP: %.4f", executionVWAP));
        System.out.println(String.format("Market VWAP: %.4f", marketVWAP));
        System.out.println(String.format("Market Mid VWAP: %.4f", marketMidVWAP));
        System.out.println("\n--- Slippage (bps) ---");
        System.out.println(String.format("Arrival Price Slippage: %.2f bps", arrivalPriceSlippage));
        System.out.println(String.format("Decision Price Slippage: %.2f bps", decisionPriceSlippage));
        System.out.println(String.format("VWAP Slippage: %.2f bps", vwapSlippage));
        System.out.println(String.format("Mid Price Slippage: %.2f bps", midPriceSlippage));
        System.out.println("\n--- Execution Quality ---");
        System.out.println(String.format("Implementation Shortfall: %.2f bps", implementationShortfall));
        System.out.println(String.format("Fill Ratio: %.2f%%", fillRatio * 100));
        System.out.println(String.format("Fill Efficiency: %.2f%%", fillEfficiency * 100));
        System.out.println(String.format("Immediate Execution: %.2f%%", immediateExecutionRatio * 100));
        System.out.println(String.format("Avg Amendments/Order: %.2f", avgAmendmentsPerOrder));
        System.out.println("========================\n");
    }

    // Getters
    public double getArrivalPriceSlippage() {
        return arrivalPriceSlippage;
    }

    public double getDecisionPriceSlippage() {
        return decisionPriceSlippage;
    }

    public double getVwapSlippage() {
        return vwapSlippage;
    }

    public double getMidPriceSlippage() {
        return midPriceSlippage;
    }

    public double getImplementationShortfall() {
        return implementationShortfall;
    }

    public double getFillRatio() {
        return fillRatio;
    }

    public double getArrivalPrice() {
        return arrivalPrice;
    }

    public double getExecutionVWAP() {
        return executionVWAP;
    }

    public double getMarketVWAP() {
        return marketVWAP;
    }

    public double getMarketMidVWAP() {
        return marketMidVWAP;
    }

    public double getFillEfficiency() {
        return fillEfficiency;
    }

    public double getImmediateExecutionRatio() {
        return immediateExecutionRatio;
    }

    public double getAvgAmendmentsPerOrder() {
        return avgAmendmentsPerOrder;
    }
}