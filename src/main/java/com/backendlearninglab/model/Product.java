package com.backendlearninglab.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a product in the catalog.
 * <p>
 * CRITICAL LESSON: MONEY REPRESENTATION
 * Always use {@link BigDecimal} for prices in Java backends, NEVER double or float!
 * Why? IEEE 754 binary floating-point representation cannot precisely represent decimal fractions
 * like 0.1 or 0.05. Over millions of transactions, rounding errors accumulate into real financial losses.
 */
public class Product {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public Product() {
    }

    public Product(Long id, String sku, String name, String description, BigDecimal price,
                   String category, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        setSku(sku);
        setName(name);
        this.description = description;
        setPrice(price);
        setCategory(category);
        this.active = active;
        this.createdAt = (createdAt != null) ? createdAt : Instant.now();
        this.updatedAt = (updatedAt != null) ? updatedAt : Instant.now();
    }

    public Product(String sku, String name, String description, BigDecimal price, String category) {
        this(null, sku, name, description, price, category, true, Instant.now(), Instant.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("Product SKU cannot be blank");
        }
        this.sku = sku.trim().toUpperCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }
        this.name = name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price must be non-null and non-negative: " + price);
        }
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Product category cannot be blank");
        }
        this.category = category.trim().toUpperCase();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        if (id != null && product.id != null) {
            return Objects.equals(id, product.id);
        }
        return Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(sku);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", active=" + active +
                '}';
    }
}
