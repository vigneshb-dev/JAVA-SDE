package com.backendlearninglab.examples.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Educational Simulation of Spring's @Autowired / @Inject.
 * <p>
 * Signals our container to resolve and inject the required dependency.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface MiniInject {
}
