package com.cache.core;

import com.cache.model.CacheEntry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private final ScheduledExecutorService evictionScheduler;

    public LRUCache(int capacity, long evictionIntervalMs) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be > 0");

        this.capacity = capacity;

        this.store = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > capacity;
            }
        };

        this.evictionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-eviction-thread");
            t.setDaemon(true);
            return t;
        });

        evictionScheduler.scheduleAtFixedRate(
                this::evictExpiredEntries,
                evictionIntervalMs,
                evictionIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    public LRUCache(int capacity) {
        this(capacity, 30_000);
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
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = store.get(key);
            if (entry == null) return Optional.empty();

            if (entry.isExpired()) {
                lock.readLock().unlock();
                return removeExpiredAndReturn(key);
            }

            entry.touch();
            return Optional.of(entry.getValue());

        } finally {
            try { lock.readLock().unlock(); } catch (IllegalMonitorStateException ignored) {}
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
    public void shutdown() {
        evictionScheduler.shutdown();
        try {
            if (!evictionScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                evictionScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            evictionScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void evictExpiredEntries() {
        lock.writeLock().lock();
        try {
            store.entrySet().removeIf(e -> e.getValue().isExpired());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Optional<V> removeExpiredAndReturn(K key) {
        lock.writeLock().lock();
        try {
            CacheEntry<V> entry = store.get(key);
            if (entry != null && entry.isExpired()) {
                store.remove(key);
            }
            return Optional.empty();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
