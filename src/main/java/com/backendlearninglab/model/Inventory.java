package com.backendlearninglab.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity tracking stock levels for a product in OrderFlow warehouse.
 */
public class Inventory {

    private Long productId;
    private int availableStock;
    private int reservedStock;
    private String warehouseLocation;
    private Instant lastRestockedAt;

    public Inventory() {
    }

    public Inventory(Long productId, int availableStock, int reservedStock,
                     String warehouseLocation, Instant lastRestockedAt) {
        setProductId(productId);
        setAvailableStock(availableStock);
        setReservedStock(reservedStock);
        this.warehouseLocation = (warehouseLocation != null) ? warehouseLocation : "MAIN-WH-01";
        this.lastRestockedAt = (lastRestockedAt != null) ? lastRestockedAt : Instant.now();
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product ID must be a valid positive integer");
        }
        this.productId = productId;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        if (availableStock < 0) {
            throw new IllegalArgumentException("Available stock cannot be negative: " + availableStock);
        }
        this.availableStock = availableStock;
    }

    public int getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(int reservedStock) {
        if (reservedStock < 0) {
            throw new IllegalArgumentException("Reserved stock cannot be negative: " + reservedStock);
        }
        this.reservedStock = reservedStock;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public Instant getLastRestockedAt() {
        return lastRestockedAt;
    }

    public void setLastRestockedAt(Instant lastRestockedAt) {
        this.lastRestockedAt = lastRestockedAt;
    }

    /**
     * Total physical stock on premises = available for sale + reserved for in-flight orders.
     */
    public int getTotalPhysicalStock() {
        return availableStock + reservedStock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(productId, inventory.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "productId=" + productId +
                ", availableStock=" + availableStock +
                ", reservedStock=" + reservedStock +
                ", warehouseLocation='" + warehouseLocation + '\'' +
                ", totalPhysicalStock=" + getTotalPhysicalStock() +
                '}';
    }
}
