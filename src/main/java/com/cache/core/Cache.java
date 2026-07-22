package com.cache.core;

import java.util.Optional;

/**
 * Contract for a thread-safe key-value cache with optional TTL.
 */
public interface Cache<K, V> {

    void put(K key, V value);

    void put(K key, V value, long ttlMillis);

    Optional<V> get(K key);

    void remove(K key);

    int size();

    void clear();

    int evictExpiredEntries();
}
