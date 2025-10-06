package simulation;

import data.Order;
import data.OrderTemplate;
import data.TickData;
import ingestion.MarketDataManager;
import strategy.ExecutionStrategy;
import util.SimulationLogger;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates batch simulations across multiple dates and strategies
 * Enables systematic testing of execution strategies under different market conditions
 */
public class SimulationBatch {
    private final OrderTemplate orderTemplate;
    private final DateSelector dateSelector;
    private final TimeWindowSelector timeSelector;
    private final List<ExecutionStrategy> strategies;
    private final MarketDataManager dataManager;
    private final boolean verbose;

    private SimulationBatch(Builder builder) {
        this.orderTemplate = builder.orderTemplate;
        this.dateSelector = builder.dateSelector;
        this.timeSelector = builder.timeSelector;
        this.strategies = builder.strategies;
        this.dataManager = builder.dataManager;
        this.verbose = builder.verbose;
    }

    /**
     * Run batch simulation across all selected dates and strategies
     */
    public List<BatchResult> run() {
        SimulationLogger.setVerbose(verbose);
        SimulationLogger.logSection("BATCH SIMULATION START");

        // 1. Select dates
        List<LocalDate> availableDates = dataManager.listDatesAsLocalDate(orderTemplate.getSymbol());
        List<LocalDate> selectedDates = dateSelector.selectDates(availableDates, dataManager);

        SimulationLogger.log("Order template: " + orderTemplate);
        SimulationLogger.log("Date selector: " + dateSelector.getDescription());
        SimulationLogger.log("Time selector: " + timeSelector.getDescription());
        SimulationLogger.log("Selected dates: " + selectedDates.size());
        SimulationLogger.log("Strategies: " + strategies.size());
        SimulationLogger.log("");

        List<BatchResult> results = new ArrayList<>();
        AtomicInteger simulationCount = new AtomicInteger(0);
        int totalSimulations = selectedDates.size() * strategies.size();

        // 2. For each date and strategy, run simulation
        for (LocalDate date : selectedDates) {
            for (ExecutionStrategy strategy : strategies) {
                simulationCount.incrementAndGet();
                SimulationLogger.logSection(String.format("Simulation %d/%d: %s on %s",
                    simulationCount.get(), totalSimulations, strategy.getName(), date));

                try {
                    BatchResult result = runSingle(date, strategy);
                    results.add(result);
                } catch (Exception e) {
                    SimulationLogger.log("ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        SimulationLogger.logSection("BATCH SIMULATION COMPLETE");
        SimulationLogger.log("Total simulations: " + results.size());

        // Print aggregated summary
        printBatchSummary(results);

        return results;
    }

    /**
     * Run single simulation for a specific date and strategy
     */
    private BatchResult runSingle(LocalDate date, ExecutionStrategy strategy) {
        // 1. Load market data for the date
        List<TickData> dayData = dataManager.getTickData(orderTemplate.getSymbol(), date);

        // 2. For MarketCloseSelector, set order duration before calling select()
        if (timeSelector instanceof MarketCloseSelector) {
            ((MarketCloseSelector) timeSelector).setOrderDuration(orderTemplate.getDuration());
        }

        // 3. Select time window within the day (gets start time)
        TimeWindow preliminaryWindow = timeSelector.select(dayData);

        // 4. Build final window using order duration
        Instant startTime = preliminaryWindow.getStartTime();
        Instant endTime = startTime.plus(orderTemplate.getDuration());
        TimeWindow finalWindow = new TimeWindow(
            startTime,
            endTime
        );

        // 5. Materialize order with concrete times from final window
        String orderId = String.format("BATCH_%s_%s", date, strategy.getName());
        Order order = orderTemplate.materialize(orderId, startTime);

        // 6. Run simulation
        SimulationResult simResult = Simulator.builder()
            .withDataReader(dataManager.getReader(orderTemplate.getSymbol(), date))
            .withParentOrder(order)
            .withTimeWindow(new FixedTimeWindowSelector(finalWindow))  // Use final window
            .withStrategy(strategy)
            .verbose(false)  // Disable verbose for batch runs
            .run()
            .get(0);

        return new BatchResult(date, strategy.getName(), simResult);
    }

    /**
     * Print aggregated statistics across batch
     */
    private void printBatchSummary(List<BatchResult> results) {
        if (results.isEmpty()) {
            return;
        }

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║           BATCH SIMULATION SUMMARY                   ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        // Group by strategy
        results.stream()
            .collect(java.util.stream.Collectors.groupingBy(BatchResult::getStrategyName))
            .forEach((strategyName, strategyResults) -> {
                System.out.println("\n=== " + strategyName + " ===");
                System.out.println("Simulations: " + strategyResults.size());

                // Calculate average metrics
                double avgArrivalSlippage = strategyResults.stream()
                    .map(BatchResult::getSimulationResult)
                    .filter(r -> r.getExecutionMetrics() != null)
                    .mapToDouble(r -> r.getExecutionMetrics().getArrivalPriceSlippage())
                    .average()
                    .orElse(0.0);

                double avgVwapSlippage = strategyResults.stream()
                    .map(BatchResult::getSimulationResult)
                    .filter(r -> r.getExecutionMetrics() != null)
                    .mapToDouble(r -> r.getExecutionMetrics().getVwapSlippage())
                    .average()
                    .orElse(0.0);

                double avgFillRatio = strategyResults.stream()
                    .map(BatchResult::getSimulationResult)
                    .filter(r -> r.getExecutionMetrics() != null)
                    .mapToDouble(r -> r.getExecutionMetrics().getFillRatio())
                    .average()
                    .orElse(0.0);

                System.out.printf("Avg Arrival Slippage: %.2f bps\n", avgArrivalSlippage);
                System.out.printf("Avg VWAP Slippage: %.2f bps\n", avgVwapSlippage);
                System.out.printf("Avg Fill Ratio: %.2f%%\n", avgFillRatio * 100);
            });

        System.out.println("\n════════════════════════════════════════════════════════\n");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OrderTemplate orderTemplate;
        private DateSelector dateSelector;
        private TimeWindowSelector timeSelector = new FullDaySelector();
        private final List<ExecutionStrategy> strategies = new ArrayList<>();
        private MarketDataManager dataManager;
        private boolean verbose = false;

        public Builder withOrderTemplate(OrderTemplate orderTemplate) {
            this.orderTemplate = orderTemplate;
            return this;
        }

        public Builder withDateSelector(DateSelector dateSelector) {
            this.dateSelector = dateSelector;
            return this;
        }

        public Builder withTimeSelector(TimeWindowSelector timeSelector) {
            this.timeSelector = timeSelector;
            return this;
        }

        public Builder withStrategy(ExecutionStrategy strategy) {
            this.strategies.add(strategy);
            return this;
        }

        public Builder withStrategies(ExecutionStrategy... strategies) {
            this.strategies.addAll(List.of(strategies));
            return this;
        }

        public Builder withStrategies(List<ExecutionStrategy> strategies) {
            this.strategies.addAll(strategies);
            return this;
        }

        public Builder withDataManager(MarketDataManager dataManager) {
            this.dataManager = dataManager;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public SimulationBatch build() {
            if (orderTemplate == null) {
                throw new IllegalStateException("OrderTemplate is required");
            }
            if (dateSelector == null) {
                throw new IllegalStateException("DateSelector is required");
            }
            if (strategies.isEmpty()) {
                throw new IllegalStateException("At least one strategy is required");
            }
            if (dataManager == null) {
                throw new IllegalStateException("MarketDataManager is required");
            }
            return new SimulationBatch(this);
        }

        public List<BatchResult> run() {
            return build().run();
        }
    }
}