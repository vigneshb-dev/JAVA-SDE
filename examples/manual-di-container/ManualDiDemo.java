package com.backendlearninglab.examples.di;

/**
 * Runnable Demonstration of the Manual Dependency Injection Container.
 */
public class ManualDiDemo {

    public static void main(String[] args) {
        System.out.println("=== EDUCATIONAL EXERCISE: MANUAL DEPENDENCY INJECTION CONTAINER ===");

        // 1. Initialize our mini IoC container with component classes
        MiniApplicationContext context = new MiniApplicationContext(
                DatabaseConnector.class,
                OrderProcessor.class,
                NotificationService.class
        );

        // 2. Request the top-level bean from the container
        OrderProcessor processor = context.getBean(OrderProcessor.class);

        // 3. Execute work to verify dependencies were injected and functional
        processor.processOrder("ORD-MANUAL-001", 149.99);

        // 4. Verify singleton scope (same reference returned)
        DatabaseConnector db1 = context.getBean(DatabaseConnector.class);
        DatabaseConnector db2 = context.getBean(DatabaseConnector.class);
        System.out.println("Singleton verification: db1 == db2 -> " + (db1 == db2) + " (True = single instance reused)");
    }

    // -------------------------------------------------------------------------
    // TEST COMPONENTS MANAGED BY THE CONTAINER
    // -------------------------------------------------------------------------

    @MiniComponent
    public static class DatabaseConnector {
        public void executeInsert(String sql) {
            System.out.println("  [DB] Executing SQL: " + sql);
        }
    }

    @MiniComponent
    public static class NotificationService {
        public void sendNotification(String message) {
            System.out.println("  [NOTIFY] Alert: " + message);
        }
    }

    @MiniComponent
    public static class OrderProcessor {

        // Constructor Injection
        private final DatabaseConnector databaseConnector;

        // Field Injection
        @MiniInject
        private NotificationService notificationService;

        @MiniInject
        public OrderProcessor(DatabaseConnector databaseConnector) {
            this.databaseConnector = databaseConnector;
        }

        public void processOrder(String orderNumber, double amount) {
            System.out.println("OrderProcessor processing: " + orderNumber + " ($" + amount + ")");
            databaseConnector.executeInsert("INSERT INTO orders VALUES ('" + orderNumber + "', " + amount + ")");
            notificationService.sendNotification("Order " + orderNumber + " successfully processed!");
        }
    }
}
