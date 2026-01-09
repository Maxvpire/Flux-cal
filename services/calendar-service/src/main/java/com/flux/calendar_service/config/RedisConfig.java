package com.flux.calendar_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    // Redis connection configuration
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6379);
        // Uncomment and configure for production
        // config.setPassword(RedisPassword.of("your-password"));
        return new LettuceConnectionFactory(config);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        // Configure serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Create customized ObjectMapper for JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        GenericJackson2JsonRedisSerializer valueSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        
        // Enable transaction support
        template.setEnableTransactionSupport(true);
        
        template.afterPropertiesSet();
        log.info("RedisTemplate configured successfully");
        return template;
    }

    @Bean
    public CacheManager cacheManager() {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(createObjectMapper())));

        // Create cache configurations for different cache names
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Event-related cache configurations
        cacheConfigurations.put("event", createCacheConfig(Duration.ofHours(2)));
        cacheConfigurations.put("events", createCacheConfig(Duration.ofMinutes(15)));
        cacheConfigurations.put("allEvents", createCacheConfig(Duration.ofMinutes(15)));
        cacheConfigurations.put("calendarEvents", createCacheConfig(Duration.ofMinutes(30)));
        cacheConfigurations.put("userEvents", createCacheConfig(Duration.ofMinutes(30)));
        cacheConfigurations.put("eventSearch", createCacheConfig(Duration.ofMinutes(10)));
        cacheConfigurations.put("bulkEvents", createCacheConfig(Duration.ofMinutes(5)));

        // Calendar-related cache configurations
        cacheConfigurations.put("calendar", createCacheConfig(Duration.ofHours(2)));
        cacheConfigurations.put("calendars", createCacheConfig(Duration.ofMinutes(15)));
        cacheConfigurations.put("allCalendars", createCacheConfig(Duration.ofMinutes(15)));
        cacheConfigurations.put("userCalendars", createCacheConfig(Duration.ofMinutes(30)));
        cacheConfigurations.put("userPrimaryCalendar", createCacheConfig(Duration.ofHours(1)));
        cacheConfigurations.put("calendarByTitle", createCacheConfig(Duration.ofMinutes(45)));
        cacheConfigurations.put("bulkCalendars", createCacheConfig(Duration.ofMinutes(10)));
        cacheConfigurations.put("calendarSearch", createCacheConfig(Duration.ofMinutes(5)));

        // Location-related cache configurations
        cacheConfigurations.put("location", createCacheConfig(Duration.ofHours(1)));
        cacheConfigurations.put("locations", createCacheConfig(Duration.ofMinutes(15)));
        cacheConfigurations.put("eventLocations", createCacheConfig(Duration.ofMinutes(30)));
        cacheConfigurations.put("locationSearch", createCacheConfig(Duration.ofMinutes(10)));
        cacheConfigurations.put("locationsByCity", createCacheConfig(Duration.ofMinutes(20)));
        cacheConfigurations.put("locationsByCountry", createCacheConfig(Duration.ofMinutes(20)));
        cacheConfigurations.put("nearbyLocations", createCacheConfig(Duration.ofMinutes(5)));

        // Conference-related cache configurations
        cacheConfigurations.put("conference", createCacheConfig(Duration.ofHours(1)));
        cacheConfigurations.put("conferences", createCacheConfig(Duration.ofMinutes(15)));

        // Build cache manager
        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory())
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("CacheManager configured with {} cache configurations", cacheConfigurations.size());
        return cacheManager;
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .withCacheConfiguration("shortLivedCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(30)))
                .withCacheConfiguration("mediumLivedCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("longLivedCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)));
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache get error for key: {} in cache: {}. Message: {}", 
                        key, cache.getName(), exception.getMessage());
                // Don't throw exception, just log it
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache put error for key: {} in cache: {}. Message: {}", 
                        key, cache.getName(), exception.getMessage());
                // Don't throw exception, just log it
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache evict error for key: {} in cache: {}. Message: {}", 
                        key, cache.getName(), exception.getMessage());
                // Don't throw exception, just log it
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Cache clear error for cache: {}. Message: {}", 
                        cache.getName(), exception.getMessage());
                // Don't throw exception, just log it
            }
        };
    }

    // Helper methods
    private RedisCacheConfiguration createCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(createObjectMapper())));
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    // Cache monitoring and statistics beans
    @Bean
    public CacheMetricsContributor cacheMetricsContributor() {
        return new CacheMetricsContributor();
    }
}

// Cache metrics contributor class
@Slf4j
class CacheMetricsContributor {
    
    public void logCacheStatistics() {
        // This could be enhanced to pull actual metrics from Redis
        log.info("Cache statistics logging enabled");
    }
}