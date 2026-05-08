package com.docvalidator.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache for MCP responses with TTL support.
 * Reduces redundant calls to MCP servers and improves performance.
 */
@Slf4j
@Component
public class McpCache {
    
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Duration defaultTtl = Duration.ofMinutes(5);
    
    /**
     * Get a cached value if it exists and hasn't expired.
     */
    public <T> T get(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);
        
        if (entry == null) {
            log.debug("Cache miss for key: {}", key);
            return null;
        }
        
        if (entry.isExpired()) {
            log.debug("Cache entry expired for key: {}", key);
            cache.remove(key);
            return null;
        }
        
        log.debug("Cache hit for key: {}", key);
        try {
            return type.cast(entry.getValue());
        } catch (ClassCastException e) {
            log.warn("Cache type mismatch for key: {}", key, e);
            cache.remove(key);
            return null;
        }
    }
    
    /**
     * Put a value in the cache with default TTL.
     */
    public void put(String key, Object value) {
        put(key, value, defaultTtl);
    }
    
    /**
     * Put a value in the cache with custom TTL.
     */
    public void put(String key, Object value, Duration ttl) {
        if (key == null || value == null) {
            return;
        }
        
        CacheEntry entry = new CacheEntry(value, Instant.now().plus(ttl));
        cache.put(key, entry);
        log.debug("Cached value for key: {} with TTL: {}", key, ttl);
    }
    
    /**
     * Invalidate a specific cache entry.
     */
    public void invalidate(String key) {
        cache.remove(key);
        log.debug("Invalidated cache for key: {}", key);
    }
    
    /**
     * Invalidate all cache entries matching a pattern.
     */
    public void invalidatePattern(String pattern) {
        cache.keySet().stream()
            .filter(key -> key.matches(pattern))
            .forEach(this::invalidate);
        log.debug("Invalidated cache entries matching pattern: {}", pattern);
    }
    
    /**
     * Clear all cache entries.
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        log.info("Cleared {} cache entries", size);
    }
    
    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        long totalEntries = cache.size();
        long expiredEntries = cache.values().stream()
            .filter(CacheEntry::isExpired)
            .count();
        
        return new CacheStats(totalEntries, expiredEntries);
    }
    
    /**
     * Clean up expired entries.
     */
    public void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debug("Cleaned up expired cache entries");
    }
    
    /**
     * Cache entry with expiration time.
     */
    private static class CacheEntry {
        private final Object value;
        private final Instant expiresAt;
        
        public CacheEntry(Object value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
        
        public Object getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
    
    /**
     * Cache statistics.
     */
    public record CacheStats(long totalEntries, long expiredEntries) {
        public long activeEntries() {
            return totalEntries - expiredEntries;
        }
    }
}

// Made with Bob
