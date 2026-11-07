package com.backendlearninglab.spring.service;

import com.backendlearninglab.spring.dto.request.CreateProductRequest;
import com.backendlearninglab.spring.dto.response.ProductResponse;
import com.backendlearninglab.spring.entity.InventoryEntity;
import com.backendlearninglab.spring.entity.ProductEntity;
import com.backendlearninglab.spring.exception.ResourceNotFoundException;
import com.backendlearninglab.spring.repository.SpringDataInventoryRepository;
import com.backendlearninglab.spring.repository.SpringDataProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final SpringDataProductRepository productRepository;
    private final SpringDataInventoryRepository inventoryRepository;

    public ProductService(SpringDataProductRepository productRepository,
                          SpringDataInventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DataIntegrityViolationException("Product SKU already exists: " + request.sku());
        }

        ProductEntity product = new ProductEntity(
                request.sku(),
                request.name(),
                request.description(),
                request.price(),
                request.category()
        );

        ProductEntity savedProduct = productRepository.saveAndFlush(product);

        // Automatically initialize inventory record
        int initialStock = Math.max(0, request.initialStock());
        InventoryEntity inventory = new InventoryEntity(savedProduct.getId(), initialStock, 0, "MAIN-WH-01");
        inventoryRepository.save(inventory);

        return ProductResponse.fromEntity(savedProduct);
    }

    /**
     * @Cacheable(value = "products", key = "#id")
     * <p>
     * THE CACHE-ASIDE PATTERN:
     * 1. Spring CacheInterceptor checks if key "products::#id" exists in Redis.
     * 2. CACHE HIT: Returns deserialized ProductResponse immediately without touching PostgreSQL!
     * 3. CACHE MISS: Executes this method (hits DB), puts result into Redis with TTL, then returns.
     */
    @org.springframework.cache.annotation.Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @org.springframework.cache.annotation.Cacheable(value = "productListings", key = "'ALL'")
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @org.springframework.cache.annotation.Cacheable(value = "productListings", key = "#category")
    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category.toUpperCase()).stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    /**
     * @CachePut(value = "products", key = "#id")
     * <p>
     * Updates PostgreSQL AND immediately refreshes the Redis cache entry with the new price,
     * ensuring subsequent reads see the updated price without waiting for TTL expiration!
     */
    @Transactional
    @org.springframework.cache.annotation.CachePut(value = "products", key = "#id")
    @org.springframework.cache.annotation.CacheEvict(value = "productListings", allEntries = true)
    public ProductResponse updatePrice(Long id, BigDecimal newPrice) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        // Dirty checking: setting price in managed entity triggers UPDATE on commit
        product.setPrice(newPrice);
        return ProductResponse.fromEntity(product);
    }

    /**
     * @CacheEvict(value = {"products", "productListings"}, allEntries = true)
     * <p>
     * Evicts stale cache entries so users never see deleted or deactivated merchandise.
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"products", "productListings"}, allEntries = true)
    public void deactivateProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(false);
    }
}
