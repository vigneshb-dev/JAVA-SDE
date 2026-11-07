package com.backendlearninglab.spring.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

/**
 * Resilient Cache Error Handler for Graceful Degradation.
 * <p>
 * TEACHING FOCUS: Cache Failure vs. Database Failure:
 * <ul>
 *   <li><b>Database Failure:</b> Critical! If PostgreSQL is down, write transactions CANNOT proceed. The request must fail with 500.</li>
 *   <li><b>Cache Failure:</b> Non-Critical! A cache (Redis) is an optimization layer, NOT the source of truth.
 *       If Redis crashes or network partitions occur, the application should <b>gracefully degrade</b>
 *       by falling back directly to PostgreSQL, logging a warning rather than crashing user requests with 500!</li>
 * </ul>
 */
@Component
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ResilientCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache GET failed for key [{}] in cache [{}]. Degrading gracefully to database: {}",
                key, cache.getName(), exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis cache PUT failed for key [{}] in cache [{}]. Continuing without caching: {}",
                key, cache.getName(), exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache EVICT failed for key [{}] in cache [{}]: {}",
                key, cache.getName(), exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis cache CLEAR failed for cache [{}]: {}", cache.getName(), exception.getMessage());
    }
}
