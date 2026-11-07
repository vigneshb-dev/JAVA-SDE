package com.backendlearninglab.spring.repository;

import com.backendlearninglab.spring.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataProductRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findBySku(String sku);

    boolean existsBySku(String sku);

    List<ProductEntity> findByActiveTrue();

    List<ProductEntity> findByCategoryAndActiveTrue(String category);
}
