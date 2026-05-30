package com.cache.model;

import java.time.Instant;

/**
 * Holds the cached value along with TTL and access metadata.
 */
public class CacheEntry<V> {

    private final V value;
    private final Instant expiresAt;
    private volatile long lastAccessedAt;

    public CacheEntry(V value, long ttlMillis) {
        this.value = value;
        this.expiresAt = ttlMillis > 0
                ? Instant.now().plusMillis(ttlMillis)
                : null;
        this.lastAccessedAt = System.currentTimeMillis();
    }

    public V getValue() {
        return value;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public long getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void touch() {
        this.lastAccessedAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "CacheEntry{value=" + value +
                ", expiresAt=" + expiresAt +
                ", lastAccessed=" + lastAccessedAt + "}";
    }
}
