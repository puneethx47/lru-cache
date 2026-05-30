package com.cache.dto;

public class LoadTestResult {

    private String cacheName;
    private int totalRequests;
    private int reads;
    private int writes;
    private long hits;
    private long misses;
    private long durationMs;
    private double throughputOps;
    private double avgLatencyMs;
    private long errors;

    public LoadTestResult() {}

    public double hitRatio() {
        return reads == 0 ? 0.0 : (double) hits / reads;
    }

    public String getCacheName()               { return cacheName; }
    public void setCacheName(String v)         { this.cacheName = v; }

    public int getTotalRequests()              { return totalRequests; }
    public void setTotalRequests(int v)        { this.totalRequests = v; }

    public int getReads()                      { return reads; }
    public void setReads(int v)                { this.reads = v; }

    public int getWrites()                     { return writes; }
    public void setWrites(int v)               { this.writes = v; }

    public long getHits()                      { return hits; }
    public void setHits(long v)                { this.hits = v; }

    public long getMisses()                    { return misses; }
    public void setMisses(long v)              { this.misses = v; }

    public long getDurationMs()                { return durationMs; }
    public void setDurationMs(long v)          { this.durationMs = v; }

    public double getThroughputOps()           { return throughputOps; }
    public void setThroughputOps(double v)     { this.throughputOps = v; }

    public double getAvgLatencyMs()            { return avgLatencyMs; }
    public void setAvgLatencyMs(double v)      { this.avgLatencyMs = v; }

    public long getErrors()                    { return errors; }
    public void setErrors(long v)              { this.errors = v; }

    public double getHitRatio()                { return hitRatio(); }

    @Override
    public String toString() {
        return String.format(
                "LoadTestResult{cache='%s', total=%d, reads=%d, writes=%d, " +
                        "hits=%d, misses=%d, hitRatio=%.2f, duration=%dms, " +
                        "throughput=%.1f ops/s, avgLatency=%.3fms, errors=%d}",
                cacheName, totalRequests, reads, writes,
                hits, misses, hitRatio(), durationMs,
                throughputOps, avgLatencyMs, errors
        );
    }
}