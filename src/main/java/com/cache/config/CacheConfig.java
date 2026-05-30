package com.cache.config;

import com.cache.core.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CacheManager manager = new CacheManager();
        manager.createCache("users",    1000, 60_000);
        manager.createCache("sessions", 500,  30_000);
        return manager;
    }
}
