package com.cache.config;

import com.cache.core.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class CacheConfig {

    @Bean
    public ScheduledExecutorService cacheEvictionScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "cache-eviction-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    public CacheManager cacheManager(ScheduledExecutorService cacheEvictionScheduler,
            @Value("${cache.eviction.interval-ms:30000}") long intervalMs) {
        CacheManager manager = new CacheManager(cacheEvictionScheduler, intervalMs);
        manager.createCache("users",    1000, 60_000);
        manager.createCache("sessions", 500,  30_000);
        return manager;
    }
}
