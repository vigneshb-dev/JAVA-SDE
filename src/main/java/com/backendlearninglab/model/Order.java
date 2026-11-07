package com.backendlearninglab.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain Aggregate Root representing a customer order in OrderFlow.
 */
public class Order {

    private Long id;
    private String orderNumber;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private Instant createdAt;
    private Instant updatedAt;

    // Associated line items
    private final List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Order(Long id, String orderNumber, Long userId, OrderStatus status,
                 BigDecimal totalAmount, String shippingAddress, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        setUserId(userId);
        this.status = (status != null) ? status : OrderStatus.PENDING;
        this.totalAmount = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
        setShippingAddress(shippingAddress);
        this.createdAt = (createdAt != null) ? createdAt : Instant.now();
        this.updatedAt = (updatedAt != null) ? updatedAt : Instant.now();
    }

    public Order(String orderNumber, Long userId, String shippingAddress) {
        this(null, orderNumber, userId, OrderStatus.PENDING, BigDecimal.ZERO, shippingAddress, Instant.now(), Instant.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive non-null ID: " + userId);
        }
        this.userId = userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = (status != null) ? status : OrderStatus.PENDING;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Shipping address cannot be empty");
        }
        this.shippingAddress = shippingAddress.trim();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Defensive encapsulation: Return an unmodifiable view so external callers
     * cannot bypass order total recalculation or validation logic.
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add null item to order");
        }
        this.items.add(item);
        recalculateTotal();
    }

    public void addItems(List<OrderItem> newItems) {
        if (newItems != null) {
            this.items.addAll(newItems);
            recalculateTotal();
        }
    }

    public void recalculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem item : items) {
            if (item.getSubtotal() != null) {
                sum = sum.add(item.getSubtotal());
            }
        }
        this.totalAmount = sum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        if (id != null && order.id != null) {
            return Objects.equals(id, order.id);
        }
        return Objects.equals(orderNumber, order.orderNumber);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(orderNumber);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", userId=" + userId +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", itemsCount=" + items.size() +
                '}';
    }
}
