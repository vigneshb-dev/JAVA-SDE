package com.backendlearninglab.spring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity mapping the 'inventory' table.
 * <p>
 * TEACHING FOCUS: Assigned Primary Keys in Spring Data JPA & Persistable<ID>.
 * When an entity's primary key is assigned manually (productId) instead of using
 * an auto-increment generator (@GeneratedValue), Spring Data's save() checks 'getId() != null'.
 * Because it is NOT null, Spring Data mistakenly treats the new entity as detached and calls
 * entityManager.merge() instead of entityManager.persist().
 * <p>
 * Implementing {@link Persistable} allows us to return isNew = true on new instances,
 * directing Spring Data to call persist() and preventing unexpected merge failures!
 */
@Entity
@Table(name = "inventory")
public class InventoryEntity implements Persistable<Long> {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private ProductEntity product;

    @Column(name = "available_stock", nullable = false)
    private int availableStock = 0;

    @Column(name = "reserved_stock", nullable = false)
    private int reservedStock = 0;

    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation = "MAIN-WH-01";

    @Column(name = "last_restocked_at")
    private Instant lastRestockedAt = Instant.now();

    @Transient
    private boolean isNew = true;

    public InventoryEntity() {
    }

    public InventoryEntity(Long productId, int availableStock, int reservedStock, String warehouseLocation) {
        this.productId = productId;
        this.availableStock = availableStock;
        this.reservedStock = reservedStock;
        this.warehouseLocation = warehouseLocation;
        this.lastRestockedAt = Instant.now();
        this.isNew = true;
    }

    public InventoryEntity(ProductEntity product, int availableStock, int reservedStock, String warehouseLocation) {
        this(product != null ? product.getId() : null, availableStock, reservedStock, warehouseLocation);
        this.product = product;
    }

    @Override
    public Long getId() {
        return productId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
        }
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }

    public int getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(int reservedStock) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryEntity that = (InventoryEntity) o;
        return productId != null && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "InventoryEntity{" +
                "productId=" + productId +
                ", availableStock=" + availableStock +
                ", reservedStock=" + reservedStock +
                ", warehouseLocation='" + warehouseLocation + '\'' +
                '}';
    }
}
