package com.cache.service;

import com.cache.core.CacheManager;
import com.cache.core.LRUCache;
import com.cache.dto.LoadTestRequest;
import com.cache.dto.LoadTestResult;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.LockSupport;

@Service
public class LoadTestService {
    private final CacheManager cacheManager;
    private final ConcurrentMap<String, TestState> tests = new ConcurrentHashMap<>();
    private final ExecutorService coordinator = Executors.newCachedThreadPool(r -> daemon(r, "load-test-coordinator"));

    public LoadTestService(CacheManager cacheManager) { this.cacheManager = cacheManager; }

    public LoadTestResult start(String cacheName, LoadTestRequest request) {
        request.validate();
        LRUCache<String, String> cache = cacheManager.getCache(cacheName);
        String id = UUID.randomUUID().toString();
        TestState state = new TestState(id, cacheName);
        tests.put(id, state);
        coordinator.submit(() -> execute(state, cache, request));
        return snapshot(state);
    }

    public LoadTestResult get(String cacheName, String testId) {
        TestState state = tests.get(testId);
        if (state == null || !state.cacheName.equals(cacheName)) throw new NoSuchElementException("Load test not found: " + testId);
        return snapshot(state);
    }

    private void execute(TestState state, LRUCache<String, String> cache, LoadTestRequest req) {
        state.status = "RUNNING";
        state.startedNanos = System.nanoTime();
        long deadline = state.startedNanos + TimeUnit.SECONDS.toNanos(req.getDurationSeconds());
        ExecutorService workers = Executors.newFixedThreadPool(req.getThreadCount(), r -> daemon(r, "load-test-worker"));
        ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "load-test-sampler"));
        AtomicLong nextPermit = new AtomicLong(state.startedNanos);
        long interval = req.getOpsPerSecond() == null ? 0 : TimeUnit.SECONDS.toNanos(1) / req.getOpsPerSecond();
        String value = sizedValue(req.getValueSizeBytes());
        sampler.scheduleAtFixedRate(() -> state.sample(), 1, 1, TimeUnit.SECONDS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < req.getThreadCount(); i++) {
                futures.add(workers.submit(() -> runWorker(state, cache, req, deadline, interval, nextPermit, value)));
            }
            for (Future<?> future : futures) future.get();
            state.status = "COMPLETED";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            state.status = "FAILED";
            state.errorMessage = "Load test interrupted";
        } catch (ExecutionException e) {
            state.status = "FAILED";
            state.errorMessage = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
        } finally {
            state.finishedNanos = System.nanoTime();
            state.sample();
            workers.shutdownNow();
            sampler.shutdownNow();
        }
    }

    private void runWorker(TestState s, LRUCache<String, String> cache, LoadTestRequest req,
            long deadline, long interval, AtomicLong nextPermit, String value) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (System.nanoTime() < deadline) {
            if (interval > 0) {
                long permit = nextPermit.getAndAdd(interval);
                long wait = permit - System.nanoTime();
                if (wait > 0) LockSupport.parkNanos(wait);
                if (System.nanoTime() >= deadline) break;
            }
            String key = "key-" + random.nextInt(req.getKeySpaceSize());
            boolean read = random.nextDouble() < req.getReadWriteRatio();
            long start = System.nanoTime();
            try {
                if (read) {
                    if (cache.get(key).isPresent()) s.hits.increment(); else s.misses.increment();
                    s.reads.increment();
                } else {
                    cache.put(key, value);
                    s.writes.increment();
                }
            } catch (RuntimeException e) {
                s.errors.increment();
            } finally {
                s.latencies.add(System.nanoTime() - start);
                s.operations.increment();
            }
        }
    }

    private LoadTestResult snapshot(TestState s) {
        long now = s.finishedNanos == 0 ? System.nanoTime() : s.finishedNanos;
        long elapsedNs = s.startedNanos == 0 ? 0 : Math.max(1, now - s.startedNanos);
        long[] values = s.latencies.stream().mapToLong(Long::longValue).sorted().toArray();
        return new LoadTestResult(s.id, s.cacheName, s.status, TimeUnit.NANOSECONDS.toMillis(elapsedNs),
                s.operations.sum(), s.reads.sum(), s.writes.sum(), s.hits.sum(), s.misses.sum(), s.errors.sum(),
                elapsedNs == 0 ? 0 : s.operations.sum() * 1_000_000_000d / elapsedNs,
                percentile(values, .50), percentile(values, .95), percentile(values, .99),
                List.copyOf(s.points), s.errorMessage);
    }

    private static double percentile(long[] sorted, double percentile) {
        if (sorted.length == 0) return 0;
        int index = Math.min(sorted.length - 1, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index] / 1_000_000d;
    }

    private static String sizedValue(int bytes) {
        if (bytes == 0) return "";
        byte[] data = new byte[bytes];
        Arrays.fill(data, (byte) 'x');
        return new String(data, StandardCharsets.US_ASCII);
    }

    private static Thread daemon(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }

    @PreDestroy public void shutdown() { coordinator.shutdownNow(); }

    private static final class TestState {
        final String id;
        final String cacheName;
        volatile String status = "QUEUED";
        volatile String errorMessage;
        volatile long startedNanos;
        volatile long finishedNanos;
        final LongAdder operations = new LongAdder(), reads = new LongAdder(), writes = new LongAdder();
        final LongAdder hits = new LongAdder(), misses = new LongAdder(), errors = new LongAdder();
        final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        final CopyOnWriteArrayList<LoadTestResult.ThroughputPoint> points = new CopyOnWriteArrayList<>();
        final AtomicLong lastSampleOps = new AtomicLong();
        TestState(String id, String cacheName) { this.id = id; this.cacheName = cacheName; }
        void sample() {
            if (startedNanos == 0) return;
            long seconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedNanos));
            long current = operations.sum();
            long previous = lastSampleOps.getAndSet(current);
            points.add(new LoadTestResult.ThroughputPoint(seconds, current - previous));
        }
    }
}
