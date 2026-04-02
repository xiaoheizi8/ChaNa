package com.chanacode.core.registry;

import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.common.constant.RegistryConstants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class ServiceRegistry {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ServiceInstance>> serviceIndex;
    private final ConcurrentHashMap<String, ServiceInstance> instanceIndex;
    private final ConcurrentHashMap<String, Long> versionIndex;
    private final AtomicLong totalRegistrations;
    private final AtomicLong totalDiscovers;
    private volatile long currentVersion;

    public ServiceRegistry() {
        this.serviceIndex = new ConcurrentHashMap<>(8192);
        this.instanceIndex = new ConcurrentHashMap<>(102400);
        this.versionIndex = new ConcurrentHashMap<>();
        this.totalRegistrations = new AtomicLong(0);
        this.totalDiscovers = new AtomicLong(0);
        this.currentVersion = System.currentTimeMillis();
    }

    public boolean register(ServiceInstance instance) {
        if (instance == null || instance.getInstanceId() == null) {
            return false;
        }

        String serviceName = instance.getServiceName();
        String instanceId = instance.getInstanceId();
        String namespace = instance.getNamespace() != null ? instance.getNamespace() : RegistryConstants.DEFAULT_NAMESPACE;
        String key = buildKey(namespace, serviceName);

        instanceIndex.compute(instanceId, (id, old) -> {
            if (old == null) {
                CopyOnWriteArrayList<ServiceInstance> list = serviceIndex.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
                list.add(instance);
            } else {
                CopyOnWriteArrayList<ServiceInstance> list = serviceIndex.get(key);
                if (list != null) {
                    list.removeIf(i -> i.getInstanceId().equals(instanceId));
                    list.add(instance);
                }
            }
            return instance;
        });

        versionIndex.put(key, ++currentVersion);
        totalRegistrations.incrementAndGet();
        return true;
    }

    public boolean deregister(String instanceId, String serviceName, String namespace) {
        String key = buildKey(namespace, serviceName);
        ServiceInstance removed = instanceIndex.remove(instanceId);
        if (removed != null) {
            CopyOnWriteArrayList<ServiceInstance> list = serviceIndex.get(key);
            if (list != null) {
                list.removeIf(i -> i.getInstanceId().equals(instanceId));
                if (list.isEmpty()) {
                    serviceIndex.remove(key);
                }
            }
            versionIndex.put(key, ++currentVersion);
            return true;
        }
        return false;
    }

    public List<ServiceInstance> discover(String serviceName, String namespace) {
        totalDiscovers.incrementAndGet();
        String key = buildKey(namespace, serviceName);
        CopyOnWriteArrayList<ServiceInstance> list = serviceIndex.get(key);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }

    public List<ServiceInstance> discoverHealthy(String serviceName, String namespace) {
        List<ServiceInstance> all = discover(serviceName, namespace);
        List<ServiceInstance> healthy = new ArrayList<>();
        for (ServiceInstance inst : all) {
            if (inst.isHealthy()) {
                healthy.add(inst);
            }
        }
        return healthy;
    }

    public boolean heartbeat(String instanceId) {
        ServiceInstance instance = instanceIndex.get(instanceId);
        if (instance != null) {
            instance.updateHeartbeat(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public boolean markUnhealthy(String instanceId) {
        ServiceInstance instance = instanceIndex.get(instanceId);
        if (instance != null) {
            instance.setHealthy(false);
            return true;
        }
        return false;
    }

    public Set<String> getAllServices() {
        return new HashSet<>(serviceIndex.keySet());
    }

    public long getTotalInstances() {
        return instanceIndex.size();
    }

    public long getServiceCount() {
        return serviceIndex.size();
    }

    public long getVersion(String serviceName, String namespace) {
        return versionIndex.getOrDefault(buildKey(namespace, serviceName), 0L);
    }

    public long getTotalRegistrations() {
        return totalRegistrations.get();
    }

    public long getTotalDiscovers() {
        return totalDiscovers.get();
    }

    public ServiceInstance getInstance(String instanceId) {
        return instanceIndex.get(instanceId);
    }

    private String buildKey(String namespace, String serviceName) {
        return namespace + ":/" + serviceName;
    }
}
