package com.chanacode.core.cache;

import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.common.constant.RegistryConstants;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ChaNa三级缓存管理器
 *
 * <p>采用三级缓存架构，平衡性能与一致性：
 * <ul>
 *   <li><b>L1 本地缓存</b> - Guava Cache，TTL 5秒，进程内共享</li>
 *   <li><b>L2 进程缓存</b> - ConcurrentHashMap，TTL 30秒</li>
 *   <li><b>L3 注册表</b> - 直接访问ServiceRegistry</li>
 * </ul>
 *
 * <p>读取流程：L1 -> L2 -> L3
 * <p>写入流程：同时写入L1和L2
 * <p>失效策略：版本号变更时失效对应缓存
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
public class RegistryCacheManager {

    private final L1LocalCache localCache;
    private final L2ProcessCache processCache;
    private final Map<String, Long> cacheVersionMap;

    public RegistryCacheManager() {
        this.localCache = new L1LocalCache(RegistryConstants.L1_CACHE_SIZE, RegistryConstants.L1_CACHE_TTL_SECONDS);
        this.processCache = new L2ProcessCache(RegistryConstants.L2_CACHE_SIZE, RegistryConstants.L2_CACHE_TTL_SECONDS * 1000L);
        this.cacheVersionMap = new ConcurrentHashMap<>();
    }

    /**
     * @methodName: getFromCache
     * @description: 三级缓存读取 - L1 -> L2 -> null
     * @param: [namespace, serviceName, version]
     * @return: List<ServiceInstance>
     */
    public List<ServiceInstance> getFromCache(String namespace, String serviceName, long version) {
        String key = buildKey(namespace, serviceName);
        Long cachedVersion = cacheVersionMap.get(key);

        if (cachedVersion != null && cachedVersion == version) {
            List<ServiceInstance> cached = localCache.get(key);
            if (cached != null) {
                return cached;
            }
        }

        List<ServiceInstance> processCached = processCache.get(key);
        if (processCached != null) {
            localCache.put(key, processCached);
            return processCached;
        }
        return null;
    }

    /**
     * @methodName: putToCache
     * @description: 写入缓存
     * @param: [namespace, serviceName, instances, version]
     */
    public void putToCache(String namespace, String serviceName, List<ServiceInstance> instances, long version) {
        String key = buildKey(namespace, serviceName);
        localCache.put(key, instances);
        processCache.put(key, instances);
        cacheVersionMap.put(key, version);
    }

    /**
     * @methodName: invalidate
     * @description: 失效缓存
     * @param: [namespace, serviceName]
     */
    public void invalidate(String namespace, String serviceName) {
        String key = buildKey(namespace, serviceName);
        localCache.invalidate(key);
        processCache.invalidate(key);
        cacheVersionMap.remove(key);
    }

    /**
     * @methodName: invalidateAll
     * @description: 失效所有缓存
     */
    public void invalidateAll() {
        localCache.invalidateAll();
        processCache.invalidateAll();
        cacheVersionMap.clear();
    }

    private String buildKey(String namespace, String serviceName) {
        return namespace + ":/" + serviceName;
    }

    private static class L1LocalCache {
        private final Cache<String, List<ServiceInstance>> cache;

        L1LocalCache(int maxSize, long expireSeconds) {
            this.cache = CacheBuilder.newBuilder()
                    .maximumSize(maxSize)
                    .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                    .recordStats()
                    .build();
        }

        List<ServiceInstance> get(String key) {
            return cache.getIfPresent(key);
        }

        void put(String key, List<ServiceInstance> value) {
            cache.put(key, value);
        }

        void invalidate(String key) {
            cache.invalidate(key);
        }

        void invalidateAll() {
            cache.invalidateAll();
        }
    }

    private static class L2ProcessCache {
        private final Map<String, CacheEntry> cache;

        L2ProcessCache(int expectedSize, long expireMs) {
            this.cache = new ConcurrentHashMap<>(expectedSize);
        }

        List<ServiceInstance> get(String key) {
            CacheEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                entry.updateAccessTime();
                return entry.instances;
            }
            return null;
        }

        void put(String key, List<ServiceInstance> instances) {
            cache.put(key, new CacheEntry(instances, 30_000));
        }

        void invalidate(String key) {
            cache.remove(key);
        }

        void invalidateAll() {
            cache.clear();
        }
    }

    private static class CacheEntry {
        final List<ServiceInstance> instances;
        final long createTime;
        final long ttlMs;
        volatile long lastAccessTime;

        CacheEntry(List<ServiceInstance> instances, long ttlMs) {
            this.instances = instances;
            this.createTime = System.currentTimeMillis();
            this.ttlMs = ttlMs;
            this.lastAccessTime = createTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createTime > ttlMs;
        }

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
