package com.peakconnect.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache Configuration that wires up a Two-Level Cache:
 * - Caffeine (Local/L1): Avoids a network hop to Redis for extremely hot, recently-computed values within the same instance.
 * - Redis (Distributed/L2): Keeps the cache consistent and shared across multiple app instances.
 */
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.annotation.JsonTypeInfo

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val objectMapper = ObjectMapper().apply {
            registerModule(kotlinModule())
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            activateDefaultTyping(polymorphicTypeValidator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
        }
        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))

        val priceRedisConfig = defaultConfig.entryTtl(Duration.ofMinutes(5))
        val riskRedisConfig = defaultConfig.entryTtl(Duration.ofMinutes(15))
        val guideRedisConfig = defaultConfig.entryTtl(Duration.ofMinutes(2))
        
        val redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("priceCache", priceRedisConfig)
            .withCacheConfiguration("riskCache", riskRedisConfig)
            .withCacheConfiguration("guideAvailabilityCache", guideRedisConfig)
            .build()
            
        // Combine with Caffeine L1
        return TwoLevelCacheManager(redisCacheManager)
    }
}

class TwoLevelCacheManager(private val redisCacheManager: RedisCacheManager) : CacheManager {
    private val caches = ConcurrentHashMap<String, Cache>()

    override fun getCache(name: String): Cache? {
        return caches.getOrPut(name) {
            val redisCache = redisCacheManager.getCache(name) ?: return null
            // 30 seconds local TTL for all caches as per requirement
            val caffeineCache = org.springframework.cache.caffeine.CaffeineCache(
                name,
                Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).maximumSize(1000).build()
            )
            TwoLevelCache(name, caffeineCache, redisCache)
        }
    }

    override fun getCacheNames(): Collection<String> {
        return redisCacheManager.cacheNames
    }
}

class TwoLevelCache(
    private val cacheName: String,
    private val localCache: Cache,
    private val remoteCache: Cache
) : Cache {

    override fun getName() = cacheName
    override fun getNativeCache() = this

    override fun get(key: Any): Cache.ValueWrapper? {
        // 1. Try L1 (Caffeine)
        val localValue = localCache.get(key)
        if (localValue != null) return localValue

        // 2. Try L2 (Redis)
        val remoteValue = remoteCache.get(key)
        if (remoteValue != null) {
            // Populate L1
            localCache.put(key, remoteValue.get())
            return remoteValue
        }
        return null
    }

    override fun <T : Any?> get(key: Any, type: Class<T>?): T? {
        val valueWrapper = get(key)
        @Suppress("UNCHECKED_CAST")
        return valueWrapper?.get() as? T
    }

    override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? {
        var value = get(key)?.get()
        if (value == null) {
            value = valueLoader.call()
            if (value != null) put(key, value)
        }
        @Suppress("UNCHECKED_CAST")
        return value as? T
    }

    override fun put(key: Any, value: Any?) {
        localCache.put(key, value)
        remoteCache.put(key, value)
    }

    override fun putIfAbsent(key: Any, value: Any?): Cache.ValueWrapper? {
        val remoteValue = remoteCache.putIfAbsent(key, value)
        if (remoteValue == null) {
             localCache.put(key, value)
        }
        return remoteValue
    }

    override fun evict(key: Any) {
        localCache.evict(key)
        remoteCache.evict(key)
    }

    override fun clear() {
        localCache.clear()
        remoteCache.clear()
    }
}
