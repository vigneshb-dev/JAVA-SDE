package com.backendlearninglab.corejava.functional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * CORE JAVA: Functional Interfaces, Lambdas, Method References, Stream API, and Optional.
 * <p>
 * RUNNABLE: Contains its own main() method.
 */
public class FunctionalAndStreamDemo {

    public static void main(String[] args) {
        System.out.println("=== FUNCTIONAL PROGRAMMING & STREAM API ===");

        demonstrateCoreFunctionalInterfaces();
        demonstrateStreamPipeline();
        demonstrateOptionalMastery();
    }

    /**
     * THE FOUR CORE FUNCTIONAL INTERFACES:
     * 1. Predicate<T>:    Takes T -> returns boolean  (used for filtering)
     * 2. Function<T, R>:   Takes T -> returns R        (used for mapping/transformation)
     * 3. Consumer<T>:      Takes T -> returns void     (used for side-effects like printing/saving)
     * 4. Supplier<T>:      Takes void -> returns T     (used for lazy evaluation / factory generation)
     */
    private static void demonstrateCoreFunctionalInterfaces() {
        System.out.println("\n[1] Core Functional Interfaces & Method References:");

        Predicate<String> isNotEmpty = s -> !s.isBlank();
        Function<String, Integer> stringLength = String::length; // Method reference syntax
        Consumer<String> printer = System.out::println;
        Supplier<Double> randomSupplier = Math::random;

        String testStr = "OrderFlow Backend";
        if (isNotEmpty.test(testStr)) {
            printer.accept("  String '" + testStr + "' length: " + stringLength.apply(testStr));
        }
        System.out.println("  Lazy generated random number: " + randomSupplier.get());
    }

    /**
     * STREAM API: Declarative, Fluent Data Transformation Pipeline.
     * <p>
     * Three stages of a Stream:
     * 1. Source: Collection, array, or I/O channel.
     * 2. Intermediate Operations: (filter, map, sorted) - Lazy! Not executed until terminal op is called.
     * 3. Terminal Operation: (collect, count, reduce, forEach) - Triggers actual execution.
     */
    private static void demonstrateStreamPipeline() {
        System.out.println("\n[2] Stream API: Transforming E-Commerce Products:");

        List<ProductDto> products = List.of(
                new ProductDto("Laptop", "TECH", new BigDecimal("1200.00")),
                new ProductDto("Mouse", "TECH", new BigDecimal("25.00")),
                new ProductDto("Keyboard", "TECH", new BigDecimal("85.00")),
                new ProductDto("Desk", "FURNITURE", new BigDecimal("450.00")),
                new ProductDto("Chair", "FURNITURE", new BigDecimal("220.00"))
        );

        // Task: Find names of all TECH products over $50, sorted alphabetically
        List<String> expensiveTechNames = products.stream()
                .filter(p -> "TECH".equals(p.category()))
                .filter(p -> p.price().compareTo(new BigDecimal("50.00")) > 0)
                .map(ProductDto::name) // Method reference
                .sorted()
                .collect(Collectors.toList());

        System.out.println("  Filtered & sorted tech items: " + expensiveTechNames);

        // Grouping by Category:
        Map<String, List<ProductDto>> grouped = products.stream()
                .collect(Collectors.groupingBy(ProductDto::category));
        System.out.println("  Categories count: " + grouped.keySet());

        // Aggregate Sum using reduce:
        BigDecimal totalInventoryValue = products.stream()
                .map(ProductDto::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("  Total catalog value: $" + totalInventoryValue);
    }

    /**
     * OPTIONAL<T>: The Solution to NullPointerExceptions.
     * <p>
     * GOLDEN RULES OF OPTIONAL:
     * 1. NEVER call optional.get() without checking isPresent() first (calling .get() directly defeats the purpose!).
     * 2. Prefer fluent methods: map(), flatMap(), filter(), orElse(), orElseGet(), orElseThrow().
     * 3. Do NOT use Optional as a class field or method parameter (it was designed purely for return types).
     */
    private static void demonstrateOptionalMastery() {
        System.out.println("\n[3] Optional Best Practices vs. Anti-Patterns:");

        // Safe lookup simulating finding a user by ID
        Optional<String> foundUser = findUserById(42L);
        Optional<String> missingUser = findUserById(999L);

        // Idiomatic: orElse / orElseGet
        String userName = foundUser.orElse("Anonymous Guest");
        System.out.println("  Found user: " + userName);

        // Idiomatic: map + ifPresent
        foundUser.map(String::toUpperCase)
                .ifPresent(name -> System.out.println("  Uppercase user name: " + name));

        // Idiomatic: orElseThrow for missing entities
        try {
            missingUser.orElseThrow(() -> new IllegalArgumentException("User with ID 999 does not exist in DB"));
        } catch (IllegalArgumentException e) {
            System.out.println("  Properly raised domain exception via orElseThrow: " + e.getMessage());
        }
    }

    private static Optional<String> findUserById(Long id) {
        if (id.equals(42L)) {
            return Optional.of("Alice Smith");
        }
        return Optional.empty();
    }

    record ProductDto(String name, String category, BigDecimal price) {}
}
