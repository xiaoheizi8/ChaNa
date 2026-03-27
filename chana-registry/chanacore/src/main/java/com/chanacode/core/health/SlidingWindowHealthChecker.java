package com.chanacode.core.health;

import com.chanacode.common.constant.RegistryConstants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ChaNa滑动窗口健康检查器
 *
 * <p>采用滑动时间窗口算法判断节点健康状态，有效防止网络抖动导致的误剔除。
 *
 * <p>核心算法：
 * <ul>
 *   <li>时间窗口：30秒，分为3个10秒桶</li>
 *   <li>健康率：成功心跳数 / 总心跳数</li>
 *   <li>判定阈值：健康率 >= 30% 标记为健康</li>
 *   <li>保护模式：健康实例比例 < 20% 时停止剔除</li>
 * </ul>
 *
 * <p>设计原理：
 * <br>通过滑动窗口统计最近30秒的心跳成功率，
 * 只有连续失败的实例才会被标记为不健康，
 * 单次网络抖动不会影响健康状态判定。
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
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

    /**
     * @methodName: recordHeartbeat
     * @description: 记录心跳
     * @param: [instanceId, success]
     */
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
        if (healthyRate >= RegistryConstants.UNHEALTHY_THRESHOLD) {
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

    /**
     * @methodName: isHealthy
     * @description: 检查实例是否健康
     * @param: [instanceId]
     * @return: boolean
     */
    public boolean isHealthy(String instanceId) {
        HealthStatus status = instanceHealth.get(instanceId);
        return status != null && status.isHealthy();
    }

    /**
     * @methodName: shouldProtect
     * @description: 检查是否应该进入保护模式
     *              当健康实例比例低于阈值时停止剔除
     * @return: boolean
     */
    public boolean shouldProtect() {
        int total = healthyCount.get() + unhealthyCount.get();
        if (total == 0) return false;
        double healthyRate = (double) healthyCount.get() / total;
        return healthyRate < RegistryConstants.PROTECTION_THRESHOLD;
    }

    public int getHealthyCount() { return healthyCount.get(); }
    public int getUnhealthyCount() { return unhealthyCount.get(); }

    public static class HealthWindow {
        private static final int TOTAL_BUCKETS = RegistryConstants.HEALTH_CHECK_WINDOW_SECONDS / RegistryConstants.HEALTH_CHECK_BUCKET_SIZE;
        
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
            if (now - lastBucketTime >= RegistryConstants.HEALTH_CHECK_BUCKET_SIZE * 1000L) {
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
