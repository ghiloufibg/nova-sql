package com.novasql.cache;

import com.novasql.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class QueryCache {
    private static final Logger logger = LoggerFactory.getLogger(QueryCache.class);

    private final int maxSize;
    private final long ttlSeconds;
    private final Map<String, CacheEntry> cache;

    public QueryCache(int maxSize, long ttlSeconds) {
        this.maxSize = maxSize;
        this.ttlSeconds = ttlSeconds;

        // LRU cache implementation using LinkedHashMap
        this.cache = new LinkedHashMap<String, CacheEntry>(maxSize + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxSize;
            }
        };
    }

    public QueryResult get(String sql) {
        synchronized (cache) {
            CacheEntry entry = cache.get(sql);
            if (entry == null) {
                logger.debug("Cache miss for query: {}", sql);
                return null;
            }

            // Check TTL
            if (entry.isExpired()) {
                logger.debug("Cache entry expired for query: {}", sql);
                cache.remove(sql);
                return null;
            }

            logger.debug("Cache hit for query: {}", sql);
            entry.updateLastAccessed();
            return entry.getResult();
        }
    }

    public void put(String sql, QueryResult result) {
        // Only cache SELECT results
        if (result.getResultType() == QueryResult.ResultType.SELECT) {
            synchronized (cache) {
                cache.put(sql, new CacheEntry(result, ttlSeconds));
                logger.debug("Cached result for query: {}", sql);
            }
        }
    }

    public void invalidateTable(String tableName) {
        synchronized (cache) {
            // Remove all cached queries that reference this table
            cache.entrySet().removeIf(entry -> {
                String sql = entry.getKey().toUpperCase();
                return sql.contains("FROM " + tableName.toUpperCase()) ||
                       sql.contains("JOIN " + tableName.toUpperCase());
            });
            logger.debug("Invalidated cache entries for table: {}", tableName);
        }
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
            logger.debug("Cache cleared");
        }
    }

    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }

    public double getHitRate() {
        // This would need hit/miss counters in a real implementation
        return 0.0; // Simplified for demo
    }

    private static class CacheEntry {
        private final QueryResult result;
        private final Instant createdAt;
        private final long ttlSeconds;
        private Instant lastAccessed;

        public CacheEntry(QueryResult result, long ttlSeconds) {
            this.result = result;
            this.ttlSeconds = ttlSeconds;
            this.createdAt = Instant.now();
            this.lastAccessed = createdAt;
        }

        public QueryResult getResult() {
            return result;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(ttlSeconds, ChronoUnit.SECONDS));
        }

        public void updateLastAccessed() {
            this.lastAccessed = Instant.now();
        }
    }
}