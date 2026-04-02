package com.chanacode.core.health;

import com.chanacode.common.constant.RegistryConstants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SlidingWindowHealthChecker {

    private final ConcurrentHashMap<String, HealthWindow> instanceWindows;
    private final ConcurrentHashMap<String, HealthStatus> instanceHealth;
    private final AtomicInteger healthyCount;
    private final AtomicInteger unhealthyCount;

    public SlidingWindowHealthChecker() {
        this.instanceWindows = new ConcurrentHashMap<>();
        this.instanceHealth = new ConcurrentHashMap<>();
        this.healthyCount = new AtomicInteger(0);
        this.unhealthyCount = new AtomicInteger(0);
    }

    public void recordHeartbeat(String instanceId, boolean success) {
        HealthWindow window = instanceWindows.computeIfAbsent(instanceId, k -> new HealthWindow());
        window.record(success);
        updateHealthStatus(instanceId);
    }

    private void updateHealthStatus(String instanceId) {
        HealthWindow window = instanceWindows.get(instanceId);
        if (window == null) return;

        double healthyRate = window.getHealthyRate();
        HealthStatus currentStatus = instanceHealth.get(instanceId);
        boolean wasHealthy = currentStatus != null && currentStatus.isHealthy();

        boolean shouldBeHealthy;
        if (healthyRate >= 0.3) {
            shouldBeHealthy = true;
        } else if (healthyRate < 0.1) {
            shouldBeHealthy = false;
        } else {
            shouldBeHealthy = wasHealthy;
        }

        instanceHealth.compute(instanceId, (k, v) -> {
            if (v == null) {
                if (shouldBeHealthy) healthyCount.incrementAndGet();
                else unhealthyCount.incrementAndGet();
                return new HealthStatus(instanceId, shouldBeHealthy, healthyRate);
            }
            if (v.isHealthy() != shouldBeHealthy) {
                if (shouldBeHealthy) {
                    healthyCount.incrementAndGet();
                    unhealthyCount.decrementAndGet();
                } else {
                    unhealthyCount.incrementAndGet();
                    healthyCount.decrementAndGet();
                }
            }
            v.update(shouldBeHealthy, healthyRate);
            return v;
        });
    }

    public boolean isHealthy(String instanceId) {
        HealthStatus status = instanceHealth.get(instanceId);
        return status != null && status.isHealthy();
    }

    public boolean shouldProtect() {
        int total = healthyCount.get() + unhealthyCount.get();
        if (total == 0) return false;
        double healthyRate = (double) healthyCount.get() / total;
        return healthyRate < 0.2;
    }

    public int getHealthyCount() { return healthyCount.get(); }
    public int getUnhealthyCount() { return unhealthyCount.get(); }

    public static class HealthWindow {
        private static final int TOTAL_BUCKETS = 3;
        
        private final AtomicInteger[] successBuckets;
        private final AtomicInteger[] totalBuckets;
        private volatile int currentBucket;
        private volatile long lastBucketTime;

        HealthWindow() {
            this.successBuckets = new AtomicInteger[TOTAL_BUCKETS];
            this.totalBuckets = new AtomicInteger[TOTAL_BUCKETS];
            this.lastBucketTime = System.currentTimeMillis();
            for (int i = 0; i < TOTAL_BUCKETS; i++) {
                successBuckets[i] = new AtomicInteger(0);
                totalBuckets[i] = new AtomicInteger(0);
            }
        }

        void record(boolean success) {
            long now = System.currentTimeMillis();
            if (now - lastBucketTime >= 10000L) {
                currentBucket = (currentBucket + 1) % TOTAL_BUCKETS;
                successBuckets[currentBucket].set(0);
                totalBuckets[currentBucket].set(0);
                lastBucketTime = now;
            }
            totalBuckets[currentBucket].incrementAndGet();
            if (success) {
                successBuckets[currentBucket].incrementAndGet();
            }
        }

        double getHealthyRate() {
            int total = 0, success = 0;
            for (int i = 0; i < TOTAL_BUCKETS; i++) {
                total += totalBuckets[i].get();
                success += successBuckets[i].get();
            }
            return total > 0 ? (double) success / total : 1.0;
        }
    }

    public static class HealthStatus {
        private final String instanceId;
        private volatile boolean healthy;
        private volatile double healthyRate;

        HealthStatus(String instanceId, boolean healthy, double healthyRate) {
            this.instanceId = instanceId;
            this.healthy = healthy;
            this.healthyRate = healthyRate;
        }

        void update(boolean healthy, double healthyRate) {
            this.healthy = healthy;
            this.healthyRate = healthyRate;
        }

        boolean isHealthy() { return healthy; }
        double getHealthyRate() { return healthyRate; }
    }
}
