package simulation;

import data.Fill;
import data.Order;
import data.OrderTemplate;
import data.TickData;
import execution.MarketSimulator;
import metrics.ExecutionMetrics;
import metrics.MetricsCollector;
import strategy.ExecutionStrategy;
import util.SimulationLogger;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Represents a single simulation to execute
 * Each task is independent and can be run in parallel
 */
class SimulationTask {
    private final OrderTemplate orderTemplate;
    private final ExecutionStrategy strategy;
    private final String timeWindow;
    private final String date;
    private final SimulationContext context;

    public SimulationTask(OrderTemplate orderTemplate, ExecutionStrategy strategy,
                          String timeWindow, String date, SimulationContext context) {
        this.orderTemplate = orderTemplate;
        this.strategy = strategy;
        this.timeWindow = timeWindow;
        this.date = date;
        this.context = context;
    }

    /**
     * Execute this simulation task
     */
    public SimulationResult execute() {
        long startTime = System.currentTimeMillis();

        try {
            // 1. Resolve date ("latest" -> actual date)
            LocalDate actualDate = resolveDate(date);

            // 2. Parse time window ("market-open" -> TimeWindowSelector)
            TimeWindowSelector timeSelector = parseTimeWindow(timeWindow);

            // 3. Load market data
            List<TickData> marketData = context.getDataManager()
                .getTickData(orderTemplate.getSymbol(), actualDate);

            if (marketData.isEmpty()) {
                throw new RuntimeException("No market data for " + orderTemplate.getSymbol() + " on " + actualDate);
            }

            // 4. Select time window
            TimeWindow window = timeSelector.select(marketData);

            // 5. Materialize order with absolute times
            String orderId = "SIM_" + UUID.randomUUID().toString().substring(0, 8);
            Order order = orderTemplate.materialize(orderId, window.getStartTime());

            // 6. Create market simulator with metrics collector
            MarketSimulator simulator = MarketSimulator.builder().build();
            MetricsCollector metricsCollector = new MetricsCollector();
            simulator.addEventListener(metricsCollector);

            // 7. Run simulation
            List<Fill> fills = simulator.simulate(strategy, order, marketData);

            // 8. Calculate execution metrics
            ExecutionMetrics executionMetrics = metricsCollector.calculateMetrics();

            long executionTime = System.currentTimeMillis() - startTime;

            // 9. Return result with metadata
            return new SimulationResult(
                strategy, fills, executionMetrics, executionTime,
                actualDate, timeWindow
            );

        } catch (Exception e) {
            SimulationLogger.log("ERROR executing simulation: " + e.getMessage());
            throw new RuntimeException("Simulation failed", e);
        }
    }

    /**
     * Resolve date string to actual LocalDate
     * Supports: "2025-10-24", "latest", "random"
     */
    private LocalDate resolveDate(String dateSpec) {
        String ticker = orderTemplate.getSymbol();
        List<LocalDate> availableDates = context.getDataManager().listDatesAsLocalDate(ticker);

        if (availableDates.isEmpty()) {
            throw new RuntimeException("No data available for " + ticker);
        }

        switch (dateSpec.toLowerCase()) {
            case "latest":
                return availableDates.get(availableDates.size() - 1);  // Most recent

            case "random":
                return availableDates.get(new Random().nextInt(availableDates.size()));

            default:
                return LocalDate.parse(dateSpec);  // "2025-10-24"
        }
    }

    /**
     * Parse time window string to TimeWindowSelector
     * Supports: "full-day", "market-open", "market-close", "10:00", "10:00-11:00"
     */
    private TimeWindowSelector parseTimeWindow(String spec) {
        switch (spec.toLowerCase()) {
            case "full-day":
            case "all":
                return new FullDaySelector();

            case "market-open":
                return new MarketOpenSelector();

            case "market-close":
                MarketCloseSelector closeSelector = new MarketCloseSelector();
                closeSelector.setOrderDuration(orderTemplate.getDuration());
                return closeSelector;

            default:
                // Parse "10:00" or "10:00-11:00"
                if (spec.contains("-")) {
                    String[] parts = spec.split("-");
                    LocalTime start = LocalTime.parse(parts[0].trim());
                    LocalTime end = LocalTime.parse(parts[1].trim());
                    return new CustomTimeRangeSelector(start, end);
                } else {
                    return new CustomTimeSelector(LocalTime.parse(spec.trim()));
                }
        }
    }

    /**
     * Custom time range selector for "HH:mm-HH:mm" format
     */
    private static class CustomTimeRangeSelector implements TimeWindowSelector {
        private final LocalTime startTime;
        private final LocalTime endTime;

        public CustomTimeRangeSelector(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public TimeWindow select(List<TickData> marketData) {
            if (marketData == null || marketData.isEmpty()) {
                throw new IllegalArgumentException("Market data cannot be empty");
            }

            // Find first tick at or after start time
            TickData startTick = marketData.stream()
                .filter(tick -> !tick.getTimestamp().atZone(java.time.ZoneId.systemDefault())
                    .toLocalTime().isBefore(startTime))
                .findFirst()
                .orElse(marketData.get(0));

            // Find last tick at or before end time
            TickData endTick = marketData.get(marketData.size() - 1);
            for (int i = marketData.size() - 1; i >= 0; i--) {
                TickData tick = marketData.get(i);
                LocalTime tickTime = tick.getTimestamp()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalTime();
                if (!tickTime.isAfter(endTime)) {
                    endTick = tick;
                    break;
                }
            }

            return new TimeWindow(startTick.getTimestamp(), endTick.getTimestamp());
        }

        @Override
        public String getDescription() {
            return "Time range: " + startTime + " to " + endTime;
        }
    }
}
