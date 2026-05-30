package com.cache.core;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for managing multiple named caches.
 */
public class CacheManager {

    private final ConcurrentHashMap<String, LRUCache<?, ?>> caches = new ConcurrentHashMap<>();

    public <K, V> LRUCache<K, V> createCache(String name, int capacity, long evictionIntervalMs) {
        LRUCache<K, V> cache = new LRUCache<>(capacity, evictionIntervalMs);
        caches.put(name, cache);
        return cache;
    }

    @SuppressWarnings("unchecked")
    public <K, V> LRUCache<K, V> getCache(String name) {
        LRUCache<?, ?> cache = caches.get(name);
        if (cache == null) throw new IllegalArgumentException("No cache registered with name: " + name);
        return (LRUCache<K, V>) cache;
    }

    public void shutdownAll() {
        caches.values().forEach(LRUCache::shutdown);
        caches.clear();
    }
}
