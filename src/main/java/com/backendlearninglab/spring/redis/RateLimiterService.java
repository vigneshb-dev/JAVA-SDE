package com.backendlearninglab.spring.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REDIS USE CASE: Distributed Rate Limiting.
 * <p>
 * TEACHING FOCUS: Why Redis is the Ideal Tool:
 * In a distributed architecture with 5 microservice replicas behind an AWS ALB,
 * an in-memory Java counter (e.g. AtomicInteger) only counts requests hitting THAT specific replica.
 * Redis provides a centralized, single-threaded atomic counter across ALL replicas!
 * <p>
 * ALGORITHM: Fixed Window Counter using Redis INCR + EXPIRE.
 * <p>
 * RESILIENCE PATTERN:
 * If Redis is unavailable or unconfigured, gracefully falls back to a node-local
 * {@link ConcurrentMap} so that throttling remains active without taking the service down.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentMap<String, WindowCounter> fallbackStore = new ConcurrentHashMap<>();

    private record WindowCounter(AtomicInteger count, long expiresAtMillis) {}

    public RateLimiterService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if a client identifier (e.g. IP or User ID) is allowed to execute a request.
     *
     * @param clientKey Unique identifier (e.g. "rate_limit:ip:192.168.1.1")
     * @param maxRequests Maximum requests allowed in the time window
     * @param window Duration of the rate limiting window
     * @return true if permitted, false if rate limit exceeded
     */
    public boolean isAllowed(String clientKey, int maxRequests, Duration window) {
        try {
            String redisKey = "rate_limit:" + clientKey;

            // Redis INCR is atomic!
            Long count = redisTemplate.opsForValue().increment(redisKey);

            if (count != null && count == 1) {
                // First request in this window -> Set the expiration TTL
                redisTemplate.expire(redisKey, window);
            }

            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded for client [{}]: {} requests (limit is {})", clientKey, count, maxRequests);
                return false;
            }
            return true;

        } catch (Exception e) {
            log.warn("Redis unavailable for rate limiting ({}); falling back to node-local memory.", e.getMessage());
            return fallbackRateLimit(clientKey, maxRequests, window);
        }
    }

    private boolean fallbackRateLimit(String clientKey, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();

        WindowCounter counter = fallbackStore.compute(clientKey, (k, existing) -> {
            if (existing == null || now > existing.expiresAtMillis) {
                return new WindowCounter(new AtomicInteger(1), now + windowMillis);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        boolean permitted = counter.count.get() <= maxRequests;
        if (!permitted) {
            log.warn("Local fallback rate limit exceeded for [{}]: {} requests", clientKey, counter.count.get());
        }
        return permitted;
    }
}
