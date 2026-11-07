package com.backendlearninglab.corejava.oop;

/**
 * CORE JAVA: Classes, Objects, Constructors, Encapsulation, Access Modifiers, Static, and Final.
 * <p>
 * RUNNABLE: Contains its own main() method for instant execution and observation.
 */
public class OopFundamentalsDemo {

    public static void main(String[] args) {
        System.out.println("=== OOP FUNDAMENTALS: ENCAPSULATION, STATIC, FINAL ===");

        // 1. Instantiation via constructors
        BankAccount account1 = new BankAccount("ACC-1001", 500.00);
        BankAccount account2 = new BankAccount("ACC-1002", 1200.00);

        // 2. Encapsulated operations (state is modified strictly through methods)
        account1.deposit(250.00);
        account1.withdraw(100.00);

        // 3. Static member access (belonging to class, shared across all instances)
        System.out.println("Total accounts created: " + BankAccount.getTotalAccountsCreated());
        System.out.println("Bank routing code (static final constant): " + BankAccount.ROUTING_CODE);

        // 4. Demonstrating defensive encapsulation preventing invalid state
        try {
            System.out.println("Attempting negative deposit...");
            account1.deposit(-50.00);
        } catch (IllegalArgumentException e) {
            System.out.println("Defensive encapsulation blocked invalid operation: " + e.getMessage());
        }
    }

    /**
     * BankAccount demonstrates:
     * - Encapsulation (private fields, validated accessors)
     * - Constructors with defensive validation
     * - static vs instance variables
     * - final fields and constants
     */
    public static class BankAccount {

        // WHAT: 'static final' is a compile-time constant shared globally across all instances.
        // WHY: Saves memory; no need to allocate a separate routing code string per account.
        public static final String ROUTING_CODE = "ROUT-998877";

        // WHAT: 'static' field shared across all instances.
        // WHY: Tracks global class-level state (e.g. how many accounts exist).
        private static int totalAccountsCreated = 0;

        // WHAT: 'final' instance field. Once assigned in the constructor, it CANNOT change.
        // WHY: Protects identity immutability. An account number should never mutate mid-lifecycle.
        private final String accountNumber;

        // WHAT: 'private' instance variable. Accessible ONLY inside this class.
        // WHY: Hides internal state so external code cannot corrupt the balance.
        private double balance;

        // CONSTRUCTOR:
        // WHAT: Initializes object state with mandatory arguments.
        // WHY: Guarantees that an object cannot exist in a half-initialized or invalid state.
        public BankAccount(String accountNumber, double initialDeposit) {
            if (accountNumber == null || accountNumber.isBlank()) {
                throw new IllegalArgumentException("Account number must be specified");
            }
            if (initialDeposit < 0) {
                throw new IllegalArgumentException("Initial balance cannot be negative");
            }

            this.accountNumber = accountNumber;
            this.balance = initialDeposit;
            totalAccountsCreated++;
        }

        // ENCAPSULATED METHOD:
        // WHAT: Modifies state while enforcing business rules (invariants).
        // WHY:
        // If 'balance' were public, any caller could write: account.balance = -999999;
        // With encapsulation, the method validates inputs and ensures balance integrity.
        public void deposit(double amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Deposit amount must be strictly positive: " + amount);
            }
            this.balance += amount;
            System.out.printf("  [DEPOSIT] %s deposited $%.2f. New Balance: $%.2f%n", accountNumber, amount, balance);
        }

        public void withdraw(double amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Withdrawal amount must be strictly positive");
            }
            if (amount > this.balance) {
                throw new IllegalStateException("Insufficient funds: balance is " + balance + ", attempted " + amount);
            }
            this.balance -= amount;
            System.out.printf("  [WITHDRAW] %s withdrew $%.2f. New Balance: $%.2f%n", accountNumber, amount, balance);
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public double getBalance() {
            return balance;
        }

        public static int getTotalAccountsCreated() {
            return totalAccountsCreated;
        }

        /*
         * WHAT WOULD HAPPEN WITH BAD CODE?
         * 
         * Bad Code Example:
         * public class BadBankAccount {
         *     public double balance; // Public mutable field!
         * }
         * 
         * Consequences:
         * 1. Anyone anywhere in the codebase can mutate 'balance' directly without audit or validation.
         * 2. Multithreaded race conditions happen without any locking or validation gate.
         * 3. Refactoring becomes impossible because 200 external classes directly read and write the field.
         */
    }
}
