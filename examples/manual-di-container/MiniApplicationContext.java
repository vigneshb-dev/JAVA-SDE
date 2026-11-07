package com.backendlearninglab.examples.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * EDUCATIONAL SIMULATION: Micro Dependency Injection Container.
 * <p>
 * PURPOSE:
 * Demystifies Spring's ApplicationContext and BeanFactory by showing how
 * Inversion of Control (IoC) and Dependency Injection (DI) work under the hood
 * using pure Java Reflection API.
 * <p>
 * THIS IS NOT SPRING INTERNALS — It is a simplified mental model.
 */
public class MiniApplicationContext {

    // The Bean Registry (Spring's DefaultSingletonBeanRegistry singletonObjects map)
    private final Map<Class<?>, Object> beanRegistry = new HashMap<>();

    // Tracks classes currently being created to detect circular dependencies
    private final Set<Class<?>> currentlyInCreation = new HashSet<>();

    public MiniApplicationContext(Class<?>... componentClasses) {
        System.out.println("[MiniApplicationContext] Initializing container...");
        for (Class<?> clazz : componentClasses) {
            if (clazz.isAnnotationPresent(MiniComponent.class)) {
                // Ensure bean is instantiated and registered
                getBean(clazz);
            }
        }
        System.out.println("[MiniApplicationContext] Container initialization complete. Registered "
                + beanRegistry.size() + " singleton beans.\n");
    }

    /**
     * Resolves and returns a singleton bean of the requested type.
     * If the bean does not exist yet, instantiates it and resolves its dependencies.
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T getBean(Class<T> requiredType) {
        // 1. Check if bean already exists in registry (First-Level Cache / Singleton cache)
        if (beanRegistry.containsKey(requiredType)) {
            return (T) beanRegistry.get(requiredType);
        }

        // 2. Check for circular dependency (A depends on B, B depends on A)
        if (currentlyInCreation.contains(requiredType)) {
            throw new IllegalStateException("Circular dependency detected while creating bean: " + requiredType.getName());
        }

        currentlyInCreation.add(requiredType);
        try {
            // 3. Instantiate the bean (Constructor Injection preferred)
            T instance = createBeanInstance(requiredType);

            // 4. Register the early bean reference in the registry
            beanRegistry.put(requiredType, instance);

            // 5. Populate remaining field-injected dependencies
            populateFieldInjections(instance);

            return instance;
        } finally {
            currentlyInCreation.remove(requiredType);
        }
    }

    /**
     * Resolves the constructor to use and injects constructor parameters.
     */
    @SuppressWarnings("unchecked")
    private <T> T createBeanInstance(Class<T> clazz) {
        try {
            Constructor<?> targetConstructor = null;

            // Search for constructor annotated with @MiniInject
            for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                if (ctor.isAnnotationPresent(MiniInject.class)) {
                    targetConstructor = ctor;
                    break;
                }
            }

            // Fallback to single public constructor if only one exists (Spring 4.3+ behavior!)
            if (targetConstructor == null && clazz.getDeclaredConstructors().length == 1) {
                targetConstructor = clazz.getDeclaredConstructors()[0];
            }

            if (targetConstructor == null) {
                targetConstructor = clazz.getDeclaredConstructor();
            }

            targetConstructor.setAccessible(true);

            // Resolve dependencies for constructor arguments
            Class<?>[] parameterTypes = targetConstructor.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                System.out.println("  -> Resolving constructor dependency: " + parameterTypes[i].getSimpleName()
                        + " for " + clazz.getSimpleName());
                args[i] = getBean(parameterTypes[i]); // Recursive lookup
            }

            System.out.println("  -> Instantiating bean: " + clazz.getSimpleName());
            return (T) targetConstructor.newInstance(args);

        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate bean: " + clazz.getName(), e);
        }
    }

    /**
     * Injects dependencies into fields annotated with @MiniInject (Field Injection simulation).
     */
    private void populateFieldInjections(Object instance) {
        Class<?> clazz = instance.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(MiniInject.class)) {
                try {
                    field.setAccessible(true);
                    if (field.get(instance) == null) {
                        System.out.println("  -> Field injecting: " + field.getName() + " in " + clazz.getSimpleName());
                        Object dependency = getBean(field.getType());
                        field.set(instance, dependency);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed field injection on: " + field.getName(), e);
                }
            }
        }
    }
}
