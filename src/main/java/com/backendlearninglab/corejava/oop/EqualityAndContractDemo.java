package com.backendlearninglab.corejava.oop;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CORE JAVA: equals(), hashCode(), toString(), and the Immutable Object Pattern.
 * <p>
 * RUNNABLE: Contains its own main() method.
 */
public class EqualityAndContractDemo {

    public static void main(String[] args) {
        System.out.println("=== EQUALS, HASHCODE CONTRACT & IMMUTABILITY ===");

        // 1. Identity (==) vs Equality (.equals)
        String s1 = new String("SKU-100");
        String s2 = new String("SKU-100");

        System.out.println("s1 == s2 (Reference Identity): " + (s1 == s2)); // false: different memory heap locations
        System.out.println("s1.equals(s2) (Logical Equivalence): " + s1.equals(s2)); // true: identical characters

        // 2. The Dreaded equals/hashCode Contract Violation
        demonstrateBrokenHashCodeInHashSet();

        // 3. Proper Immutable Object Usage
        demonstrateProperImmutableValueObject();
    }

    /**
     * WHAT HAPPENS WHEN YOU OVERRIDE equals() BUT FORGET hashCode()?
     * <p>
     * THE CONTRACT:
     * If objA.equals(objB) is true, then objA.hashCode() MUST EQUAL objB.hashCode()!
     * <p>
     * CONSEQUENCE OF VIOLATION:
     * When you put an object in a HashSet or HashMap, the collection computes hashCode()
     * to determine which hash bucket to place it in.
     * If two logically equal objects have DIFFERENT hash codes, HashSet will store them in
     * different buckets, creating DUPLICATES in a Set!
     * Even worse, map.get(equalKey) returns NULL!
     */
    private static void demonstrateBrokenHashCodeInHashSet() {
        System.out.println("\n[THE PITFALL] Broken HashCode Contract in HashSet:");

        Set<BrokenKey> brokenSet = new HashSet<>();
        BrokenKey k1 = new BrokenKey("user_42");
        BrokenKey k2 = new BrokenKey("user_42");

        System.out.println("k1.equals(k2)? " + k1.equals(k2)); // true
        System.out.println("k1.hashCode() == k2.hashCode()? " + (k1.hashCode() == k2.hashCode())); // FALSE! Random identity hashes!

        brokenSet.add(k1);
        brokenSet.add(k2);

        System.out.println("HashSet size (Should be 1 because keys are equal): " + brokenSet.size() + " (BUG: Duplicates in Set!)");
    }

    private static void demonstrateProperImmutableValueObject() {
        System.out.println("\n[PROFESSIONAL PATTERN] Correct Immutable Key in HashMap:");

        Map<ProductSku, String> catalogMap = new HashMap<>();
        ProductSku skuA = new ProductSku("TECH", 101);
        ProductSku skuB = new ProductSku("TECH", 101);

        catalogMap.put(skuA, "High-End Wireless Mouse");

        // Lookup using a different object instance that is logically equal
        String retrieved = catalogMap.get(skuB);
        System.out.println("Retrieved by equal key: " + retrieved + " (SUCCESS!)");
        System.out.println("toString representation: " + skuA);
    }

    // BROKEN CLASS: Overrides equals() but forgets hashCode()
    static class BrokenKey {
        private final String id;

        BrokenKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BrokenKey brokenKey = (BrokenKey) o;
            return Objects.equals(id, brokenKey.id);
        }
        // Missing hashCode() -> JVM uses default System.identityHashCode (memory address)
    }

    /**
     * PROPER IMMUTABLE VALUE OBJECT
     * Rules of Immutability:
     * 1. Class declared 'final' so subclasses cannot override methods to introduce mutability.
     * 2. All fields are 'private final'.
     * 3. No setter methods.
     * 4. If fields contain mutable objects (e.g. Date, List), perform defensive copying.
     */
    public static final class ProductSku {
        private final String categoryPrefix;
        private final int sequenceNumber;

        public ProductSku(String categoryPrefix, int sequenceNumber) {
            if (categoryPrefix == null || categoryPrefix.isBlank()) {
                throw new IllegalArgumentException("Prefix cannot be blank");
            }
            this.categoryPrefix = categoryPrefix.trim().toUpperCase();
            this.sequenceNumber = sequenceNumber;
        }

        public String getCategoryPrefix() {
            return categoryPrefix;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductSku that = (ProductSku) o;
            return sequenceNumber == that.sequenceNumber &&
                    Objects.equals(categoryPrefix, that.categoryPrefix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(categoryPrefix, sequenceNumber);
        }

        @Override
        public String toString() {
            return categoryPrefix + "-" + sequenceNumber;
        }
    }
}
