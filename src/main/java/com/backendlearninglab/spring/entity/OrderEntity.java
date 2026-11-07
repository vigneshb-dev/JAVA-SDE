package com.backendlearninglab.spring.entity;

import com.backendlearninglab.model.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JPA Aggregate Root Entity mapping the 'orders' table.
 * <p>
 * TEACHING FOCUS: @OneToMany & Association Lifecycle:
 * <ul>
 *   <li><b>mappedBy = "order"</b>: Marks this as the INVERSE side of the bidirectional relationship.
 *       The database table 'orders' does NOT have an item_id column. The 'order_items' table owns
 *       the foreign key!</li>
 *   <li><b>cascade = CascadeType.ALL</b>: Propagates PERSIST, MERGE, and REMOVE from Order to all
 *       its OrderItems. Saving the order automatically saves its items without calling itemRepo.save().</li>
 *   <li><b>orphanRemoval = true</b>: If an OrderItem is removed from the 'items' list in Java,
 *       Hibernate automatically issues a DELETE query for that row on transaction commit.</li>
 *   <li><b>FetchType.LAZY</b>: Line items are NOT fetched from database until explicitly accessed
 *       (e.g. order.getItems().size()). Prevents massive memory bloat on list queries.</li>
 * </ul>
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Bidirectional One-to-Many association
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemEntity> items = new ArrayList<>();

    public OrderEntity() {
    }

    public OrderEntity(String orderNumber, UserEntity user, String shippingAddress) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Helper synchronization method: Ensures both sides of the bidirectional
     * in-memory object relationship are kept in sync!
     */
    public void addItem(OrderItemEntity item) {
        if (item != null) {
            this.items.add(item);
            item.setOrder(this);
            recalculateTotal();
        }
    }

    public void removeItem(OrderItemEntity item) {
        if (item != null) {
            this.items.remove(item);
            item.setOrder(null);
            recalculateTotal();
        }
    }

    public void recalculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItemEntity item : items) {
            if (item.getSubtotal() != null) {
                sum = sum.add(item.getSubtotal());
            }
        }
        this.totalAmount = sum;
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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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
        this.shippingAddress = shippingAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItemEntity> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderEntity that = (OrderEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        // DO NOT print 'items' here: Invoking items in toString() triggers LazyInitializationException or recursion!
        return "OrderEntity{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
