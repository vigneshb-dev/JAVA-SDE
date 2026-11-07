package com.backendlearninglab.corejava.generics;

import java.util.ArrayList;
import java.util.List;

/**
 * CORE JAVA: Generics, Bounded Types, Wildcards, and the PECS Principle.
 * <p>
 * RUNNABLE: Contains its own main() method.
 */
public class GenericsAndWildcardsDemo {

    public static void main(String[] args) {
        System.out.println("=== GENERICS, BOUNDED TYPES & PECS WILDCARDS ===");

        // 1. Generic Class
        ResultContainer<String> stringResult = ResultContainer.success("Order ORD-123 processed");
        System.out.println("Generic container payload: " + stringResult.getData());

        // 2. Bounded Type Parameter (<T extends Number>)
        NumericStatistics<Double> stats = new NumericStatistics<>(List.of(10.5, 20.0, 35.5));
        System.out.println("Calculated average: " + stats.calculateAverage());

        // 3. PECS Principle: Producer Extends, Consumer Super
        demonstratePecsRule();
    }

    /**
     * THE PECS PRINCIPLE (Producer Extends, Consumer Super):
     * <p>
     * - Use `? extends T` when you only READ from a collection (it PRODUCES items for you).
     * - Use `? super T` when you only WRITE into a collection (it CONSUMES items from you).
     */
    private static void demonstratePecsRule() {
        System.out.println("\n[PECS RULE DEMO]:");

        // PRODUCER EXTENDS (? extends Number):
        // List of Integers can be passed where `? extends Number` is expected
        List<Integer> integers = List.of(1, 2, 3, 4, 5);
        double sum = sumOfNumbers(integers); // PRODUCER: We only read from it
        System.out.println("  Sum from producer: " + sum);

        // CONSUMER SUPER (? super Integer):
        // We can pass a List of Objects or Numbers and safely ADD Integers to it
        List<Number> numberList = new ArrayList<>();
        populateNumbers(numberList); // CONSUMER: We write into it
        System.out.println("  Consumer list after populating: " + numberList);
    }

    // Producer Extends: Reads numbers out
    public static double sumOfNumbers(List<? extends Number> list) {
        double sum = 0.0;
        for (Number n : list) {
            sum += n.doubleValue();
        }
        // list.add(10); // COMPILE ERROR: Cannot add to ? extends! Compiler doesn't know exact subtype.
        return sum;
    }

    // Consumer Super: Writes integers in
    public static void populateNumbers(List<? super Integer> destination) {
        destination.add(100);
        destination.add(200);
        destination.add(300);
        // Integer item = destination.get(0); // COMPILE ERROR: Can only read as Object!
    }

    /**
     * GENERIC CLASS: Type safety at compile time.
     * Prevents ClassCastExceptions caused by untyped Object references.
     */
    public static class ResultContainer<T> {
        private final T data;
        private final boolean success;

        private ResultContainer(T data, boolean success) {
            this.data = data;
            this.success = success;
        }

        public static <T> ResultContainer<T> success(T data) {
            return new ResultContainer<>(data, true);
        }

        public T getData() {
            return data;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    /**
     * BOUNDED GENERICS: Limits type parameter to Number or any of its subclasses.
     */
    public static class NumericStatistics<T extends Number> {
        private final List<T> numbers;

        public NumericStatistics(List<T> numbers) {
            this.numbers = numbers;
        }

        public double calculateAverage() {
            if (numbers.isEmpty()) return 0.0;
            double total = 0;
            for (T num : numbers) {
                total += num.doubleValue();
            }
            return total / numbers.size();
        }
    }

    /*
     * WHAT WOULD HAPPEN WITH BAD CODE (Raw Types pre-Java 5)?
     * 
     * List rawList = new ArrayList();
     * rawList.add("Text");
     * rawList.add(123); // Compiler allows anything!
     * 
     * String s = (String) rawList.get(1); // RUNTIME ClassCastException crashes app in production!
     * 
     * Generics catch this at COMPILE TIME before any code ever touches a server.
     */
}
