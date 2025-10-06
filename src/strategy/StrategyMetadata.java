package strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata about a strategy including its parameters
 * Built from @Strategy annotation
 */
public class StrategyMetadata {
    private final String name;
    private final String description;
    private final Class<? extends ExecutionStrategy> strategyClass;
    private final List<ParameterMetadata> parameters;

    public StrategyMetadata(String name, String description,
                           Class<? extends ExecutionStrategy> strategyClass,
                           List<ParameterMetadata> parameters) {
        this.name = name;
        this.description = description;
        this.strategyClass = strategyClass;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Class<? extends ExecutionStrategy> getStrategyClass() {
        return strategyClass;
    }

    public List<ParameterMetadata> getParameters() {
        return parameters;
    }

    /**
     * Print formatted metadata to console
     */
    public void print() {
        System.out.println("\n" + name + " - " + description);
        if (!parameters.isEmpty()) {
            System.out.println("  Parameters:");
            for (ParameterMetadata param : parameters) {
                String defaultInfo = param.hasDefault() ? " (default: " + param.getDefaultValue() + ")" : "";
                System.out.println(String.format("    - %s (%s): %s%s",
                    param.getName(),
                    param.getType().getSimpleName(),
                    param.getDescription(),
                    defaultInfo));
            }
        }
    }

    @Override
    public String toString() {
        return name + " (" + strategyClass.getSimpleName() + ")";
    }

    /**
     * Metadata about a single strategy parameter
     */
    public static class ParameterMetadata {
        private final String name;
        private final Class<?> type;
        private final String description;
        private final String defaultValue;

        public ParameterMetadata(String name, Class<?> type, String description, String defaultValue) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public Class<?> getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public boolean hasDefault() {
            return defaultValue != null && !defaultValue.isEmpty();
        }

        @Override
        public String toString() {
            return name + " (" + type.getSimpleName() + ")";
        }
    }
}
