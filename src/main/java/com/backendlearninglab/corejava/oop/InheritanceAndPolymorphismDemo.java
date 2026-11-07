package com.backendlearninglab.corejava.oop;

import java.math.BigDecimal;
import java.util.List;

/**
 * CORE JAVA: Inheritance, Polymorphism, Abstraction, and Interfaces.
 * <p>
 * RUNNABLE: Contains its own main() method for direct execution.
 */
public class InheritanceAndPolymorphismDemo {

    public static void main(String[] args) {
        System.out.println("=== INHERITANCE, ABSTRACTION & POLYMORPHISM ===");

        // POLYMORPHISM IN ACTION:
        // Variable type is the Interface (PaymentProcessor), but the runtime instances are concrete implementations.
        List<PaymentProcessor> processors = List.of(
                new CreditCardProcessor("4111-XXXX-XXXX-1111"),
                new UpiProcessor("alice@okhdfcbank"),
                new CryptoProcessor("0x71C...B29")
        );

        BigDecimal orderAmount = new BigDecimal("150.00");

        // The caller does NOT know or care whether it's Credit Card, UPI, or Crypto!
        // The JVM automatically dispatches to the correct subclass method at runtime (Dynamic Dispatch).
        for (PaymentProcessor processor : processors) {
            System.out.println("\nProcessing via: " + processor.getProviderName());
            boolean success = processor.processPayment(orderAmount);
            System.out.println("Result: " + (success ? "APPROVED" : "DECLINED"));
            processor.generateAuditReceipt(orderAmount); // Invoking default method from interface
        }
    }

    // -------------------------------------------------------------------------
    // 1. INTERFACE: The Contract
    // -------------------------------------------------------------------------
    // WHAT: Defines what behaviors a class MUST provide, without dictating how.
    // WHY: Enables loose coupling. Code depends on abstractions, not concrete implementations
    //      (Dependency Inversion Principle).
    public interface PaymentProcessor {

        boolean processPayment(BigDecimal amount);

        String getProviderName();

        // WHAT: Default method (Java 8+).
        // WHY: Allows adding new utility methods to interfaces without breaking existing implementations.
        default void generateAuditReceipt(BigDecimal amount) {
            System.out.println("  [AUDIT LOG] Payment of $" + amount + " recorded for provider: " + getProviderName());
        }
    }

    // -------------------------------------------------------------------------
    // 2. ABSTRACT BASE CLASS: Shared Code + Template Pattern
    // -------------------------------------------------------------------------
    // WHAT: Cannot be instantiated directly. Holds shared state and common logic.
    // WHY: Prevents code duplication among related payment providers.
    public static abstract class AbstractPaymentProcessor implements PaymentProcessor {

        private final String accountIdentifier;

        protected AbstractPaymentProcessor(String accountIdentifier) {
            if (accountIdentifier == null || accountIdentifier.isBlank()) {
                throw new IllegalArgumentException("Account identifier cannot be blank");
            }
            this.accountIdentifier = accountIdentifier;
        }

        public String getAccountIdentifier() {
            return accountIdentifier;
        }

        // Common validation logic shared across all subclasses
        protected void validateTransaction(BigDecimal amount) {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Transaction amount must be positive: " + amount);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. CONCRETE SUBCLASSES (Polymorphic Implementations)
    // -------------------------------------------------------------------------
    public static class CreditCardProcessor extends AbstractPaymentProcessor {
        public CreditCardProcessor(String maskedCardNumber) {
            super(maskedCardNumber);
        }

        @Override
        public boolean processPayment(BigDecimal amount) {
            validateTransaction(amount);
            System.out.println("  Contacting Visa/Mastercard network for card: " + getAccountIdentifier());
            return true;
        }

        @Override
        public String getProviderName() {
            return "CreditCard Gateway";
        }
    }

    public static class UpiProcessor extends AbstractPaymentProcessor {
        public UpiProcessor(String upiVpa) {
            super(upiVpa);
        }

        @Override
        public boolean processPayment(BigDecimal amount) {
            validateTransaction(amount);
            System.out.println("  Sending instant push notification to UPI handle: " + getAccountIdentifier());
            return true;
        }

        @Override
        public String getProviderName() {
            return "UPI Instant Payment";
        }
    }

    public static class CryptoProcessor extends AbstractPaymentProcessor {
        public CryptoProcessor(String walletAddress) {
            super(walletAddress);
        }

        @Override
        public boolean processPayment(BigDecimal amount) {
            validateTransaction(amount);
            System.out.println("  Verifying block confirmations for wallet: " + getAccountIdentifier());
            return true;
        }

        @Override
        public String getProviderName() {
            return "Web3 Crypto Settlement";
        }
    }

    /*
     * WHAT WOULD HAPPEN WITH BAD CODE?
     * 
     * Bad Code Anti-Pattern (Spaghetti Type-Checking):
     * public void processPayment(String type, BigDecimal amount) {
     *     if (type.equals("CREDIT_CARD")) {
     *         // 50 lines of Visa logic
     *     } else if (type.equals("UPI")) {
     *         // 50 lines of UPI logic
     *     } else if (type.equals("CRYPTO")) {
     *         // 50 lines of Crypto logic
     *     }
     * }
     * 
     * Consequences:
     * 1. Violates Open/Closed Principle: Adding PayPal requires modifying the existing method,
     *    risking regressions across all other payment methods.
     * 2. Massive, brittle method prone to merge conflicts in teams.
     * 3. Cannot mock individual providers during unit tests.
     */
}
