package com.backendlearninglab.corejava.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * CORE JAVA: Arrays, Collections, Wrapper Classes, and Autoboxing Pitfalls.
 * <p>
 * RUNNABLE: Contains its own main() method.
 */
public class CollectionsAndArraysDemo {

    public static void main(String[] args) {
        System.out.println("=== ARRAYS, COLLECTIONS & AUTOBOXING PITFALLS ===");

        demonstrateAutoboxingPitfalls();
        demonstrateArraysVsLists();
        demonstrateSetSemantics();
        demonstrateMapSemantics();
    }

    /**
     * AUTOBOXING / UNBOXING PITFALLS:
     * 1. The Integer Cache Trap (-128 to 127):
     *    Integer.valueOf(100) == Integer.valueOf(100) is TRUE (cached by JVM).
     *    Integer.valueOf(500) == Integer.valueOf(500) is FALSE (new heap objects allocated)!
     *    Always use .equals() for wrapper objects.
     * 2. The Unboxing NullPointerException Trap:
     *    Assigning a null Integer wrapper to a primitive int triggers a hidden .intValue() call,
     *    instantly throwing NullPointerException.
     */
    private static void demonstrateAutoboxingPitfalls() {
        System.out.println("\n[1] Autoboxing & Wrapper Traps:");

        Integer a = 127;
        Integer b = 127;
        System.out.println("  127 == 127 (Cached range): " + (a == b)); // true

        Integer x = 500;
        Integer y = 500;
        System.out.println("  500 == 500 (Outside cache, == compares references): " + (x == y)); // false!
        System.out.println("  x.equals(y) (Correct comparison): " + x.equals(y)); // true

        // The Hidden NullPointerException trap:
        Integer nullableWrapper = null;
        try {
            int primitive = nullableWrapper; // JVM compiles this to: nullableWrapper.intValue()!
        } catch (NullPointerException e) {
            System.out.println("  [NPE CAUGHT] Unboxing null wrapper threw NullPointerException!");
        }
    }

    /**
     * ARRAYS VS LISTS:
     * - Array: Fixed size allocated contiguously in memory. Very fast index access, zero overhead,
     *   supports primitives directly.
     * - ArrayList: Backed by a resizable Object[] array. When full, grows by ~50% (newCapacity = old + (old >> 1)).
     */
    private static void demonstrateArraysVsLists() {
        System.out.println("\n[2] Arrays vs ArrayList:");

        // Fixed-size primitive array
        int[] fixedArray = new int[]{10, 20, 30};
        System.out.println("  Array elements: " + Arrays.toString(fixedArray));

        // Dynamic List
        List<String> dynamicList = new ArrayList<>();
        dynamicList.add("Order #1");
        dynamicList.add("Order #2");
        dynamicList.add("Order #3");
        System.out.println("  Dynamic list size: " + dynamicList.size());
    }

    /**
     * HASHSET VS TREESET:
     * - HashSet: O(1) average time complexity for add/contains/remove. Unordered.
     * - TreeSet: O(log N) operations. Backed by a Red-Black Tree. Elements always kept in Natural Sorted Order.
     */
    private static void demonstrateSetSemantics() {
        System.out.println("\n[3] Set Implementations:");

        Set<String> hashSet = new HashSet<>(List.of("Zebra", "Apple", "Mango"));
        System.out.println("  HashSet (No order guarantee): " + hashSet);

        Set<String> treeSet = new TreeSet<>(List.of("Zebra", "Apple", "Mango"));
        System.out.println("  TreeSet (Naturally sorted): " + treeSet);
    }

    /**
     * HASHMAP VS TREEMAP:
     * - HashMap: Key hashing, handles collisions via LinkedList, treeifies to Red-Black Tree when
     *   bucket threshold >= 8 (TREEIFY_THRESHOLD).
     * - TreeMap: NavigableMap sorted by keys.
     */
    private static void demonstrateMapSemantics() {
        System.out.println("\n[4] Map Implementations:");

        Map<String, Double> prices = new HashMap<>();
        prices.put("Laptop", 1200.00);
        prices.put("Mouse", 25.00);
        prices.put("Keyboard", 75.00);

        // computeIfAbsent: Idiomatic modern Java method to eliminate boilerplate null checks
        prices.computeIfAbsent("Monitor", k -> 350.00);
        System.out.println("  HashMap contents: " + prices);

        Map<String, Double> sortedPrices = new TreeMap<>(prices);
        System.out.println("  TreeMap (Sorted by key): " + sortedPrices);
    }
}
