package com.backendlearninglab.service;

import com.backendlearninglab.exception.DatabaseOperationException;
import com.backendlearninglab.exception.InsufficientStockException;
import com.backendlearninglab.exception.InvalidOrderException;
import com.backendlearninglab.exception.ProductNotFoundException;
import com.backendlearninglab.model.Order;
import com.backendlearninglab.model.OrderItem;
import com.backendlearninglab.model.Product;
import com.backendlearninglab.repository.OrderRepository;
import com.backendlearninglab.repository.ProductRepository;
import com.backendlearninglab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Service coordinating order checkout and stock reservation.
 * <p>
 * DE-MYSTIFYING SPRING'S @Transactional:
 * In Spring Boot, putting @Transactional on a method secretly creates a dynamic proxy
 * that intercepts calls, grabs a DB connection, disables autoCommit, executes the method,
 * commits, and catches RuntimeExceptions to trigger rollback.
 * <p>
 * Below is the EXACT manual implementation in Pure JDBC. Once you understand this,
 * Spring's transaction magic will never confuse you.
 */
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final DataSource dataSource;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderService(DataSource dataSource,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        OrderRepository orderRepository) {
        this.dataSource = dataSource;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Executes an atomic order checkout transaction.
     * <p>
     * GUARANTEES:
     * 1. Customer must exist and be ACTIVE.
     * 2. Products must exist and have adequate stock.
     * 3. Inventory is atomically decremented.
     * 4. Order header & items are persisted.
     * 5. If ANY step fails, ALL DB modifications roll back immediately.
     */
    public Order placeOrder(Long userId, String shippingAddress, List<OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new InvalidOrderException("Cannot checkout with an empty shopping cart.");
        }

        // Validate user existence prior to opening heavy DB transaction
        userRepository.findById(userId)
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .orElseThrow(() -> new InvalidOrderException("Cannot place order: User not found or inactive with ID: " + userId));

        // Generate human-friendly unique order reference
        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Order order = new Order(orderNumber, userId, shippingAddress);

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            // 1. BEGIN TRANSACTION: Disable auto-commit
            conn.setAutoCommit(false);
            log.info("Started transaction for order {}", orderNumber);

            // 2. Validate products, reserve stock, and build order line items
            for (OrderItemRequest req : itemRequests) {
                Product product = productRepository.findById(req.productId())
                        .orElseThrow(() -> new ProductNotFoundException(req.productId()));

                if (!product.isActive()) {
                    throw new InvalidOrderException("Product " + product.getName() + " is currently discontinued.");
                }

                // ATOMIC INVENTORY RESERVATION (runs inside same transaction)
                boolean stockReserved = productRepository.reserveStock(product.getId(), req.quantity(), conn);
                if (!stockReserved) {
                    throw new InsufficientStockException(product.getId(), req.quantity(), 0);
                }

                // Create line item preserving product's current catalog price
                OrderItem item = new OrderItem(product.getId(), req.quantity(), product.getPrice());
                order.addItem(item);
            }

            // 3. Persist Order Header (generates DB sequence ID)
            orderRepository.saveInTransaction(order, conn);

            // 4. Persist each Order Item with the parent Order ID
            for (OrderItem item : order.getItems()) {
                item.setOrderId(order.getId());
                orderRepository.saveItemInTransaction(item, conn);
            }

            // 5. COMMIT TRANSACTION: Permanently persist all changes
            conn.commit();
            log.info("Successfully placed order {} (ID: {}) with total {}", orderNumber, order.getId(), order.getTotalAmount());
            return order;

        } catch (SQLException | RuntimeException e) {
            // 6. ROLLBACK TRANSACTION on any exception
            log.error("Order processing failed for {}. Triggering transaction rollback!", orderNumber, e);
            if (conn != null) {
                try {
                    conn.rollback();
                    log.warn("Rollback completed successfully. No partial data persisted.");
                } catch (SQLException rollbackEx) {
                    log.error("Catastrophic error: Rollback failed!", rollbackEx);
                }
            }
            // Rethrow domain-specific exception so caller is notified
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new DatabaseOperationException("Failed to process order due to database error", e);
            }
        } finally {
            // 7. CLEANUP: Reset autoCommit and return connection to pool
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    log.warn("Failed to reset auto-commit / close connection", closeEx);
                }
            }
        }
    }

    /**
     * DTO (Data Transfer Object) for item checkout request.
     * Using Java Record for immutable data carrier.
     */
    public record OrderItemRequest(Long productId, int quantity) {
        public OrderItemRequest {
            if (productId == null || productId <= 0) {
                throw new IllegalArgumentException("productId must be positive");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be greater than zero");
            }
        }
    }
}
