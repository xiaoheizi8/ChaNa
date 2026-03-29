package com.chanacode.core.namespace;

import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.common.constant.RegistryConstants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ChaNa多租户命名空间管理器
 *
 * <p>支持Namespace和Group两级隔离架构，支持100+租户。
 *
 * <p>隔离层级：
 * <ul>
 *   <li><b>Namespace</b> - 租户级别隔离，如 dev/test/prod 环境</li>
 *   <li><b>Group</b> - 服务分组隔离，如灰度/正式/内部/外部</li>
 * </ul>
 *
 * <p>使用场景：
 * <ul>
 *   <li>多租户系统 - 每个租户独立的Namespace</li>
 *   <li>多环境隔离 - dev/test/staging/prod</li>
 *   <li>服务分组 - 按业务线或功能分组</li>
 * </ul>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
public class NamespaceManager {

    private final ConcurrentHashMap<String, Namespace> namespaces;

    public NamespaceManager() {
        this.namespaces = new ConcurrentHashMap<>();
        createNamespace(RegistryConstants.DEFAULT_NAMESPACE, "Default namespace");
    }

    public boolean createNamespace(String namespace, String description) {
        if (namespace == null || namespace.isBlank()) return false;
        return namespaces.putIfAbsent(namespace, new Namespace(namespace, description)) == null;
    }

    public boolean deleteNamespace(String namespace) {
        if (RegistryConstants.DEFAULT_NAMESPACE.equals(namespace)) return false;
        return namespaces.remove(namespace) != null;
    }

    public Namespace getNamespace(String namespace) {
        return namespaces.getOrDefault(namespace, namespaces.get(RegistryConstants.DEFAULT_NAMESPACE));
    }

    public List<NamespaceInfo> getAllNamespaces() {
        List<NamespaceInfo> result = new ArrayList<>();
        for (Namespace ns : namespaces.values()) {
            result.add(new NamespaceInfo(ns.getName(), ns.getDescription(), ns.getServiceCount(), ns.getInstanceCount()));
        }
        return result;
    }

    public boolean registerService(String namespace, ServiceInstance instance) {
        return getNamespace(namespace).registerService(instance);
    }

    public boolean deregisterService(String namespace, String instanceId, String serviceName) {
        return getNamespace(namespace).deregisterService(instanceId, serviceName);
    }

    public List<ServiceInstance> discoverServices(String namespace, String serviceName, String group, List<String> tags) {
        return getNamespace(namespace).discoverServices(serviceName, group, tags);
    }

    public long getVersion(String namespace) {
        return getNamespace(namespace).getVersion();
    }

    public static class Namespace {
        private final String name;
        private final String description;
        private final ConcurrentHashMap<String, ServiceGroup> services;
        private final AtomicLong version;
        private final AtomicLong totalInstances;

        Namespace(String name, String description) {
            this.name = name;
            this.description = description;
            this.services = new ConcurrentHashMap<>();
            this.version = new AtomicLong(System.currentTimeMillis());
            this.totalInstances = new AtomicLong(0);
        }

        boolean registerService(ServiceInstance instance) {
            String serviceName = instance.getServiceName();
            ServiceGroup sg = services.computeIfAbsent(serviceName, k -> new ServiceGroup(serviceName));
            if (sg.register(instance)) {
                totalInstances.incrementAndGet();
                version.incrementAndGet();
                return true;
            }
            return false;
        }

        boolean deregisterService(String instanceId, String serviceName) {
            ServiceGroup sg = services.get(serviceName);
            if (sg != null && sg.deregister(instanceId)) {
                totalInstances.decrementAndGet();
                version.incrementAndGet();
                return true;
            }
            return false;
        }

        List<ServiceInstance> discoverServices(String serviceName, String group, List<String> tags) {
            ServiceGroup sg = services.get(serviceName);
            if (sg == null) return List.of();
            return sg.discover(group, tags);
        }

        String getName() { return name; }
        String getDescription() { return description; }
        long getVersion() { return version.get(); }
        int getServiceCount() { return services.size(); }
        long getInstanceCount() { return totalInstances.get(); }
    }

    public static class ServiceGroup {
        private final String serviceName;
        private final ConcurrentHashMap<String, CopyOnWriteArrayList<ServiceInstance>> groups;
        private final ConcurrentHashMap<String, ServiceInstance> instances;

        ServiceGroup(String serviceName) {
            this.serviceName = serviceName;
            this.groups = new ConcurrentHashMap<>();
            this.instances = new ConcurrentHashMap<>();
            groups.put(RegistryConstants.DEFAULT_GROUP, new CopyOnWriteArrayList<>());
        }

        boolean register(ServiceInstance instance) {
            String group = instance.getGroup() != null ? instance.getGroup() : RegistryConstants.DEFAULT_GROUP;
            groups.computeIfAbsent(group, k -> new CopyOnWriteArrayList<>()).add(instance);
            instances.put(instance.getInstanceId(), instance);
            return true;
        }

        boolean deregister(String instanceId) {
            ServiceInstance removed = instances.remove(instanceId);
            if (removed != null) {
                String group = removed.getGroup() != null ? removed.getGroup() : RegistryConstants.DEFAULT_GROUP;
                CopyOnWriteArrayList<ServiceInstance> groupInstances = groups.get(group);
                if (groupInstances != null) {
                    groupInstances.removeIf(i -> i.getInstanceId().equals(instanceId));
                }
                return true;
            }
            return false;
        }

        List<ServiceInstance> discover(String group, List<String> tags) {
            List<ServiceInstance> result;
            if (group != null) {
                CopyOnWriteArrayList<ServiceInstance> groupInstances = groups.get(group);
                result = groupInstances != null ? new ArrayList<>(groupInstances) : new ArrayList<>();
            } else {
                result = new ArrayList<>();
                for (var insts : groups.values()) {
                    result.addAll(insts);
                }
            }
            if (tags != null && !tags.isEmpty()) {
                result = result.stream().filter(i -> i.getTags() != null && i.getTags().containsAll(tags)).toList();
            }
            return result;
        }
    }

    public record NamespaceInfo(String namespace, String description, int serviceCount, long instanceCount) {}
}
