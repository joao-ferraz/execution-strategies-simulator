package strategy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to describe a strategy parameter
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    /**
     * Parameter name
     */
    String name();

    /**
     * Parameter type
     */
    Class<?> type();

    /**
     * Human-readable description
     */
    String description();

    /**
     * Default value as string (optional)
     */
    String defaultValue() default "";
}
