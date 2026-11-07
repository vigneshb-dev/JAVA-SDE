package com.backendlearninglab.spring.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * REDIS USE CASE: Stateless JWT Token Revocation (Blacklisting).
 * <p>
 * TEACHING FOCUS: The JWT Invalidation Dilemma:
 * <ul>
 *   <li>By design, JWTs are stateless and cannot be revoked before their expiration time without storing state.</li>
 *   <li>If a user clicks "Logout" or changes their password, how do you revoke their active JWT?</li>
 *   <li><b>THE REDIS SOLUTION:</b> Store the revoked token signature in Redis with a TTL equal to the
 *       remaining lifespan of the token. Once the token reaches its natural expiration date,
 *       Redis automatically purges the blacklist key via its TTL engine! Memory never leaks.</li>
 * </ul>
 * <p>
 * RESILIENCE PATTERN:
 * If Redis is offline during local testing or partitions, gracefully uses a node-local
 * {@link ConcurrentMap} with timestamp expiration.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentMap<String, Long> fallbackBlacklist = new ConcurrentHashMap<>();

    public TokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Blacklists an active JWT token until its expiration date.
     */
    public void blacklistToken(String token, Duration remainingValidity) {
        try {
            String redisKey = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(redisKey, "REVOKED", remainingValidity);
            log.info("Token successfully blacklisted in Redis for duration: {}s", remainingValidity.getSeconds());
        } catch (Exception e) {
            log.warn("Redis unavailable ({}); blacklisting token in node-local fallback.", e.getMessage());
            fallbackBlacklist.put(token, System.currentTimeMillis() + remainingValidity.toMillis());
        }
    }

    /**
     * Checks if a token has been explicitly revoked prior to its natural expiration.
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String redisKey = BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            Long expiresAt = fallbackBlacklist.get(token);
            if (expiresAt != null) {
                if (System.currentTimeMillis() < expiresAt) {
                    return true;
                } else {
                    fallbackBlacklist.remove(token); // TTL expired
                }
            }
            return false;
        }
    }
}
