package com.backendlearninglab.examples.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Educational Simulation of Spring's @Component.
 * <p>
 * Marks a class as managed by our MiniApplicationContext.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MiniComponent {
}
