package strategy;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry for discovering and instantiating execution strategies
 * Scans classpath for @Strategy annotated classes
 */
public class StrategyRegistry {
    private static final Map<String, StrategyMetadata> registry = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Initialize registry by scanning classpath for @Strategy annotations
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            // Scan strategy package for annotated classes
            List<Class<?>> classes = findClassesInPackage("strategy");

            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(Strategy.class)) {
                    if (!ExecutionStrategy.class.isAssignableFrom(clazz)) {
                        System.err.println("Warning: " + clazz.getName() +
                            " has @Strategy but doesn't implement ExecutionStrategy");
                        continue;
                    }

                    Strategy annotation = clazz.getAnnotation(Strategy.class);

                    // Build parameter metadata
                    List<StrategyMetadata.ParameterMetadata> params = new ArrayList<>();
                    for (Parameter param : annotation.parameters()) {
                        params.add(new StrategyMetadata.ParameterMetadata(
                            param.name(),
                            param.type(),
                            param.description(),
                            param.defaultValue()
                        ));
                    }

                    // Create metadata
                    @SuppressWarnings("unchecked")
                    Class<? extends ExecutionStrategy> strategyClass =
                        (Class<? extends ExecutionStrategy>) clazz;

                    StrategyMetadata metadata = new StrategyMetadata(
                        annotation.name(),
                        annotation.description(),
                        strategyClass,
                        params
                    );

                    registry.put(annotation.name(), metadata);
                }
            }

            initialized = true;
            System.out.println("[StrategyRegistry] Initialized with " + registry.size() + " strategies");

        } catch (Exception e) {
            System.err.println("Error initializing StrategyRegistry: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get all registered strategies
     */
    public static List<StrategyMetadata> getAllStrategies() {
        ensureInitialized();
        return new ArrayList<>(registry.values());
    }

    /**
     * Get metadata for a specific strategy by name
     */
    public static StrategyMetadata getMetadata(String name) {
        ensureInitialized();
        return registry.get(name);
    }

    /**
     * Check if a strategy exists
     */
    public static boolean hasStrategy(String name) {
        ensureInitialized();
        return registry.containsKey(name);
    }

    /**
     * Create strategy instance from name and parameters
     */
    public static ExecutionStrategy create(String name, Object... params) {
        ensureInitialized();

        StrategyMetadata metadata = registry.get(name);
        if (metadata == null) {
            throw new IllegalArgumentException("Strategy not found: " + name);
        }

        try {
            // Find constructor matching parameter types
            Class<?>[] paramTypes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                paramTypes[i] = params[i].getClass();
                // Handle primitive types
                if (params[i] instanceof Integer) paramTypes[i] = int.class;
                else if (params[i] instanceof Double) paramTypes[i] = double.class;
                else if (params[i] instanceof Boolean) paramTypes[i] = boolean.class;
                else if (params[i] instanceof Long) paramTypes[i] = long.class;
            }

            Constructor<? extends ExecutionStrategy> constructor =
                metadata.getStrategyClass().getConstructor(paramTypes);

            return constructor.newInstance(params);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create strategy " + name + ": " + e.getMessage(), e);
        }
    }

    /**
     * List all available strategies to console
     */
    public static void listStrategies() {
        ensureInitialized();

        if (registry.isEmpty()) {
            System.out.println("No strategies found. Make sure classes are annotated with @Strategy");
            return;
        }

        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║           AVAILABLE EXECUTION STRATEGIES           ║");
        System.out.println("╚════════════════════════════════════════════════════╝");

        for (StrategyMetadata metadata : registry.values()) {
            metadata.print();
        }

        System.out.println();
    }

    /**
     * Find all classes in a package
     */
    private static List<Class<?>> findClassesInPackage(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }

        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }

        return classes;
    }

    /**
     * Recursively find classes in directory
     */
    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();

        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    // Skip classes that can't be loaded
                }
            }
        }

        return classes;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }
}
