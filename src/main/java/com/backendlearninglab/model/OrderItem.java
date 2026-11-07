package com.backendlearninglab.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Line item in an order.
 * <p>
 * NOTICE: It stores unitPrice and subtotal explicitly!
 * Why not just look up Product.getPrice()?
 * Because product prices change over time! An order item must permanently capture
 * the exact price paid at the historical moment the order was placed.
 */
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long productId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public OrderItem() {
    }

    public OrderItem(Long id, Long orderId, Long productId, int quantity, BigDecimal unitPrice) {
        this.id = id;
        this.orderId = orderId;
        setProductId(productId);
        setQuantity(quantity);
        setUnitPrice(unitPrice);
        recalculateSubtotal();
    }

    public OrderItem(Long productId, int quantity, BigDecimal unitPrice) {
        this(null, null, productId, quantity, unitPrice);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid productId: " + productId);
        }
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero: " + quantity);
        }
        this.quantity = quantity;
        if (this.unitPrice != null) {
            recalculateSubtotal();
        }
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price must be non-negative: " + unitPrice);
        }
        this.unitPrice = unitPrice;
        if (this.quantity > 0) {
            recalculateSubtotal();
        }
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    private void recalculateSubtotal() {
        this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        if (id != null && orderItem.id != null) {
            return Objects.equals(id, orderItem.id);
        }
        return Objects.equals(orderId, orderItem.orderId) &&
               Objects.equals(productId, orderItem.productId);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(orderId, productId);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                '}';
    }
}
