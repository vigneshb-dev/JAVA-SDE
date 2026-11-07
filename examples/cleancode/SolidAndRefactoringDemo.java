package com.backendlearninglab.examples.cleancode;

import java.math.BigDecimal;

/**
 * EDUCATIONAL DEMONSTRATION: Clean Code & Architecture Principles.
 * <p>
 * TEACHING FOCUS:
 * <ul>
 *   <li>1. <b>Single Responsibility Principle (SRP):</b> Refactoring a "God Object" doing DB queries,
 *          discount math, credit card charging, and email sending into decoupled single-purpose components.</li>
 *   <li>2. <b>Open/Closed Principle (OCP):</b> Using polymorphism/interfaces for payment strategies
 *          instead of giant {@code switch(paymentType)} statements.</li>
 *   <li>3. <b>Composition over Inheritance:</b> Why deep class inheritance hierarchies break under change.</li>
 *   <li>4. <b>KISS & YAGNI:</b> Recognizing over-engineering and premature abstraction.</li>
 * </ul>
 */
public class SolidAndRefactoringDemo {

    // =========================================================================
    // THE "BAD" CODE: God Object Anti-Pattern (Violates SRP, OCP, and DIP)
    // =========================================================================
    public static class BadOrderProcessor {
        public void processOrder(String orderId, String customerEmail, String paymentType, BigDecimal amount) {
            System.out.println("--- [BAD CODE] Executing God Object Method ---");

            // 1. Inlined Tax/Discount Logic (Should be separate domain logic)
            BigDecimal finalAmount = amount;
            if (amount.compareTo(new BigDecimal("100")) > 0) {
                finalAmount = amount.multiply(new BigDecimal("0.90")); // 10% discount
            }

            // 2. Giant Switch Statement for Payment (Violates OCP - Adding ApplePay requires modifying this class!)
            if ("CREDIT_CARD".equalsIgnoreCase(paymentType)) {
                System.out.println("  Connecting to Stripe gateway... Charged: $" + finalAmount);
            } else if ("PAYPAL".equalsIgnoreCase(paymentType)) {
                System.out.println("  Connecting to PayPal API... Charged: $" + finalAmount);
            } else {
                throw new IllegalArgumentException("Unsupported payment type: " + paymentType);
            }

            // 3. Inlined Notification Logic (Violates SRP - OrderProcessor should not know SMTP protocols!)
            System.out.println("  Opening SMTP socket to mail.server.com:25...");
            System.out.println("  Email sent to " + customerEmail + " for Order " + orderId);

            // 4. Inlined Persistence
            System.out.println("  Writing directly to SQL database: INSERT INTO orders VALUES('" + orderId + "');");
        }
    }

    // =========================================================================
    // THE "REFACTORED" CLEAN CODE: SOLID & Composition
    // =========================================================================

    // OCP & DIP: Payment Strategy Abstraction
    public interface PaymentGateway {
        void charge(String orderId, BigDecimal amount);
    }

    public static class StripePaymentGateway implements PaymentGateway {
        @Override
        public void charge(String orderId, BigDecimal amount) {
            System.out.println("  [Stripe] Successfully charged $" + amount + " for Order " + orderId);
        }
    }

    public static class PayPalPaymentGateway implements PaymentGateway {
        @Override
        public void charge(String orderId, BigDecimal amount) {
            System.out.println("  [PayPal] Successfully captured $" + amount + " for Order " + orderId);
        }
    }

    // SRP: Notification Service
    public interface NotificationService {
        void sendOrderConfirmation(String email, String orderId);
    }

    public static class EmailNotificationService implements NotificationService {
        @Override
        public void sendOrderConfirmation(String email, String orderId) {
            System.out.println("  [Email] Dispatched confirmation notice to: " + email);
        }
    }

    // SRP: Discount Calculator
    public static class DiscountPolicy {
        public BigDecimal applyDiscounts(BigDecimal amount) {
            if (amount.compareTo(new BigDecimal("100")) > 0) {
                return amount.multiply(new BigDecimal("0.90"));
            }
            return amount;
        }
    }

    // Cohesive Order Coordinator (Composition over Inheritance)
    public static class CleanOrderProcessor {
        private final PaymentGateway paymentGateway;
        private final NotificationService notificationService;
        private final DiscountPolicy discountPolicy;

        public CleanOrderProcessor(PaymentGateway paymentGateway,
                                   NotificationService notificationService,
                                   DiscountPolicy discountPolicy) {
            this.paymentGateway = paymentGateway;
            this.notificationService = notificationService;
            this.discountPolicy = discountPolicy;
        }

        public void processOrder(String orderId, String customerEmail, BigDecimal amount) {
            System.out.println("--- [CLEAN CODE] Executing Refactored Domain Flow ---");
            BigDecimal finalAmount = discountPolicy.applyDiscounts(amount);
            paymentGateway.charge(orderId, finalAmount);
            notificationService.sendOrderConfirmation(customerEmail, orderId);
            System.out.println("  [Order] Order " + orderId + " successfully coordinated!");
        }
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("        CLEAN CODE: BAD CODE VS REFACTORED SOLID CODE DEMO                     ");
        System.out.println("================================================================================\n");

        // Running Bad Code
        BadOrderProcessor bad = new BadOrderProcessor();
        bad.processOrder("ORD-901", "alice@example.com", "CREDIT_CARD", new BigDecimal("150.00"));

        System.out.println();

        // Running Refactored Clean Code
        PaymentGateway stripeGateway = new StripePaymentGateway();
        NotificationService emailService = new EmailNotificationService();
        DiscountPolicy discountPolicy = new DiscountPolicy();

        CleanOrderProcessor clean = new CleanOrderProcessor(stripeGateway, emailService, discountPolicy);
        clean.processOrder("ORD-901", "alice@example.com", new BigDecimal("150.00"));

        System.out.println("\nWHY IS THE REFACTORED CODE BETTER?");
        System.out.println("1. Testability: We can mock PaymentGateway and NotificationService easily in unit tests.");
        System.out.println("2. Extensibility (OCP): To support Apple Pay, we add ApplePayGateway without touching CleanOrderProcessor!");
        System.out.println("3. Maintainability: Email changes only affect EmailNotificationService.");
        System.out.println("================================================================================");
    }
}
