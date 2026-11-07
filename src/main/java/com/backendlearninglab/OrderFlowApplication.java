package com.backendlearninglab;

import com.backendlearninglab.model.Inventory;
import com.backendlearninglab.model.Order;
import com.backendlearninglab.model.Product;
import com.backendlearninglab.model.User;
import com.backendlearninglab.model.UserRole;
import com.backendlearninglab.repository.JdbcOrderRepository;
import com.backendlearninglab.repository.JdbcProductRepository;
import com.backendlearninglab.repository.JdbcUserRepository;
import com.backendlearninglab.repository.OrderRepository;
import com.backendlearninglab.repository.ProductRepository;
import com.backendlearninglab.repository.UserRepository;
import com.backendlearninglab.service.OrderService;
import com.backendlearninglab.util.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * OrderFlow — Main Application Entry Point.
 * <p>
 * Demonstrates a complete realistic backend order processing workflow using:
 * - Core Java 21+
 * - Pure JDBC (No Spring Magic)
 * - PostgreSQL / H2 In-Memory DataSource
 * - Transaction Management (ACID Commit &amp; Rollback)
 */
public class OrderFlowApplication {

    private static final Logger log = LoggerFactory.getLogger(OrderFlowApplication.class);

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   OrderFlow — E-Commerce Order Processing & Inventory System  ");
        System.out.println("   Backend Learning Lab (Phase 1: Java + Maven + JDBC + SQL)   ");
        System.out.println("===============================================================\n");

        DataSource dataSource = ConnectionManager.getDataSource();

        // 1. Ensure database schema exists (creates tables in H2 fallback if PostgreSQL not running)
        initializeDatabaseSchema(dataSource);

        // 2. Initialize Pure JDBC Repositories
        UserRepository userRepository = new JdbcUserRepository(dataSource);
        ProductRepository productRepository = new JdbcProductRepository(dataSource);
        OrderRepository orderRepository = new JdbcOrderRepository(dataSource);

        // 3. Initialize Business Service
        OrderService orderService = new OrderService(dataSource, userRepository, productRepository, orderRepository);

        try {
            System.out.println("--- [STEP 1] PROVISIONING USERS & PRODUCTS ---");
            User customer = new User("alice.smith@example.com", "secure_hash_pwd", "Alice Smith", UserRole.CUSTOMER);
            customer = userRepository.save(customer);
            System.out.println("Saved User: " + customer);

            Product keyboard = new Product("TECH-KEY-01", "Mechanical Keyboard RGB", "Tactile switches", new BigDecimal("129.99"), "ELECTRONICS");
            keyboard = productRepository.save(keyboard);
            productRepository.saveInventory(new Inventory(keyboard.getId(), 10, 0, "WH-A1", null));

            Product mouse = new Product("TECH-MOU-02", "Wireless Optical Mouse", "16000 DPI", new BigDecimal("49.99"), "ELECTRONICS");
            mouse = productRepository.save(mouse);
            productRepository.saveInventory(new Inventory(mouse.getId(), 25, 0, "WH-A2", null));

            System.out.println("Saved Product 1: " + keyboard.getName() + " ($" + keyboard.getPrice() + ")");
            System.out.println("Saved Product 2: " + mouse.getName() + " ($" + mouse.getPrice() + ")");

            System.out.println("\n--- [STEP 2] ATOMIC CHECKOUT TRANSACTION (SUCCESS PATH) ---");
            List<OrderService.OrderItemRequest> itemsToBuy = List.of(
                    new OrderService.OrderItemRequest(keyboard.getId(), 2),
                    new OrderService.OrderItemRequest(mouse.getId(), 1)
            );

            Order completedOrder = orderService.placeOrder(customer.getId(), "742 Evergreen Terrace, Springfield", itemsToBuy);
            System.out.println("Transaction Committed! Order Placed: " + completedOrder.getOrderNumber());
            System.out.println("Total Amount: $" + completedOrder.getTotalAmount());
            System.out.println("Line Items Count: " + completedOrder.getItems().size());

            // Verify inventory updated
            productRepository.findInventoryByProductId(keyboard.getId()).ifPresent(inv ->
                    System.out.println("Remaining available keyboard stock: " + inv.getAvailableStock() + " (Reserved: " + inv.getReservedStock() + ")"));

            System.out.println("\n--- [STEP 3] ATOMIC TRANSACTION ROLLBACK (FAILURE PATH) ---");
            System.out.println("Attempting to purchase 100 keyboards (Stock is only 8)...");
            try {
                orderService.placeOrder(customer.getId(), "742 Evergreen Terrace",
                        List.of(new OrderService.OrderItemRequest(keyboard.getId(), 100)));
            } catch (Exception e) {
                System.out.println("Expected Transaction Rollback: " + e.getMessage());
            }

            // Verify inventory was NOT deducted during the failed checkout
            productRepository.findInventoryByProductId(keyboard.getId()).ifPresent(inv ->
                    System.out.println("Verified Keyboard Stock after Rollback: " + inv.getAvailableStock() + " (State preserved!)"));

            System.out.println("\n===============================================================");
            System.out.println("  PHASE 1 CORE WORKFLOW COMPLETED SUCCESSFULLY!");
            System.out.println("  Explore the individual learning modules in:");
            System.out.println("    - com.backendlearninglab.corejava.*");
            System.out.println("    - com.backendlearninglab.concurrency.*");
            System.out.println("    - com.backendlearninglab.jdbc.demo.*");
            System.out.println("===============================================================");

        } catch (Exception e) {
            log.error("Application execution failed", e);
        }
    }

    private static void initializeDatabaseSchema(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(150) NOT NULL, " +
                    "role VARCHAR(30) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "updated_at TIMESTAMP NOT NULL)");

            // Create products table
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                    "sku VARCHAR(64) NOT NULL UNIQUE, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "description TEXT, " +
                    "price NUMERIC(12, 2) NOT NULL, " +
                    "category VARCHAR(100) NOT NULL, " +
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "updated_at TIMESTAMP NOT NULL)");

            // Create inventory table
            stmt.execute("CREATE TABLE IF NOT EXISTS inventory (" +
                    "product_id BIGINT PRIMARY KEY, " +
                    "available_stock INT NOT NULL DEFAULT 0, " +
                    "reserved_stock INT NOT NULL DEFAULT 0, " +
                    "warehouse_location VARCHAR(100), " +
                    "last_restocked_at TIMESTAMP)");

            // Create orders table
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                    "order_number VARCHAR(64) NOT NULL UNIQUE, " +
                    "user_id BIGINT NOT NULL, " +
                    "status VARCHAR(30) NOT NULL, " +
                    "total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00, " +
                    "shipping_address TEXT NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "updated_at TIMESTAMP NOT NULL)");

            // Create order_items table
            stmt.execute("CREATE TABLE IF NOT EXISTS order_items (" +
                    "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                    "order_id BIGINT NOT NULL, " +
                    "product_id BIGINT NOT NULL, " +
                    "quantity INT NOT NULL, " +
                    "unit_price NUMERIC(12, 2) NOT NULL, " +
                    "subtotal NUMERIC(12, 2) NOT NULL)");

            log.info("Database schema initialized and verified.");

        } catch (SQLException e) {
            log.error("Failed to initialize database schema", e);
        }
    }
}
