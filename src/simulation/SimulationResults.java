package simulation;

import metrics.ExecutionMetrics;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Collection wrapper for simulation results with filtering, aggregation, and export capabilities
 */
public class SimulationResults {
    private final List<SimulationResult> results;

    public SimulationResults(List<SimulationResult> results) {
        this.results = results;
    }

    // ==================== Filtering Methods ====================

    /**
     * Filter results by strategy name
     */
    public SimulationResults byStrategy(String strategyName) {
        return filter(r -> r.getStrategyName().equals(strategyName));
    }

    /**
     * Filter results by date
     */
    public SimulationResults byDate(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return filter(r -> r.getDate() != null && r.getDate().equals(localDate));
    }

    /**
     * Filter results by date
     */
    public SimulationResults byDate(LocalDate date) {
        return filter(r -> r.getDate() != null && r.getDate().equals(date));
    }

    /**
     * Filter results by time window
     */
    public SimulationResults byTime(String timeWindow) {
        return filter(r -> r.getTimeWindow() != null && r.getTimeWindow().equals(timeWindow));
    }

    /**
     * Generic filter
     */
    public SimulationResults filter(Predicate<SimulationResult> predicate) {
        List<SimulationResult> filtered = results.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        return new SimulationResults(filtered);
    }

    // ==================== Aggregation Methods ====================

    /**
     * Get average value for a specific metric
     */
    public double avgMetric(String metricName) {
        return results.stream()
                .map(SimulationResult::getExecutionMetrics)
                .filter(m -> m != null)
                .mapToDouble(m -> getMetricValue(m, metricName))
                .average()
                .orElse(0.0);
    }

    /**
     * Get best result based on metric (lower is better for slippage)
     */
    public SimulationResult best(String metricName) {
        return results.stream()
                .filter(r -> r.getExecutionMetrics() != null)
                .min(Comparator.comparingDouble(r -> getMetricValue(r.getExecutionMetrics(), metricName)))
                .orElse(null);
    }

    /**
     * Get worst result based on metric (higher is worse for slippage)
     */
    public SimulationResult worst(String metricName) {
        return results.stream()
                .filter(r -> r.getExecutionMetrics() != null)
                .max(Comparator.comparingDouble(r -> getMetricValue(r.getExecutionMetrics(), metricName)))
                .orElse(null);
    }

    /**
     * Rank strategies by metric (lower is better)
     */
    public Map<String, Double> strategyRankings(String metricName) {
        return results.stream()
                .filter(r -> r.getExecutionMetrics() != null)
                .collect(Collectors.groupingBy(
                        SimulationResult::getStrategyName,
                        Collectors.averagingDouble(r -> getMetricValue(r.getExecutionMetrics(), metricName))
                ));
    }

    // ==================== Output Methods ====================

    /**
     * Print comprehensive summary
     */
    public void printSummary() {
        if (results.isEmpty()) {
            System.out.println("No simulation results available.");
            return;
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("              SIMULATION RESULTS SUMMARY");
        System.out.println("=".repeat(60));

        System.out.printf("\nTotal Simulations: %d\n", results.size());

        // Count unique strategies, dates, time windows
        long uniqueStrategies = results.stream().map(SimulationResult::getStrategyName).distinct().count();
        long uniqueDates = results.stream().map(SimulationResult::getDate).filter(d -> d != null).distinct().count();
        long uniqueTimes = results.stream().map(SimulationResult::getTimeWindow).filter(t -> t != null).distinct().count();

        System.out.printf("Strategies: %d | Dates: %d | Time Windows: %d\n",
                uniqueStrategies, uniqueDates, uniqueTimes);

        // Print strategy rankings
        System.out.println("\n" + "-".repeat(60));
        System.out.println("Strategy Performance (Avg Arrival Slippage)");
        System.out.println("-".repeat(60));

        Map<String, Double> rankings = strategyRankings("arrivalSlippage");
        rankings.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    String indicator = entry.equals(rankings.entrySet().stream()
                            .min(Map.Entry.comparingByValue()).get()) ? " [BEST]" : "";
                    System.out.printf("%-30s  %8.2f bps%s\n",
                            entry.getKey(), entry.getValue(), indicator);
                });

        // Print time window performance if multiple time windows
        if (uniqueTimes > 1) {
            System.out.println("\n" + "-".repeat(60));
            System.out.println("Time Window Performance (Avg Arrival Slippage)");
            System.out.println("-".repeat(60));

            Map<String, Double> timePerf = results.stream()
                    .filter(r -> r.getExecutionMetrics() != null && r.getTimeWindow() != null)
                    .collect(Collectors.groupingBy(
                            SimulationResult::getTimeWindow,
                            Collectors.averagingDouble(r ->
                                r.getExecutionMetrics().getArrivalPriceSlippage())
                    ));

            timePerf.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(entry -> {
                        String indicator = entry.equals(timePerf.entrySet().stream()
                                .min(Map.Entry.comparingByValue()).get()) ? " [BEST]" : "";
                        System.out.printf("%-30s  %8.2f bps%s\n",
                                entry.getKey(), entry.getValue(), indicator);
                    });
        }

        System.out.println("\n" + "=".repeat(60) + "\n");
    }

    /**
     * Print comparison table for a specific metric
     */
    public void printComparison(String metricName) {
        if (results.isEmpty()) {
            System.out.println("No results to compare.");
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("Metric Comparison: %s\n", metricName);
        System.out.println("=".repeat(80));
        System.out.printf("%-30s %-15s %-15s %10s\n", "Strategy", "Date", "Time", metricName);
        System.out.println("-".repeat(80));

        results.stream()
                .filter(r -> r.getExecutionMetrics() != null)
                .forEach(r -> {
                    String date = r.getDate() != null ? r.getDate().toString() : "N/A";
                    String time = r.getTimeWindow() != null ? r.getTimeWindow() : "N/A";
                    double value = getMetricValue(r.getExecutionMetrics(), metricName);
                    System.out.printf("%-30s %-15s %-15s %10.2f\n",
                            r.getStrategyName(), date, time, value);
                });

        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Print comprehensive comparison table showing all metrics for all simulations
     * Highlights best value for each metric with [BEST] marker
     */
    public void printFullComparison() {
        if (results.isEmpty()) {
            System.out.println("No results to compare.");
            return;
        }

        List<SimulationResult> validResults = results.stream()
                .filter(r -> r.getExecutionMetrics() != null)
                .collect(Collectors.toList());

        if (validResults.isEmpty()) {
            System.out.println("No valid metrics to compare.");
            return;
        }

        // Find best values for each metric (lower is better for slippage)
        double bestArrivalSlippage = validResults.stream()
                .mapToDouble(r -> r.getExecutionMetrics().getArrivalPriceSlippage())
                .min().orElse(Double.MAX_VALUE);

        double bestVwapSlippage = validResults.stream()
                .mapToDouble(r -> r.getExecutionMetrics().getVwapSlippage())
                .min().orElse(Double.MAX_VALUE);

        double bestMidSlippage = validResults.stream()
                .mapToDouble(r -> r.getExecutionMetrics().getMidPriceSlippage())
                .min().orElse(Double.MAX_VALUE);

        double bestDecisionSlippage = validResults.stream()
                .mapToDouble(r -> r.getExecutionMetrics().getDecisionPriceSlippage())
                .min().orElse(Double.MAX_VALUE);

        double bestShortfall = validResults.stream()
                .mapToDouble(r -> r.getExecutionMetrics().getImplementationShortfall())
                .min().orElse(Double.MAX_VALUE);

        double bestFillRatio = validResults.stream()
                .mapToDouble(r -> r.getExecutionMetrics().getFillRatio())
                .max().orElse(Double.MIN_VALUE);  // Higher is better for fill ratio

        double bestFillEfficiency = validResults.stream()
                .mapToDouble(r -> r.getExecutionMetrics().getFillEfficiency())
                .max().orElse(Double.MIN_VALUE);  // Higher is better

        double bestImmediateRatio = validResults.stream()
                .mapToDouble(r -> r.getExecutionMetrics().getImmediateExecutionRatio())
                .max().orElse(Double.MIN_VALUE);  // Higher is better

        double bestAvgAmendments = validResults.stream()
                .mapToDouble(r -> r.getExecutionMetrics().getAvgAmendmentsPerOrder())
                .min().orElse(Double.MAX_VALUE);  // Lower is better

        // Print header
        System.out.println("\n" + "=".repeat(180));
        System.out.println("FULL METRICS COMPARISON - ALL SIMULATIONS");
        System.out.println("=".repeat(180));
        System.out.printf("%-30s %-12s %-15s | %9s | %9s | %9s | %9s | %9s | %8s | %8s | %8s | %7s\n",
                "Strategy", "Date", "Time",
                "Arrival", "VWAP", "Mid", "Decision", "Shortfall", "FillRatio", "FillEff", "Immediate", "AvgAmend");
        System.out.printf("%-30s %-12s %-15s | %9s | %9s | %9s | %9s | %9s | %8s | %8s | %8s | %7s\n",
                "", "", "",
                "(bps)", "(bps)", "(bps)", "(bps)", "(bps)", "(%)", "(%)", "(%)", "");
        System.out.println("-".repeat(180));

        // Print each result
        for (SimulationResult result : validResults) {
            ExecutionMetrics metrics = result.getExecutionMetrics();
            String date = result.getDate() != null ? result.getDate().toString() : "N/A";
            String time = result.getTimeWindow() != null ? result.getTimeWindow() : "N/A";

            double arrivalSlip = metrics.getArrivalPriceSlippage();
            double vwapSlip = metrics.getVwapSlippage();
            double midSlip = metrics.getMidPriceSlippage();
            double decisionSlip = metrics.getDecisionPriceSlippage();
            double shortfall = metrics.getImplementationShortfall();
            double fillRatio = metrics.getFillRatio();
            double fillEff = metrics.getFillEfficiency();
            double immediate = metrics.getImmediateExecutionRatio();
            double avgAmendments = metrics.getAvgAmendmentsPerOrder();

            // Build the line with markers for best values
            String arrivalStr = formatMetric(arrivalSlip, arrivalSlip == bestArrivalSlippage);
            String vwapStr = formatMetric(vwapSlip, vwapSlip == bestVwapSlippage);
            String midStr = formatMetric(midSlip, midSlip == bestMidSlippage);
            String decisionStr = formatMetric(decisionSlip, decisionSlip == bestDecisionSlippage);
            String shortfallStr = formatMetric(shortfall, shortfall == bestShortfall);
            String fillRatioStr = formatMetric(fillRatio * 100, fillRatio == bestFillRatio);
            String fillEffStr = formatMetric(fillEff * 100, fillEff == bestFillEfficiency);
            String immediateStr = formatMetric(immediate * 100, immediate == bestImmediateRatio);
            String avgAmendmentsStr = formatMetric(avgAmendments, avgAmendments == bestAvgAmendments);

            System.out.printf("%-30s %-12s %-15s | %9s | %9s | %9s | %9s | %9s | %8s | %8s | %8s | %7s\n",
                    result.getStrategyName(), date, time,
                    arrivalStr, vwapStr, midStr, decisionStr, shortfallStr,
                    fillRatioStr, fillEffStr, immediateStr, avgAmendmentsStr);
        }

        System.out.println("=".repeat(180));
        System.out.println("Note: * indicates best value for that metric (lower is better for slippage/amendments, higher for ratios)");
        System.out.println("=".repeat(180) + "\n");
    }

    /**
     * Format metric value with [BEST] marker if it's the best
     */
    private String formatMetric(double value, boolean isBest) {
        String formatted = String.format("%.2f", value);
        if (isBest) {
            return formatted + " *";  // Use asterisk instead of [BEST] to save space
        }
        return formatted;
    }

    /**
     * Export results to CSV format
     */
    public String toCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Strategy,Date,TimeWindow,ArrivalSlippage,VwapSlippage,MidPriceSlippage,FillRatio,FillEfficiency,ImmediateRatio,AvgAttempts,Fills,ExecutionTimeMs\n");

        for (SimulationResult result : results) {
            ExecutionMetrics metrics = result.getExecutionMetrics();
            csv.append(String.format("%s,%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%d\n",
                    result.getStrategyName(),
                    result.getDate() != null ? result.getDate().toString() : "N/A",
                    result.getTimeWindow() != null ? result.getTimeWindow() : "N/A",
                    metrics != null ? metrics.getArrivalPriceSlippage() : 0.0,
                    metrics != null ? metrics.getVwapSlippage() : 0.0,
                    metrics != null ? metrics.getMidPriceSlippage() : 0.0,
                    metrics != null ? metrics.getFillRatio() : 0.0,
                    metrics != null ? metrics.getFillEfficiency() : 0.0,
                    metrics != null ? metrics.getImmediateExecutionRatio() : 0.0,
                    metrics != null ? metrics.getAvgAmendmentsPerOrder() : 0.0,
                    result.getFills().size(),
                    result.getExecutionTimeMs()
            ));
        }

        return csv.toString();
    }

    /**
     * Export results to JSON format
     */
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"results\": [\n");

        for (int i = 0; i < results.size(); i++) {
            SimulationResult result = results.get(i);
            ExecutionMetrics metrics = result.getExecutionMetrics();

            json.append("    {\n");
            json.append(String.format("      \"strategy\": \"%s\",\n", result.getStrategyName()));
            json.append(String.format("      \"date\": \"%s\",\n",
                    result.getDate() != null ? result.getDate().toString() : "N/A"));
            json.append(String.format("      \"timeWindow\": \"%s\",\n",
                    result.getTimeWindow() != null ? result.getTimeWindow() : "N/A"));
            json.append(String.format("      \"fills\": %d,\n", result.getFills().size()));
            json.append(String.format("      \"executionTimeMs\": %d", result.getExecutionTimeMs()));

            if (metrics != null) {
                json.append(",\n      \"metrics\": {\n");
                json.append(String.format("        \"arrivalSlippage\": %.4f,\n",
                        metrics.getArrivalPriceSlippage()));
                json.append(String.format("        \"vwapSlippage\": %.4f,\n",
                        metrics.getVwapSlippage()));
                json.append(String.format("        \"midPriceSlippage\": %.4f,\n",
                        metrics.getMidPriceSlippage()));
                json.append(String.format("        \"fillRatio\": %.4f,\n",
                        metrics.getFillRatio()));
                json.append(String.format("        \"fillEfficiency\": %.4f,\n",
                        metrics.getFillEfficiency()));
                json.append(String.format("        \"immediateExecutionRatio\": %.4f,\n",
                        metrics.getImmediateExecutionRatio()));
                json.append(String.format("        \"avgAmendmentsPerOrder\": %.4f\n",
                        metrics.getAvgAmendmentsPerOrder()));
                json.append("      }\n");
            } else {
                json.append("\n");
            }

            json.append("    }");
            if (i < results.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n}\n");
        return json.toString();
    }

    // ==================== Access Methods ====================

    /**
     * Get all results
     */
    public List<SimulationResult> getAll() {
        return results;
    }

    /**
     * Get number of results
     */
    public int size() {
        return results.size();
    }

    /**
     * Check if empty
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * Get first result (useful for single simulations)
     */
    public SimulationResult first() {
        return results.isEmpty() ? null : results.get(0);
    }

    // ==================== Helper Methods ====================

    /**
     * Extract metric value by name
     */
    private double getMetricValue(ExecutionMetrics metrics, String metricName) {
        switch (metricName.toLowerCase()) {
            case "arrivalslippage":
            case "arrival":
                return metrics.getArrivalPriceSlippage();
            case "vwapslippage":
            case "vwap":
                return metrics.getVwapSlippage();
            case "midpriceslippage":
            case "mid":
                return metrics.getMidPriceSlippage();
            case "decisionslippage":
            case "decision":
                return metrics.getDecisionPriceSlippage();
            case "implementationshortfall":
            case "shortfall":
                return metrics.getImplementationShortfall();
            case "fillratio":
            case "fill":
                return metrics.getFillRatio();
            default:
                return metrics.getArrivalPriceSlippage();  // Default
        }
    }
}
