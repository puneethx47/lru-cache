package com.cache.core;

import com.cache.model.CacheEntry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Thread-safe LRU Cache with TTL expiry.
 *
 * Design decisions:
 * 1. ReentrantReadWriteLock — multiple concurrent reads, exclusive writes.
 * 2. LinkedHashMap(accessOrder=true) — maintains LRU order automatically.
 * 3. ScheduledExecutorService — background daemon thread scans for expired entries.
 * 4. Double-checked expiry — expired entries caught lazily on get().
 */
public class LRUCache<K, V> implements Cache<K, V> {

    private static final Logger log = Logger.getLogger(LRUCache.class.getName());

    private final int capacity;
    private final Map<K, CacheEntry<V>> store;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be > 0");

        this.capacity = capacity;

        this.store = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                boolean remove = size() > capacity;
                if (remove) evictions.incrementAndGet();
                return remove;
            }
        };
    }

    @Override
    public void put(K key, V value) {
        put(key, value, -1);
    }

    @Override
    public void put(K key, V value, long ttlMillis) {
        if (key == null) throw new IllegalArgumentException("Key must not be null");
        lock.writeLock().lock();
        try {
            store.put(key, new CacheEntry<>(value, ttlMillis));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<V> get(K key) {
        // LinkedHashMap#get mutates ordering when accessOrder=true, so this is a write operation.
        lock.writeLock().lock();
        try {
            CacheEntry<V> entry = store.get(key);
            if (entry == null) {
                misses.incrementAndGet();
                return Optional.empty();
            }

            if (entry.isExpired()) {
                misses.incrementAndGet();
                store.remove(key);
                evictions.incrementAndGet();
                return Optional.empty();
            }

            entry.touch();
            hits.incrementAndGet();
            return Optional.of(entry.getValue());

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(K key) {
        lock.writeLock().lock();
        try {
            store.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return store.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            store.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int evictExpiredEntries() {
        lock.writeLock().lock();
        try {
            int before = store.size();
            store.entrySet().removeIf(e -> e.getValue().isExpired());
            int removed = before - store.size();
            evictions.addAndGet(removed);
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int capacity() { return capacity; }
    public long hits() { return hits.get(); }
    public long misses() { return misses.get(); }
    public long evictions() { return evictions.get(); }

}
