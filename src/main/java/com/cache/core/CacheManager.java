package com.cache.core;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Factory for managing multiple named caches.
 */
public class CacheManager {

    private final ConcurrentHashMap<String, LRUCache<?, ?>> caches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService evictionScheduler;

    public CacheManager(ScheduledExecutorService evictionScheduler, long evictionIntervalMs) {
        this.evictionScheduler = evictionScheduler;
        evictionScheduler.scheduleWithFixedDelay(this::sweepAll,
                evictionIntervalMs, evictionIntervalMs, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    public <K, V> LRUCache<K, V> getOrCreate(String name, int capacity) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Cache name must not be blank");
        return (LRUCache<K, V>) caches.computeIfAbsent(name, ignored -> new LRUCache<>(capacity));
    }

    public <K, V> LRUCache<K, V> createCache(String name, int capacity, long ignoredLegacyIntervalMs) {
        return getOrCreate(name, capacity);
    }

    @SuppressWarnings("unchecked")
    public <K, V> LRUCache<K, V> getCache(String name) {
        LRUCache<?, ?> cache = caches.get(name);
        if (cache == null) throw new IllegalArgumentException("No cache registered with name: " + name);
        return (LRUCache<K, V>) cache;
    }

    public boolean remove(String name) { return caches.remove(name) != null; }

    public Map<String, LRUCache<?, ?>> getCaches() { return Map.copyOf(caches); }

    private void sweepAll() {
        caches.values().forEach(cache -> {
            try { cache.evictExpiredEntries(); }
            catch (RuntimeException ignored) { /* one cache must not stop future sweeps */ }
        });
    }

    @PreDestroy
    public void shutdownAll() {
        evictionScheduler.shutdownNow();
        caches.clear();
    }
}
