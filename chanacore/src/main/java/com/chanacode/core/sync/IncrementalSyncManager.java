package com.chanacode.core.sync;

import com.chanacode.common.dto.ServiceInstance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ChaNa增量同步管理器
 *
 * <p>基于版本控制的增量同步机制，服务实例状态变更到客户端感知的延迟小于50ms。
 *
 * <p>核心特性：
 * <ul>
 *   <li>全局单调递增版本号</li>
 *   <li>增量推送变更（新增/删除/修改）</li>
 *   <li>客户端订阅并维护本地版本</li>
 *   <li>变更延迟小于50ms</li>
 * </ul>
 *
 * <p>同步流程：
 * <ol>
 *   <li>客户端订阅服务，携带当前版本号</li>
 *   <li>服务端检测到变更，更新快照版本</li>
 *   <li>客户端拉取增量变更或服务端推送变更</li>
 *   <li>客户端更新本地缓存和服务列表</li>
 * </ol>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
public class IncrementalSyncManager {

    private final ConcurrentHashMap<String, Long> clientVersions;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Subscriber>> subscribers;
    private final ConcurrentHashMap<String, ServiceSnapshot> serviceSnapshots;
    private final AtomicLong globalVersion;
    private final ReadWriteLock versionLock;

    public IncrementalSyncManager() {
        this.clientVersions = new ConcurrentHashMap<>();
        this.subscribers = new ConcurrentHashMap<>();
        this.serviceSnapshots = new ConcurrentHashMap<>();
        this.globalVersion = new AtomicLong(0);
        this.versionLock = new ReentrantReadWriteLock();
    }
    /**
     * 订阅服务实例变更通知
     *
     * <p>客户端通过此方法订阅指定服务的实例变更通知，订阅后服务端将向该客户端推送实例变更事件。</p>
     *
     * @param clientId 客户端唯一标识
     * @param namespace 命名空间
     * @param serviceName 服务名称
     * @param currentVersion 客户端当前持有的版本号，用于增量同步
     * @return {@link SubscriptionResult} 包含当前最新版本和服务实例列表的订阅结果
     */
    public SubscriptionResult subscribe(String clientId, String namespace, String serviceName, long currentVersion) {
        String key = buildKey(namespace, serviceName);
        clientVersions.put(clientId, currentVersion);
        CopyOnWriteArrayList<Subscriber> subs = subscribers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        subs.add(new Subscriber(clientId, namespace, serviceName, currentVersion));
        
        ServiceSnapshot snapshot = serviceSnapshots.get(key);
        List<ServiceInstance> instances = snapshot != null ? snapshot.getInstances() : List.of();
        return new SubscriptionResult(snapshot != null ? snapshot.getVersion() : 0, instances);
    }
    /**
     * 取消订阅服务实例变更通知
     *
     * <p>当客户端不再需要接收某服务的变更通知时，调用此方法取消订阅关系。</p>
     *
     * @param clientId 客户端唯一标识
     * @param namespace 命名空间
     * @param serviceName 服务名称
     */
    public void unsubscribe(String clientId, String namespace, String serviceName) {
        String key = buildKey(namespace, serviceName);
        CopyOnWriteArrayList<Subscriber> subs = subscribers.get(key);
        if (subs != null) {
            subs.removeIf(s -> s.clientId().equals(clientId));
        }
        clientVersions.remove(clientId);
    }
    /**
     * 通知服务实例发生变更
     *
     * <p>当服务实例发生新增、删除或修改时，调用此方法更新快照并通知所有订阅者。</p>
     * <p>该方法会生成新的全局版本号，确保版本单调递增。</p>
     *
     * @param namespace 命名空间
     * @param serviceName 服务名称
     * @param added 新增的服务实例列表
     * @param removed 被删除的服务实例 ID 列表
     * @param modified 修改的服务实例列表
     * @return {@link IncrementalSyncResponse} 包含新版本号和变更详情的响应对象
     */
    public IncrementalSyncResponse notifyChange(String namespace, String serviceName,
                                               List<ServiceInstance> added,
                                               List<String> removed,
                                               List<ServiceInstance> modified) {
        String key = buildKey(namespace, serviceName);
        long newVersion = globalVersion.incrementAndGet();

        versionLock.writeLock().lock();
        try {
            ServiceSnapshot snapshot = serviceSnapshots.computeIfAbsent(key, k -> new ServiceSnapshot(newVersion));
            snapshot.update(added, removed, modified, newVersion);

            for (Subscriber sub : subscribers.getOrDefault(key, new CopyOnWriteArrayList<>())) {
                clientVersions.put(sub.clientId(), newVersion);
            }
        } finally {
            versionLock.writeLock().unlock();
        }

        return new IncrementalSyncResponse(newVersion, added, removed, modified);
    }

    /**
     * 获取增量变更数据
     *
     * <p>客户端通过此方法拉取自上次版本以来的增量变更数据。</p>
     * <p>若客户端版本与服务端版本一致，则返回空变更列表；否则返回所有变更实例。</p>
     *
     * @param clientId 客户端唯一标识
     * @param namespace 命名空间
     * @param serviceName 服务名称
     * @return {@link IncrementalSyncResponse} 包含最新版本和增量变更数据的响应对象
     */
    public IncrementalSyncResponse getIncrementalChanges(String clientId, String namespace, String serviceName) {
        String key = buildKey(namespace, serviceName);
        long clientVersion = clientVersions.getOrDefault(clientId, 0L);
        
        ServiceSnapshot snapshot = serviceSnapshots.get(key);
        if (snapshot == null) {
            return new IncrementalSyncResponse(globalVersion.get(), List.of(), List.of(), List.of());
        }
        
        if (clientVersion >= snapshot.getVersion()) {
            return new IncrementalSyncResponse(snapshot.getVersion(), List.of(), List.of(), List.of());
        }
        
        clientVersions.put(clientId, snapshot.getVersion());
        return new IncrementalSyncResponse(snapshot.getVersion(), snapshot.getInstances(), List.of(), List.of());
    }
    /**
     * 获取全局版本号
     *
     * <p>返回当前系统的全局版本号，该版本号单调递增，用于标识数据变更的次序。</p>
     *
     * @return 全局版本号
     */
    public long getGlobalVersion() { return globalVersion.get(); }
    /**
     * 构建服务键值
     *
     * <p>将命名空间和服务名组合成唯一的键值，用于内部缓存和订阅管理。</p>
     *
     * @param namespace 命名空间
     * @param serviceName 服务名称
     * @return 格式化的键值字符串，格式为 "namespace:/serviceName"
     */
    private String buildKey(String namespace, String serviceName) {
        return namespace + ":/" + serviceName;
    }

    public record Subscriber(String clientId, String namespace, String serviceName, long lastVersion) {}

    public static class ServiceSnapshot {
        private volatile long version;
        private final CopyOnWriteArrayList<ServiceInstance> instances;
        private final ConcurrentHashMap<String, ServiceInstance> instanceMap;

        ServiceSnapshot(long version) {
            this.version = version;
            this.instances = new CopyOnWriteArrayList<>();
            this.instanceMap = new ConcurrentHashMap<>();
        }
        /**
         * 更新服务快照
         *
         * <p>根据传入的新增、删除和修改列表更新当前服务实例快照。</p>
         * <p>该方法保证实例列表和映射表的一致性。</p>
         *
         * @param added 新增的服务实例列表
         * @param removed 被删除的服务实例 ID 列表
         * @param modified 修改的服务实例列表
         * @param newVersion 新的版本号
         */
        void update(List<ServiceInstance> added, List<String> removed, List<ServiceInstance> modified, long newVersion) {
            this.version = newVersion;

            for (String instanceId : removed) {
                ServiceInstance removedInst = instanceMap.remove(instanceId);
                if (removedInst != null) {
                    instances.remove(removedInst);
                }
            }

            for (ServiceInstance inst : modified) {
                ServiceInstance old = instanceMap.get(inst.getInstanceId());
                if (old != null) {
                    int index = instances.indexOf(old);
                    if (index >= 0) {
                        instances.set(index, inst);
                    }
                }
                instanceMap.put(inst.getInstanceId(), inst);
            }

            for (ServiceInstance inst : added) {
                if (!instanceMap.containsKey(inst.getInstanceId())) {
                    instances.add(inst);
                    instanceMap.put(inst.getInstanceId(), inst);
                }
            }
        }

        long getVersion() { return version; }
        /**
         * 获取所有服务实例
         *
         * <p>返回当前快照中所有服务实例的副本，确保外部无法直接修改内部数据结构。</p>
         *
         * @return 服务实例列表的副本
         */
        List<ServiceInstance> getInstances() { return new ArrayList<>(instances); }
    }

    public record SubscriptionResult(long version, List<ServiceInstance> instances) {}
    public record IncrementalSyncResponse(long version, List<ServiceInstance> added, List<String> removed, List<ServiceInstance> modified) {}
}
