package com.backendlearninglab.spring.redis;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Master Redis & Spring Cache Abstraction Configuration.
 * <p>
 * TEACHING FOCUS:
 * <ul>
 *   <li><b>@EnableCaching</b>: Triggers Spring's CacheInterceptor proxy to intercept methods
 *       annotated with @Cacheable, @CachePut, and @CacheEvict.</li>
 *   <li><b>Serialization</b>: Keys are serialized as readable strings (StringRedisSerializer),
 *       and values are serialized as clean JSON (GenericJackson2JsonRedisSerializer) instead of
 *       opaque Java bytecode serialization.</li>
 *   <li><b>Default TTL</b>: Enforces 10-minute expiration to prevent indefinite stale data leaks.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisConfiguration implements CachingConfigurer {

    private final ResilientCacheErrorHandler resilientCacheErrorHandler;

    public RedisConfiguration(ResilientCacheErrorHandler resilientCacheErrorHandler) {
        this.resilientCacheErrorHandler = resilientCacheErrorHandler;
    }

    /**
     * Intercepts and suppresses Redis connection failures so DB queries succeed gracefully.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return resilientCacheErrorHandler;
    }

    /**
     * Configures general-purpose RedisTemplate for custom Redis commands (Rate Limiting, Blacklists).
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serialization for Keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // JSON serialization for Values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configures RedisCacheManager with a 10-minute default TTL.
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 10 minute Time-To-Live
                .disableCachingNullValues()       // Prevent caching null values
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
