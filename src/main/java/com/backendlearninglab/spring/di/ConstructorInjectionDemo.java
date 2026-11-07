package com.backendlearninglab.spring.di;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * EDUCATIONAL COMPARISON: Field Injection vs. Constructor Injection.
 * <p>
 * This class documents why professional Java backend engineering strictly mandates
 * Constructor Injection over Field Injection.
 */
public class ConstructorInjectionDemo {

    // =========================================================================
    // 1. THE FIELD INJECTION ANTI-PATTERN (DO NOT DO THIS)
    // =========================================================================
    @Component
    public static class BadFieldInjectionOrderService {

        // WHY THIS IS AN ANTI-PATTERN:
        // 1. VIOLATES IMMUTABILITY: The field cannot be marked 'final'.
        // 2. HIDDEN DEPENDENCIES: A caller doing 'new BadFieldInjectionOrderService()'
        //    gets a compiled class where paymentGateway is null, causing instant NPE at runtime!
        // 3. UNTESTABLE WITHOUT SPRING: You cannot write a simple JUnit test without using
        //    heavy reflection (ReflectionTestUtils) or launching the full slow Spring container.
        // 4. MASKS CIRCULAR DEPENDENCIES: Two classes with field injection can circularly inject
        //    each other without failing at construction time, masking poor architecture.
        @Autowired
        private PaymentGateway paymentGateway;

        public void checkout(double amount) {
            paymentGateway.charge(amount);
        }
    }

    // =========================================================================
    // 2. THE PROFESSIONAL CONSTRUCTOR INJECTION PATTERN (RECOMMENDED)
    // =========================================================================
    @Component
    public static class GoodConstructorInjectionOrderService {

        // WHY THIS IS SUPERIOR:
        // 1. IMMUTABILITY: Field is 'final'. Once set in the constructor, it can never mutate.
        // 2. IMPOSSIBLE TO INSTANTIATE IN INVALID STATE: The Java compiler forces you to pass
        //    the required dependency.
        // 3. PURE UNIT TESTING: In a unit test, you can simply do:
        //      PaymentGateway mockGateway = mock(PaymentGateway.class);
        //      GoodConstructorInjectionOrderService service = new GoodConstructorInjectionOrderService(mockGateway);
        //    It executes in 0.001 seconds without needing any Spring test runner!
        // 4. DETECTS ARCHITECTURAL SMELLS: If a class has 8 constructor arguments, you visually
        //    realize it violates the Single Responsibility Principle and needs refactoring.
        // 5. NO @Autowired NEEDED: Since Spring 4.3, classes with a single constructor do not
        //    even need the @Autowired annotation!
        private final PaymentGateway paymentGateway;

        public GoodConstructorInjectionOrderService(PaymentGateway paymentGateway) {
            if (paymentGateway == null) {
                throw new IllegalArgumentException("paymentGateway must not be null");
            }
            this.paymentGateway = paymentGateway;
        }

        public void checkout(double amount) {
            paymentGateway.charge(amount);
        }
    }

    @Component
    public static class PaymentGateway {
        public void charge(double amount) {
            System.out.println("Charging $" + amount + " through gateway.");
        }
    }
}
