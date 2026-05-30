package com.cache.dto;

public class LoadTestRequest {

    private String cacheName;
    private int numThreads;
    private int numRequests;
    private double readRatio;
    private long ttlMillis;

    public LoadTestRequest() {}

    public LoadTestRequest(String cacheName, int numThreads, int numRequests,
                           double readRatio, long ttlMillis) {
        this.cacheName   = cacheName;
        this.numThreads  = numThreads;
        this.numRequests = numRequests;
        this.readRatio   = readRatio;
        this.ttlMillis   = ttlMillis;
    }

    public static LoadTestRequest defaults(String cacheName) {
        return new LoadTestRequest(cacheName, 1, 1000, 1.0, 0);
    }

    public String getCacheName()            { return cacheName; }
    public void setCacheName(String v)      { this.cacheName = v; }

    public int getNumThreads()              { return numThreads; }
    public void setNumThreads(int v)        { this.numThreads = v; }

    public int getNumRequests()             { return numRequests; }
    public void setNumRequests(int v)       { this.numRequests = v; }

    public double getReadRatio()            { return readRatio; }
    public void setReadRatio(double v)      { this.readRatio = v; }

    public long getTtlMillis()              { return ttlMillis; }
    public void setTtlMillis(long v)        { this.ttlMillis = v; }
}
