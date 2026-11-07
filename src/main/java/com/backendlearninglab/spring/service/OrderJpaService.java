package com.backendlearninglab.spring.service;

import com.backendlearninglab.exception.InsufficientStockException;
import com.backendlearninglab.exception.InvalidOrderException;
import com.backendlearninglab.model.OrderStatus;
import com.backendlearninglab.spring.dto.request.CreateOrderRequest;
import com.backendlearninglab.spring.dto.response.OrderResponse;
import com.backendlearninglab.spring.entity.OrderEntity;
import com.backendlearninglab.spring.entity.OrderItemEntity;
import com.backendlearninglab.spring.entity.OutboxEventEntity;
import com.backendlearninglab.spring.entity.ProductEntity;
import com.backendlearninglab.spring.entity.UserEntity;
import com.backendlearninglab.spring.exception.ResourceNotFoundException;
import com.backendlearninglab.spring.kafka.event.OrderCreatedEvent;
import com.backendlearninglab.spring.repository.SpringDataInventoryRepository;
import com.backendlearninglab.spring.repository.SpringDataOrderRepository;
import com.backendlearninglab.spring.repository.SpringDataOutboxRepository;
import com.backendlearninglab.spring.repository.SpringDataProductRepository;
import com.backendlearninglab.spring.repository.SpringDataUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service Layer for Order Checkout & Inventory Coordination.
 * <p>
 * TEACHING FOCUS: Spring's @Transactional & CGLIB Dynamic Proxies
 * <p>
 * <b>WHAT SPRING IS DOING FOR YOU:</b>
 * In Phase 1, you had to manually call:
 * <pre>
 *   conn.setAutoCommit(false);
 *   try { ... conn.commit(); }
 *   catch (Exception e) { conn.rollback(); }
 *   finally { conn.close(); }
 * </pre>
 * In Phase 2, putting {@code @Transactional} on this method tells Spring's TransactionAspect
 * to intercept calls using a CGLIB proxy, borrow a connection from HikariCP, bind it to a
 * ThreadLocal via TransactionSynchronizationManager, begin the transaction, commit on normal exit,
 * and roll back on any {@link RuntimeException}!
 * <p>
 * <b>THE CRITICAL "SELF-INVOCATION" PITFALL:</b>
 * If method A() calls method B() within the SAME class, and B() has {@code @Transactional},
 * B()'s transaction IS COMPLETELY IGNORED!
 * Why? Because calling a method directly in the same class uses the internal {@code this} reference,
 * bypassing Spring's outer CGLIB dynamic proxy!
 */
@Service
@Transactional(readOnly = true)
public class OrderJpaService {

    private static final Logger log = LoggerFactory.getLogger(OrderJpaService.class);

    private final SpringDataOrderRepository orderRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataProductRepository productRepository;
    private final SpringDataInventoryRepository inventoryRepository;
    private final SpringDataOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderJpaService(SpringDataOrderRepository orderRepository,
                           SpringDataUserRepository userRepository,
                           SpringDataProductRepository productRepository,
                           SpringDataInventoryRepository inventoryRepository,
                           SpringDataOutboxRepository outboxRepository,
                           ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes an Atomic Order Checkout Transaction.
     * <ol>
     *   <li>Validates customer exists.</li>
     *   <li>Validates product catalog prices and availability.</li>
     *   <li>Decreases inventory atomically at database lock level.</li>
     *   <li>Saves Order and line items using CascadeType.ALL.</li>
     *   <li>Commits transaction.</li>
     * </ol>
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public OrderResponse placeOrder(CreateOrderRequest request) {
        log.info("Starting @Transactional order placement for user ID: {}", request.userId());

        UserEntity user = userRepository.findById(request.userId())
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .orElseThrow(() -> new InvalidOrderException("User not found or inactive with ID: " + request.userId()));

        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        OrderEntity order = new OrderEntity(orderNumber, user, request.shippingAddress());

        // Process each line item
        for (CreateOrderRequest.OrderItemRequestDto itemReq : request.items()) {
            ProductEntity product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.productId()));

            if (!product.isActive()) {
                throw new InvalidOrderException("Cannot order deactivated product: " + product.getName());
            }

            // ATOMIC INVENTORY DECREMENT (SQL row update)
            int rowsUpdated = inventoryRepository.reserveStockAtomic(product.getId(), itemReq.quantity());
            if (rowsUpdated == 0) {
                // Throws unchecked exception -> Spring CGLIB proxy intercepts and triggers ROLLBACK!
                throw new InsufficientStockException(product.getId(), itemReq.quantity(), 0);
            }

            // Create line item entity (cascade = CascadeType.ALL will persist this automatically!)
            OrderItemEntity orderItem = new OrderItemEntity(order, product, itemReq.quantity(), product.getPrice());
            order.addItem(orderItem);
        }

        // Save order root aggregate (Cascades automatically persist all line items!)
        OrderEntity savedOrder = orderRepository.save(order);
        log.info("Transaction committed successfully for order {}. Total: ${}", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());

        // TRANSACTIONAL OUTBOX: Persist event in the SAME database transaction!
        try {
            List<OrderCreatedEvent.OrderItemPayload> eventItems = savedOrder.getItems().stream()
                    .map(i -> new OrderCreatedEvent.OrderItemPayload(i.getProduct().getId(), i.getQuantity(), i.getUnitPrice()))
                    .toList();
            OrderCreatedEvent event = OrderCreatedEvent.of(
                    savedOrder.getId(),
                    savedOrder.getOrderNumber(),
                    user.getId(),
                    user.getEmail(),
                    savedOrder.getTotalAmount(),
                    eventItems
            );
            String payloadJson = objectMapper.writeValueAsString(event);
            OutboxEventEntity outboxEvent = new OutboxEventEntity(
                    "ORDER",
                    savedOrder.getOrderNumber(),
                    "OrderCreatedEvent",
                    payloadJson
            );
            outboxRepository.save(outboxEvent);
            log.info("Saved OrderCreatedEvent to Transactional Outbox [Order #{}]", savedOrder.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to serialize Outbox event: {}", e.getMessage(), e);
            throw new RuntimeException("Outbox serialization failure", e);
        }

        return OrderResponse.fromEntity(savedOrder);
    }

    /**
     * EDUCATIONAL EXPERIMENT: Deliberately Failing Transaction.
     * Decrements inventory and creates an order, then intentionally throws an exception
     * to prove that Spring's @Transactional completely rolls back all modifications!
     */
    @Transactional(rollbackFor = Exception.class)
    public void placeOrderWithDeliberateFailure(CreateOrderRequest request) {
        log.info("Executing placeOrderWithDeliberateFailure: Attempting order placement...");

        // Step 1: Normal processing
        placeOrder(request);

        // Step 2: Deliberately throw exception right before method exits
        log.warn("Throwing deliberate RuntimeException to verify @Transactional rollback!");
        throw new IllegalStateException("Deliberate failure: Simulating payment gateway crash after order creation!");
    }

    public OrderResponse getOrderById(Long id) {
        // Uses JOIN FETCH to prevent N+1 query problem!
        return orderRepository.findByIdWithItemsAndUserJoinFetch(id)
                .map(OrderResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    /**
     * Demonstrates Dirty Checking on Status Update.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // DIRTY CHECKING: No save() call needed!
        order.setStatus(newStatus);
        return OrderResponse.fromEntity(order);
    }
}
