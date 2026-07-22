package com.cache.dto;

public class LoadTestRequest {
    private int threadCount = 4;
    private Integer opsPerSecond;
    private int keySpaceSize = 1000;
    private double readWriteRatio = 0.8;
    private int durationSeconds = 10;
    private int valueSizeBytes = 128;

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int value) { this.threadCount = value; }
    public Integer getOpsPerSecond() { return opsPerSecond; }
    public void setOpsPerSecond(Integer value) { this.opsPerSecond = value; }
    public int getKeySpaceSize() { return keySpaceSize; }
    public void setKeySpaceSize(int value) { this.keySpaceSize = value; }
    public double getReadWriteRatio() { return readWriteRatio; }
    public void setReadWriteRatio(double value) { this.readWriteRatio = value; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int value) { this.durationSeconds = value; }
    public int getValueSizeBytes() { return valueSizeBytes; }
    public void setValueSizeBytes(int value) { this.valueSizeBytes = value; }

    public void validate() {
        if (threadCount < 1 || threadCount > 256) throw new IllegalArgumentException("threadCount must be between 1 and 256");
        if (opsPerSecond != null && opsPerSecond < 1) throw new IllegalArgumentException("opsPerSecond must be positive or omitted");
        if (keySpaceSize < 1) throw new IllegalArgumentException("keySpaceSize must be positive");
        if (readWriteRatio < 0 || readWriteRatio > 1) throw new IllegalArgumentException("readWriteRatio must be between 0 and 1");
        if (durationSeconds < 1 || durationSeconds > 3600) throw new IllegalArgumentException("durationSeconds must be between 1 and 3600");
        if (valueSizeBytes < 0 || valueSizeBytes > 1_048_576) throw new IllegalArgumentException("valueSizeBytes must be between 0 and 1048576");
    }
}
