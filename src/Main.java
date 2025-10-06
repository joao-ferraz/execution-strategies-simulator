import data.OrderTemplate;
import ingestion.MarketDataManager;
import simulation.*;
import strategy.ExecutionStrategy;
import strategy.StrategyRegistry;
import strategy.TwapStrategy;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        StrategyRegistry.initialize();

        MarketDataManager dataManager = new MarketDataManager("../market_data");
        dataManager.printSummary();

        SimulationContext.setDefault(
            SimulationContext.builder()
                .dataManager(dataManager)
                .verbose(false)
                .parallel(false)
                .build()
        );

        System.out.println("\n");

        // List available strategies
        StrategyRegistry.listStrategies();

        OrderTemplate order = OrderTemplate.builder()
                .symbol("BRAP4.SA")
                .quantity(10000)
                .side("BUY")
                .durationHours(2)
                .build();

        List<ExecutionStrategy> strategies = List.of(
                new TwapStrategy(20, TwapStrategy.OrderType.LIMIT_AGGRESSIVE, 3),
                new TwapStrategy(20, TwapStrategy.OrderType.LIMIT_AGGRESSIVE, 7),
                new TwapStrategy(20, TwapStrategy.OrderType.LIMIT_AGGRESSIVE, 10)
        );

        List<String> timesOfDay = List.of("market-open","market-close","12:00","14:00");
        SimulationResults results = Sim.run(order, strategies,timesOfDay,List.of("2025-10-24"));

        results.printFullComparison();
    }

}
