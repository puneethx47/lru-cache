package com.cache;

import com.cache.core.CacheManager;
import com.cache.core.LRUCache;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Runnable demo showing all cache features.
 */
public class CacheDemo {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== LRU Cache with TTL — Demo ===\n");

        // Demo 1: Basic LRU eviction
        System.out.println("--- Demo 1: LRU Eviction (capacity=3) ---");
        LRUCache<String, String> cache = new LRUCache<>(3, 5000);
        cache.put("a", "Apple");
        cache.put("b", "Banana");
        cache.put("c", "Cherry");
        cache.get("a");
        cache.put("d", "Date");
        System.out.println("  a => " + cache.get("a"));
        System.out.println("  b => " + cache.get("b")); // evicted
        System.out.println("  c => " + cache.get("c"));
        System.out.println("  d => " + cache.get("d"));
        System.out.println("  Size: " + cache.size());
        cache.shutdown();

        // Demo 2: TTL expiry
        System.out.println("\n--- Demo 2: TTL Expiry ---");
        LRUCache<String, String> ttlCache = new LRUCache<>(100, 1000);
        ttlCache.put("session:user1", "token-abc", 2000);
        ttlCache.put("session:user2", "token-xyz");
        System.out.println("Immediately:");
        System.out.println("  user1 => " + ttlCache.get("session:user1"));
        System.out.println("  user2 => " + ttlCache.get("session:user2"));
        Thread.sleep(2500);
        System.out.println("After 2.5s:");
        System.out.println("  user1 => " + ttlCache.get("session:user1")); // expired
        System.out.println("  user2 => " + ttlCache.get("session:user2"));
        ttlCache.shutdown();

        // Demo 3: Concurrent access
        System.out.println("\n--- Demo 3: Concurrent Read/Write (10 threads) ---");
        LRUCache<Integer, String> concurrentCache = new LRUCache<>(50, 2000);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 5; i++) {
            final int writerId = i;
            executor.submit(() -> {
                for (int j = 0; j < 20; j++) {
                    concurrentCache.put(writerId * 20 + j, "value-" + (writerId * 20 + j), 5000);
                }
                System.out.println("  Writer-" + writerId + " done");
            });
        }

        for (int i = 0; i < 5; i++) {
            final int readerId = i;
            executor.submit(() -> {
                int hits = 0;
                for (int j = 0; j < 100; j++) {
                    Optional<String> val = concurrentCache.get(j % 50);
                    if (val.isPresent()) hits++;
                }
                System.out.println("  Reader-" + readerId + " hits: " + hits + "/100");
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("  Final cache size: " + concurrentCache.size());
        concurrentCache.shutdown();

        // Demo 4: CacheManager
        System.out.println("\n--- Demo 4: CacheManager ---");
        CacheManager manager = new CacheManager();
        LRUCache<String, String> userCache    = manager.createCache("users",    1000, 60_000);
        LRUCache<String, String> sessionCache = manager.createCache("sessions", 500,  30_000);
        userCache.put("user:42", "Puneeth R", 60_000);
        sessionCache.put("sess:abc", "user:42", 30 * 60_000);
        System.out.println("  user:42  => " + manager.<String, String>getCache("users").get("user:42"));
        System.out.println("  sess:abc => " + manager.<String, String>getCache("sessions").get("sess:abc"));
        manager.shutdownAll();

        System.out.println("\n=== Demo complete ===");
    }
}
