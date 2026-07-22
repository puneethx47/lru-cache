package com.cache.dto;

import java.util.List;

public record LoadTestResult(
        String testId, String cacheName, String status, long elapsedMs,
        long totalOperations, long reads, long writes, long hits, long misses,
        long errors, double throughputOpsPerSecond, double p50LatencyMs,
        double p95LatencyMs, double p99LatencyMs, List<ThroughputPoint> throughputOverTime,
        String errorMessage) {
    public record ThroughputPoint(long elapsedSeconds, double operationsPerSecond) {}
}
