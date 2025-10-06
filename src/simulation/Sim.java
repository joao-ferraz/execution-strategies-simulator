package simulation;

import data.OrderTemplate;
import strategy.ExecutionStrategy;
import util.SimulationLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simplified simulation entry point
 * Single static method to run simulations across all combinations of strategies, times, and dates
 */
public class Sim {

    /**
     * Run simulations for all combinations of strategies, time windows, and dates
     *
     * @param order Order template with relative duration
     * @param strategies List of execution strategies to test
     * @param times List of time window specifications ("market-open", "10:00", "10:00-11:00", etc.)
     * @param dates List of dates ("2025-10-24", "latest", "random")
     * @return Aggregated results with filtering and comparison capabilities
     */
    public static SimulationResults run(
            OrderTemplate order,
            List<ExecutionStrategy> strategies,
            List<String> times,
            List<String> dates) {

        return run(order, strategies, times, dates, SimulationContext.getDefault());
    }

    /**
     * Run simulations with custom context
     */
    public static SimulationResults run(
            OrderTemplate order,
            List<ExecutionStrategy> strategies,
            List<String> times,
            List<String> dates,
            SimulationContext context) {

        long startTime = System.currentTimeMillis();

        // 1. Generate all simulation tasks (Cartesian product)
        List<SimulationTask> tasks = generateTasks(order, strategies, times, dates, context);

        int totalSimulations = tasks.size();
        SimulationLogger.setVerbose(context.isVerbose());

        if (context.isVerbose()) {
            SimulationLogger.logSection("SIMULATION START");
            SimulationLogger.log(String.format("Total simulations: %d (%d strategies × %d times × %d dates)",
                totalSimulations, strategies.size(), times.size(), dates.size()));
        }

        // 2. Execute all tasks (sequential or parallel based on context)
        List<SimulationResult> results = executeTasks(tasks, context);

        long executionTime = System.currentTimeMillis() - startTime;

        if (context.isVerbose()) {
            SimulationLogger.logSection("SIMULATION COMPLETE");
            SimulationLogger.log(String.format("Completed %d simulations in %.2fs",
                results.size(), executionTime / 1000.0));
        }

        // 3. Return aggregated results
        return new SimulationResults(results);
    }

    /**
     * Convenience method: Run with single strategy
     */
    public static SimulationResults run(OrderTemplate order, ExecutionStrategy strategy) {
        return run(order, List.of(strategy), List.of("full-day"), List.of("latest"));
    }

    /**
     * Convenience method: Run with multiple strategies, default time and date
     */
    public static SimulationResults run(OrderTemplate order, List<ExecutionStrategy> strategies) {
        return run(order, strategies, List.of("full-day"), List.of("latest"));
    }

    /**
     * Convenience method: Run with single strategy, single time, single date
     */
    public static SimulationResults run(OrderTemplate order, ExecutionStrategy strategy, String time, String date) {
        return run(order, List.of(strategy), List.of(time), List.of(date));
    }

    /**
     * Generate all simulation tasks (Cartesian product)
     */
    private static List<SimulationTask> generateTasks(
            OrderTemplate order,
            List<ExecutionStrategy> strategies,
            List<String> times,
            List<String> dates,
            SimulationContext context) {

        List<SimulationTask> tasks = new ArrayList<>();

        for (ExecutionStrategy strategy : strategies) {
            for (String time : times) {
                for (String date : dates) {
                    tasks.add(new SimulationTask(order, strategy, time, date, context));
                }
            }
        }

        return tasks;
    }

    /**
     * Execute all tasks (sequential or parallel)
     */
    private static List<SimulationResult> executeTasks(List<SimulationTask> tasks, SimulationContext context) {
        if (context.isParallel()) {
            // Parallel execution for performance
            return tasks.parallelStream()
                    .map(SimulationTask::execute)
                    .collect(Collectors.toList());
        } else {
            // Sequential execution (default)
            return tasks.stream()
                    .map(SimulationTask::execute)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Builder for more readable API
     */
    public static class Builder {
        private OrderTemplate order;
        private List<ExecutionStrategy> strategies = new ArrayList<>();
        private List<String> times = new ArrayList<>();
        private List<String> dates = new ArrayList<>();
        private SimulationContext context = SimulationContext.getDefault();

        public Builder order(OrderTemplate order) {
            this.order = order;
            return this;
        }

        public Builder strategy(ExecutionStrategy strategy) {
            this.strategies.add(strategy);
            return this;
        }

        public Builder strategies(ExecutionStrategy... strategies) {
            this.strategies.addAll(List.of(strategies));
            return this;
        }

        public Builder strategies(List<ExecutionStrategy> strategies) {
            this.strategies.addAll(strategies);
            return this;
        }

        public Builder time(String time) {
            this.times.add(time);
            return this;
        }

        public Builder times(String... times) {
            this.times.addAll(List.of(times));
            return this;
        }

        public Builder times(List<String> times) {
            this.times.addAll(times);
            return this;
        }

        public Builder date(String date) {
            this.dates.add(date);
            return this;
        }

        public Builder dates(String... dates) {
            this.dates.addAll(List.of(dates));
            return this;
        }

        public Builder dates(List<String> dates) {
            this.dates.addAll(dates);
            return this;
        }

        public Builder context(SimulationContext context) {
            this.context = context;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.context = SimulationContext.builder()
                    .dataManager(context.getDataManager())
                    .verbose(verbose)
                    .parallel(context.isParallel())
                    .build();
            return this;
        }

        public Builder parallel(boolean parallel) {
            this.context = SimulationContext.builder()
                    .dataManager(context.getDataManager())
                    .verbose(context.isVerbose())
                    .parallel(parallel)
                    .build();
            return this;
        }

        public SimulationResults run() {
            if (order == null) {
                throw new IllegalStateException("Order template is required");
            }
            if (strategies.isEmpty()) {
                throw new IllegalStateException("At least one strategy is required");
            }
            if (times.isEmpty()) {
                times.add("full-day");  // Default time window
            }
            if (dates.isEmpty()) {
                dates.add("latest");  // Default date
            }

            return Sim.run(order, strategies, times, dates, context);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
