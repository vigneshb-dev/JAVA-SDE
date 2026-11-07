package com.backendlearninglab.corejava.modern;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * CORE JAVA: Rich Enums with Behavior and Modern Java Records (Java 16+).
 * <p>
 * RUNNABLE: Contains its own main() method.
 */
public class EnumsAndRecordsDemo {

    public static void main(String[] args) {
        System.out.println("=== RICH ENUMS & JAVA RECORDS ===");

        // 1. Rich Enums: Encapsulating business rules and polymorphic behavior inside enum constants
        demonstrateRichEnumBehavior();

        // 2. Records: Transparent, immutable data carriers replacing verbose boilerplate
        demonstrateModernRecords();
    }

    private static void demonstrateRichEnumBehavior() {
        System.out.println("\n[1] Rich Enums with Inherent Business Logic:");

        BigDecimal orderSubtotal = new BigDecimal("100.00");

        for (ShippingTier tier : ShippingTier.values()) {
            BigDecimal fee = tier.calculateShippingCost(orderSubtotal);
            System.out.printf("  Tier: %-12s | Cost: $%6.2f | Est. Delivery: %d days%n",
                    tier.name(), fee, tier.getEstimatedDays());
        }
    }

    private static void demonstrateModernRecords() {
        System.out.println("\n[2] Java Records (Immutable Data Carriers):");

        // Canonical instantiation
        CustomerAddress address = new CustomerAddress("742 Evergreen Terrace", "Springfield", "97477");
        System.out.println("  Record toString(): " + address);
        System.out.println("  Record accessor (not getCity(), but city()): " + address.city());

        // Compact constructor validation check
        try {
            new CustomerAddress("", "Springfield", "97477");
        } catch (IllegalArgumentException e) {
            System.out.println("  Record validation correctly triggered: " + e.getMessage());
        }
    }

    /**
     * RICH ENUM:
     * Enums in Java are full-fledged classes! They can have fields, constructors, and abstract methods
     * implemented specifically by each constant.
     */
    public enum ShippingTier {
        STANDARD(5) {
            @Override
            public BigDecimal calculateShippingCost(BigDecimal subtotal) {
                // Free shipping if order > $50
                return (subtotal.compareTo(new BigDecimal("50.00")) > 0)
                        ? BigDecimal.ZERO
                        : new BigDecimal("5.99");
            }
        },
        EXPRESS(2) {
            @Override
            public BigDecimal calculateShippingCost(BigDecimal subtotal) {
                return new BigDecimal("14.99");
            }
        },
        OVERNIGHT(1) {
            @Override
            public BigDecimal calculateShippingCost(BigDecimal subtotal) {
                return new BigDecimal("29.99");
            }
        };

        private final int estimatedDays;

        ShippingTier(int estimatedDays) {
            this.estimatedDays = estimatedDays;
        }

        public int getEstimatedDays() {
            return estimatedDays;
        }

        // Each enum constant must supply its own pricing algorithm
        public abstract BigDecimal calculateShippingCost(BigDecimal subtotal);
    }

    /**
     * JAVA RECORD:
     * <p>
     * WHAT: A specialized class for immutable data holding.
     * The compiler automatically generates:
     * - private final fields
     * - canonical constructor
     * - getters (named street(), city(), zipCode())
     * - equals() and hashCode() based on all components
     * - toString()
     * <p>
     * COMPACT CONSTRUCTOR:
     * Notice the constructor has no parameter list! It is used purely for validation and normalization.
     */
    public record CustomerAddress(String street, String city, String zipCode) {

        public CustomerAddress {
            // Compact constructor for validation
            Objects.requireNonNull(street, "street cannot be null");
            Objects.requireNonNull(city, "city cannot be null");
            Objects.requireNonNull(zipCode, "zipCode cannot be null");

            if (street.isBlank()) {
                throw new IllegalArgumentException("Street cannot be blank");
            }
            // Normalization
            street = street.trim();
            city = city.trim();
            zipCode = zipCode.trim();
        }
    }
}
