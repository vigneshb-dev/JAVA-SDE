package com.backendlearninglab.spring;

import com.backendlearninglab.spring.dto.request.CreateProductRequest;
import com.backendlearninglab.spring.dto.response.ProductResponse;
import com.backendlearninglab.spring.redis.RateLimiterService;
import com.backendlearninglab.spring.redis.TokenBlacklistService;
import com.backendlearninglab.spring.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Cache Abstraction & Redis Integration Tests.
 * <p>
 * TEACHING FOCUS:
 * <ul>
 *   <li>Verifies {@code @Cacheable} stores query results in the active Cache provider.</li>
 *   <li>Verifies {@code @CachePut} updates the cache synchronously with database mutations.</li>
 *   <li>Verifies {@code @CacheEvict} clears cache keys upon entity deactivation/deletion.</li>
 *   <li>Verifies {@link RateLimiterService} atomic request throttling (Fixed Window).</li>
 *   <li>Verifies {@link TokenBlacklistService} immediate JWT invalidation upon logout.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
class RedisCachingAndRateLimiterTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("Verify @Cacheable populates cache and @CachePut synchronizes cache on update")
    void testCacheLifecycle() {
        Cache productsCache = cacheManager.getCache("products");
        assertNotNull(productsCache, "The 'products' cache must be configured in CacheManager");

        // 1. Create a product in database
        String sku = "CACHE-TEST-" + System.currentTimeMillis();
        ProductResponse created = productService.createProduct(new CreateProductRequest(
                sku, "Mechanical Keyboard", "RGB Gaming Keyboard", new BigDecimal("129.99"), "ELECTRONICS", 50
        ));
        Long productId = created.id();

        // Ensure clean cache state for this ID
        productsCache.evict(productId);
        assertNull(productsCache.get(productId), "Cache should be empty before first lookup");

        // 2. First lookup: should trigger database fetch and populate cache via @Cacheable
        ProductResponse fetched1 = productService.getProductById(productId);
        assertNotNull(fetched1);
        assertEquals(new BigDecimal("129.99"), fetched1.price());

        // Verify cache entry now exists!
        Cache.ValueWrapper cachedEntry = productsCache.get(productId);
        assertNotNull(cachedEntry, "@Cacheable should have populated the cache");
        ProductResponse cachedProduct = (ProductResponse) cachedEntry.get();
        assertNotNull(cachedProduct);
        assertEquals(new BigDecimal("129.99"), cachedProduct.price());

        // 3. Update price: triggers @CachePut, writing new value into cache without an extra lookup
        BigDecimal newPrice = new BigDecimal("99.99");
        ProductResponse updated = productService.updatePrice(productId, newPrice);
        assertEquals(newPrice, updated.price());

        // Verify cache was updated immediately!
        Cache.ValueWrapper updatedCachedEntry = productsCache.get(productId);
        assertNotNull(updatedCachedEntry);
        ProductResponse updatedProductInCache = (ProductResponse) updatedCachedEntry.get();
        assertEquals(newPrice, updatedProductInCache.price(), "@CachePut must update the cached representation");

        // 4. Deactivate product: triggers @CacheEvict(allEntries = true)
        productService.deactivateProduct(productId);
        assertNull(productsCache.get(productId), "@CacheEvict must remove the key upon deactivation");
    }

    @Test
    @DisplayName("RateLimiterService - Atomic throttling allows under limit and blocks over limit")
    void testRateLimiterService() {
        String clientKey = "client-ip-192.168.1.100-" + System.currentTimeMillis();
        int maxAllowed = 3;
        Duration window = Duration.ofSeconds(5);

        // 1st request -> Allowed
        assertTrue(rateLimiterService.isAllowed(clientKey, maxAllowed, window));

        // 2nd request -> Allowed
        assertTrue(rateLimiterService.isAllowed(clientKey, maxAllowed, window));

        // 3rd request -> Allowed (limit reached)
        assertTrue(rateLimiterService.isAllowed(clientKey, maxAllowed, window));

        // 4th request -> Blocked! Throttling kicks in
        assertFalse(rateLimiterService.isAllowed(clientKey, maxAllowed, window));
    }

    @Test
    @DisplayName("TokenBlacklistService - Blacklists JWT token and validates revocation")
    void testTokenBlacklist() {
        String sampleToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyPayload.dummySignature";

        // Initially not blacklisted
        assertFalse(tokenBlacklistService.isTokenBlacklisted(sampleToken));

        // Blacklist token for 10 seconds (logout simulation)
        tokenBlacklistService.blacklistToken(sampleToken, Duration.ofSeconds(10));

        // Now blacklisted
        assertTrue(tokenBlacklistService.isTokenBlacklisted(sampleToken));

        // Other tokens are untouched
        assertFalse(tokenBlacklistService.isTokenBlacklisted("unrelated-active-token"));
    }
}
