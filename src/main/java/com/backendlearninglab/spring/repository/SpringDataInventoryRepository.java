package com.backendlearninglab.spring.repository;

import com.backendlearninglab.spring.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataInventoryRepository extends JpaRepository<InventoryEntity, Long> {

    /**
     * Atomic Stock Reservation Query.
     * <p>
     * WHY USE DIRECT JPQL UPDATE:
     * Prevents race conditions! Instead of fetching entity into Java memory, modifying it,
     * and saving it (which is vulnerable to Lost Updates under concurrency),
     * this executes an atomic SQL UPDATE at the database row lock level:
     *   UPDATE inventory SET available_stock = available_stock - ? WHERE product_id = ? AND available_stock >= ?
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE InventoryEntity i " +
           "SET i.availableStock = i.availableStock - :qty, " +
           "    i.reservedStock = i.reservedStock + :qty " +
           "WHERE i.productId = :productId AND i.availableStock >= :qty")
    int reserveStockAtomic(@Param("productId") Long productId, @Param("qty") int qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE InventoryEntity i " +
           "SET i.availableStock = i.availableStock + :qty, " +
           "    i.reservedStock = i.reservedStock - :qty " +
           "WHERE i.productId = :productId AND i.reservedStock >= :qty")
    int restoreStockAtomic(@Param("productId") Long productId, @Param("qty") int qty);
}
