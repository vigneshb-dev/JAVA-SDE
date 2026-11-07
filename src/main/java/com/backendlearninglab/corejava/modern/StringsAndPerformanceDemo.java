package com.backendlearninglab.corejava.modern;

/**
 * CORE JAVA: String Immutability, String Constant Pool, and String vs. StringBuilder Performance.
 * <p>
 * RUNNABLE: Contains its own main() method with a live performance benchmark.
 */
public class StringsAndPerformanceDemo {

    public static void main(String[] args) {
        System.out.println("=== STRINGS, CONSTANT POOL & STRINGBUILDER PERFORMANCE ===");

        // 1. String Constant Pool Mechanics
        demonstrateStringConstantPool();

        // 2. The Loop Concatenation Trap (String +=) vs StringBuilder
        demonstrateConcatenationBenchmark();
    }

    /**
     * STRING CONSTANT POOL:
     * - Strings created via literals ("Hello") live in the String Constant Pool (in JVM Metaspace/Heap).
     * - Identical literals point to the EXACT SAME memory address.
     * - Strings created with `new String("Hello")` bypass pool reuse and force a new heap object.
     */
    private static void demonstrateStringConstantPool() {
        System.out.println("\n[1] String Constant Pool vs Heap Allocations:");

        String literal1 = "ORDER_PENDING";
        String literal2 = "ORDER_PENDING";
        String heapAllocated = new String("ORDER_PENDING");

        System.out.println("  literal1 == literal2 (Same pool reference): " + (literal1 == literal2)); // true
        System.out.println("  literal1 == heapAllocated (Different references): " + (literal1 == heapAllocated)); // false
        System.out.println("  literal1.equals(heapAllocated) (Equal contents): " + literal1.equals(heapAllocated)); // true

        // Calling .intern() looks up or registers the string in the pool
        String interned = heapAllocated.intern();
        System.out.println("  literal1 == interned (After calling .intern()): " + (literal1 == interned)); // true
    }

    /**
     * THE LOOP CONCATENATION DISASTER:
     * <p>
     * WHAT HAPPENS IN BAD CODE:
     * String result = "";
     * for (int i = 0; i < N; i++) {
     *     result += i; // BAD!
     * }
     * Because Strings are IMMUTABLE, each `+=` creates a brand new String and array copy!
     * Complexity is O(N^2) in time and memory, triggering massive Garbage Collection pauses.
     * <p>
     * THE FIX: StringBuilder
     * Uses a mutable char/byte array that dynamically doubles in size when needed (O(N) total time).
     */
    private static void demonstrateConcatenationBenchmark() {
        System.out.println("\n[2] Live Benchmark: String += vs. StringBuilder (20,000 iterations):");

        int iterations = 20_000;

        // Test 1: Inefficient String Concatenation
        long startStr = System.nanoTime();
        String badResult = "";
        for (int i = 0; i < iterations; i++) {
            badResult += "x";
        }
        long durationStrMs = (System.nanoTime() - startStr) / 1_000_000;
        System.out.println("  String (+):        " + durationStrMs + " ms (Created 20,000 intermediate heap garbage objects!)");

        // Test 2: High-Performance StringBuilder
        long startSb = System.nanoTime();
        StringBuilder goodBuilder = new StringBuilder(iterations); // Pre-sized buffer avoids resizing
        for (int i = 0; i < iterations; i++) {
            goodBuilder.append("x");
        }
        String goodResult = goodBuilder.toString();
        long durationSbMs = (System.nanoTime() - startSb) / 1_000_000;
        System.out.println("  StringBuilder:     " + durationSbMs + " ms (Single allocated buffer)");

        if (durationStrMs > 0) {
            System.out.printf("  --> StringBuilder is ~%dx faster!%n", (durationStrMs / Math.max(1, durationSbMs)));
        }
    }
}
