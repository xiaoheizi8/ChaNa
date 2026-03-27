package com.chanacode.spring.controller;

import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.core.cache.RegistryCacheManager;
import com.chanacode.core.health.SlidingWindowHealthChecker;
import com.chanacode.core.metrics.HighPrecisionMetricsCollector;
import com.chanacode.core.namespace.NamespaceManager;
import com.chanacode.core.registry.ServiceRegistry;
import com.chanacode.core.sync.IncrementalSyncManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ChaNa 注册中心 REST API 控制器
 *
 * <p>提供管理界面的 RESTful API 接口。
 *
 * <p>接口前缀: /api/v1
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RegistryApiController {

    private final ServiceRegistry registry;
    private final RegistryCacheManager cacheManager;
    private final SlidingWindowHealthChecker healthChecker;
    private final NamespaceManager namespaceManager;
    private final IncrementalSyncManager syncManager;
    private final HighPrecisionMetricsCollector metrics;

    /**
     * 获取核心性能指标
     */
    @GetMapping("/metrics")
    public ApiResponse<MetricsDTO> getMetrics() {
        HighPrecisionMetricsCollector.MetricsSnapshot snapshot = metrics.getSnapshot();
        MetricsDTO dto = new MetricsDTO();
        dto.setQps(snapshot.qps());
        dto.setRegisterQps(snapshot.registerQps());
        dto.setDiscoverQps(snapshot.discoverQps());
        dto.setHeartbeatQps(snapshot.totalHeartbeats() / Math.max(snapshot.uptimeSeconds(), 1));
        dto.setConnections(snapshot.connections());
        dto.setAvgLatencyUs(snapshot.avgLatencyUs());
        dto.setP50LatencyUs(snapshot.p50LatencyUs());
        dto.setP90LatencyUs(snapshot.p90LatencyUs());
        dto.setP99LatencyUs(snapshot.p99LatencyUs());
        dto.setTotalRequests(snapshot.totalRequests());
        return ApiResponse.success(dto);
    }

    /**
     * 获取服务列表
     */
    @GetMapping("/services")
    public ApiResponse<List<ServiceDTO>> getServices() {
        List<ServiceDTO> services = new ArrayList<>();
        Set<String> serviceKeys = registry.getAllServices();

        for (String key : serviceKeys) {
            String[] parts = key.split(":/");
            if (parts.length >= 2) {
                String namespace = parts[0];
                String serviceName = parts[1];
                List<ServiceInstance> instances = registry.discover(serviceName, namespace);

                ServiceDTO dto = new ServiceDTO();
                dto.setServiceName(serviceName);
                dto.setNamespace(namespace);
                dto.setInstanceCount(instances.size());
                dto.setHealthyCount((int) instances.stream().filter(ServiceInstance::isHealthy).count());
                dto.setVersion(instances.isEmpty() ? "1.0.0" : instances.get(0).getVersion());
                services.add(dto);
            }
        }

        if (services.isEmpty()) {
            services.add(createDemoService());
        }
        return ApiResponse.success(services);
    }

    /**
     * 获取单个服务详情
     */
    @GetMapping("/services/{serviceName}")
    public ApiResponse<ServiceDetailDTO> getServiceDetail(@PathVariable String serviceName,
                                                         @RequestParam(defaultValue = "default") String namespace) {
        List<ServiceInstance> instances = registry.discover(serviceName, namespace);

        ServiceDetailDTO dto = new ServiceDetailDTO();
        dto.setServiceName(serviceName);
        dto.setNamespace(namespace);
        dto.setTotal(instances.size());
        dto.setInstances(instances.stream().map(this::toInstanceDTO).collect(Collectors.toList()));
        return ApiResponse.success(dto);
    }

    /**
     * 获取命名空间列表
     */
    @GetMapping("/namespaces")
    public ApiResponse<List<NamespaceDTO>> getNamespaces() {
        List<NamespaceDTO> namespaces = new ArrayList<>();

        List<NamespaceManager.NamespaceInfo> list = namespaceManager.getAllNamespaces();
        for (NamespaceManager.NamespaceInfo info : list) {
            NamespaceDTO dto = new NamespaceDTO();
            dto.setNamespace(info.namespace());
            dto.setDescription(info.description());
            dto.setServiceCount(info.serviceCount());
            dto.setInstanceCount(info.instanceCount());
            namespaces.add(dto);
        }

        if (namespaces.isEmpty()) {
            NamespaceDTO dto = new NamespaceDTO();
            dto.setNamespace("default");
            dto.setDescription("默认命名空间");
            dto.setServiceCount(1);
            dto.setInstanceCount(3);
            namespaces.add(dto);
        }
        return ApiResponse.success(namespaces);
    }

    /**
     * 获取健康状态
     */
    @GetMapping("/health")
    public ApiResponse<HealthDTO> getHealth() {
        HealthDTO dto = new HealthDTO();
        dto.setStatus("UP");
        dto.setTimestamp(System.currentTimeMillis());
        dto.setHealthyInstances(healthChecker.getHealthyCount());
        dto.setUnhealthyInstances(healthChecker.getUnhealthyCount());
        dto.setProtectionMode(healthChecker.shouldProtect());
        return ApiResponse.success(dto);
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<StatsDTO> getStats() {
        StatsDTO dto = new StatsDTO();
        dto.setTotalInstances(registry.getTotalInstances());
        dto.setTotalServices(registry.getServiceCount());
        dto.setTotalRegistrations(registry.getTotalRegistrations());
        dto.setTotalDiscovers(registry.getTotalDiscovers());
        dto.setGlobalVersion(syncManager.getGlobalVersion());
        return ApiResponse.success(dto);
    }

    /**
     * 服务注册 (POST)
     */
    @PostMapping("/services/register")
    public ApiResponse<Void> registerService(@RequestBody ServiceInstance instance) {
        boolean success = registry.register(instance);
        if (success) {
            namespaceManager.registerService(instance.getNamespace(), instance);
            cacheManager.invalidate(instance.getNamespace(), instance.getServiceName());
            metrics.incrementRegister();
            syncManager.notifyChange(instance.getNamespace(), instance.getServiceName(),
                    List.of(instance), List.of(), List.of());
            return ApiResponse.success();
        }
        return ApiResponse.fail("Registration failed");
    }

    /**
     * 服务注销 (POST)
     */
    @PostMapping("/services/deregister")
    public ApiResponse<Void> deregisterService(@RequestBody DeregisterRequest request) {
        boolean success = registry.deregister(request.getInstanceId(), request.getServiceName(), request.getNamespace());
        if (success) {
            namespaceManager.deregisterService(request.getNamespace(), request.getInstanceId(), request.getServiceName());
            cacheManager.invalidate(request.getNamespace(), request.getServiceName());
            metrics.incrementDeregister();
            syncManager.notifyChange(request.getNamespace(), request.getServiceName(),
                    List.of(), List.of(request.getInstanceId()), List.of());
            return ApiResponse.success();
        }
        return ApiResponse.fail("Deregistration failed");
    }

    /**
     * 心跳 (POST)
     */
    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@RequestBody HeartbeatRequest request) {
        boolean success = registry.heartbeat(request.getInstanceId());
        healthChecker.recordHeartbeat(request.getInstanceId(), success);
        metrics.incrementHeartbeat();
        if (success) {
            return ApiResponse.success();
        }
        return ApiResponse.fail("Instance not found");
    }

    private ServiceInstanceDTO toInstanceDTO(ServiceInstance instance) {
        ServiceInstanceDTO dto = new ServiceInstanceDTO();
        dto.setInstanceId(instance.getInstanceId());
        dto.setServiceName(instance.getServiceName());
        dto.setHost(instance.getHost());
        dto.setPort(instance.getPort());
        dto.setHealthy(instance.isHealthy());
        dto.setWeight(instance.getWeight());
        dto.setVersion(instance.getVersion());
        dto.setLastHeartbeatTime(instance.getLastHeartbeatTime());
        dto.setRegistrationTime(instance.getRegistrationTime());
        return dto;
    }

    private ServiceDTO createDemoService() {
        ServiceDTO dto = new ServiceDTO();
        dto.setServiceName("demo-service");
        dto.setNamespace("default");
        dto.setInstanceCount(3);
        dto.setHealthyCount(3);
        dto.setVersion("1.0.0");
        return dto;
    }

    // ==================== DTO 类 ====================

    @lombok.Data
    public static class MetricsDTO {
        private long qps;
        private long registerQps;
        private long discoverQps;
        private long heartbeatQps;
        private long connections;
        private double avgLatencyUs;
        private double p50LatencyUs;
        private double p90LatencyUs;
        private double p99LatencyUs;
        private long totalRequests;
    }

    @lombok.Data
    public static class ServiceDTO {
        private String serviceName;
        private String namespace;
        private int instanceCount;
        private int healthyCount;
        private int unhealthyCount;
        private String version;
        private long qps;
        private double avgLatencyUs;
    }

    @lombok.Data
    public static class ServiceDetailDTO {
        private String serviceName;
        private String namespace;
        private int total;
        private List<ServiceInstanceDTO> instances;
    }

    @lombok.Data
    public static class ServiceInstanceDTO {
        private String instanceId;
        private String serviceName;
        private String host;
        private int port;
        private boolean healthy;
        private int weight;
        private String version;
        private long lastHeartbeatTime;
        private long registrationTime;
    }

    @lombok.Data
    public static class NamespaceDTO {
        private String namespace;
        private String description;
        private int serviceCount;
        private long instanceCount;
    }

    @lombok.Data
    public static class HealthDTO {
        private String status;
        private long timestamp;
        private int healthyInstances;
        private int unhealthyInstances;
        private boolean protectionMode;
    }

    @lombok.Data
    public static class StatsDTO {
        private long totalInstances;
        private long totalServices;
        private long totalRegistrations;
        private long totalDiscovers;
        private long globalVersion;
    }

    @lombok.Data
    public static class DeregisterRequest {
        private String instanceId;
        private String serviceName;
        private String namespace;
    }

    @lombok.Data
    public static class HeartbeatRequest {
        private String instanceId;
        private String serviceName;
        private String namespace;
    }
}
