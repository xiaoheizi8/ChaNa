package com.chanacode.core.cache;

import com.chanacode.common.dto.ServiceInstance;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RegistryCacheManager {

    private final ConcurrentHashMap<String, List<ServiceInstance>> cache;
    private final ConcurrentHashMap<String, Long> versionCache;
    private final ScheduledExecutorService scheduler;
    private volatile long lastUpdateTime;

    public RegistryCacheManager() {
        this.cache = new ConcurrentHashMap<>();
        this.versionCache = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void put(String key, List<ServiceInstance> instances, long version) {
        cache.put(key, instances);
        versionCache.put(key, version);
        lastUpdateTime = System.currentTimeMillis();
    }

    public List<ServiceInstance> get(String key) {
        return cache.get(key);
    }

    public Long getVersion(String key) {
        return versionCache.get(key);
    }

    public boolean isExpired(String key, long maxAge) {
        Long version = versionCache.get(key);
        if (version == null) {
            return true;
        }
        return (System.currentTimeMillis() - lastUpdateTime) > maxAge;
    }

    public void invalidate(String key) {
        cache.remove(key);
        versionCache.remove(key);
    }

    public void clear() {
        cache.clear();
        versionCache.clear();
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
