package com.cache.service;

import com.cache.core.CacheManager;
import com.cache.core.LRUCache;
import com.cache.dto.LoadTestRequest;
import com.cache.dto.LoadTestResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LoadTestService {

    private final CacheManager cacheManager;

    public LoadTestService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public LoadTestResult run(LoadTestRequest req) {
        LRUCache<String, String> cache = cacheManager.getCache(req.getCacheName());

        int totalOps     = req.getNumRequests();
        int threads      = req.getNumThreads();
        int opsPerThread = totalOps / threads;
        int remainder    = totalOps % threads;

        AtomicLong hits          = new AtomicLong();
        AtomicLong misses        = new AtomicLong();
        AtomicLong errors        = new AtomicLong();
        AtomicLong latencyNsTotal = new AtomicLong();
        AtomicLong readCount     = new AtomicLong();
        AtomicLong writeCount    = new AtomicLong();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures  = new ArrayList<>(threads);

        long wallStart = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            int ops      = opsPerThread + (t == threads - 1 ? remainder : 0);
            int threadId = t;

            futures.add(executor.submit(() -> {
                for (int i = 0; i < ops; i++) {
                    boolean isRead = (i % 100) < (int)(req.getReadRatio() * 100);
                    long opStart = System.nanoTime();
                    try {
                        if (isRead) {
                            String key = "t" + threadId + "-k" + i;
                            Optional<String> val = cache.get(key);
                            if (val.isPresent()) hits.incrementAndGet();
                            else                 misses.incrementAndGet();
                            readCount.incrementAndGet();
                        } else {
                            String key   = "t" + threadId + "-k" + i;
                            String value = "value-" + i;
                            if (req.getTtlMillis() > 0) cache.put(key, value, req.getTtlMillis());
                            else                        cache.put(key, value);
                            writeCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        latencyNsTotal.addAndGet(System.nanoTime() - opStart);
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); }
            catch (InterruptedException | ExecutionException e) { errors.incrementAndGet(); }
        }

        executor.shutdown();

        long durationMs = Math.max(1, System.currentTimeMillis() - wallStart);

        LoadTestResult result = new LoadTestResult();
        result.setCacheName(req.getCacheName());
        result.setTotalRequests(totalOps);
        result.setReads((int) readCount.get());
        result.setWrites((int) writeCount.get());
        result.setHits(hits.get());
        result.setMisses(misses.get());
        result.setErrors(errors.get());
        result.setDurationMs(durationMs);
        result.setThroughputOps((double) totalOps / durationMs * 1000.0);
        result.setAvgLatencyMs(
            totalOps == 0 ? 0.0 : (latencyNsTotal.get() / (double) totalOps) / 1_000_000.0
        );

        return result;
    }
}
