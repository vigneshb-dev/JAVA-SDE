package com.backendlearninglab.repository;

import com.backendlearninglab.model.Inventory;
import com.backendlearninglab.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Products and their inventory levels.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    Optional<Product> findBySku(String sku);

    List<Product> findAllActive();

    List<Product> findByCategory(String category);

    boolean updatePrice(Long id, BigDecimal newPrice);

    // Inventory operations (can participate in active transactions via external Connection)
    void saveInventory(Inventory inventory);

    Optional<Inventory> findInventoryByProductId(Long productId);

    boolean reserveStock(Long productId, int quantity, Connection conn);

    boolean restoreStock(Long productId, int quantity, Connection conn);
}
