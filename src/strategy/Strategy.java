package strategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark execution strategy classes with metadata
 * Enables automatic discovery and documentation of available strategies
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Strategy {
    /**
     * Strategy name (e.g., "TWAP", "VWAP")
     */
    String name();

    /**
     * Human-readable description
     */
    String description();

    /**
     * Strategy parameters
     */
    Parameter[] parameters() default {};
}
