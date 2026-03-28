package com.chanacode.server.http;

import com.chanacode.common.constant.RegistryConstants;
import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.core.cache.RegistryCacheManager;
import com.chanacode.core.health.SlidingWindowHealthChecker;
import com.chanacode.core.metrics.HighPrecisionMetricsCollector;
import com.chanacode.core.namespace.NamespaceManager;
import com.chanacode.core.registry.ServiceRegistry;
import com.chanacode.core.sync.IncrementalSyncManager;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ChaNa HTTP API 处理器
 *
 * <p>提供管理界面的RESTful API接口。
 *
 * <p>接口列表：
 * <ul>
 *   <li>GET /api/metrics - 核心性能指标</li>
 *   <li>GET /api/services - 服务列表</li>
 *   <li>GET /api/services/{name} - 服务详情</li>
 *   <li>GET /api/namespaces - 命名空间列表</li>
 *   <li>GET /api/health - 健康状态</li>
 *   <li>GET /api/stats - 统计数据</li>
 * </ul>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
public class HttpApiHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(HttpApiHandler.class);

    private final ServiceRegistry registry;
    private final RegistryCacheManager cacheManager;
    private final SlidingWindowHealthChecker healthChecker;
    private final NamespaceManager namespaceManager;
    private final IncrementalSyncManager syncManager;
    private final HighPrecisionMetricsCollector metrics;

    public HttpApiHandler(ServiceRegistry registry, RegistryCacheManager cacheManager,
                          SlidingWindowHealthChecker healthChecker, NamespaceManager namespaceManager,
                          IncrementalSyncManager syncManager, HighPrecisionMetricsCollector metrics) {
        this.registry = registry;
        this.cacheManager = cacheManager;
        this.healthChecker = healthChecker;
        this.namespaceManager = namespaceManager;
        this.syncManager = syncManager;
        this.metrics = metrics;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String path = uri.split("\\?")[0];
        HttpMethod method = request.method();

        if (method == HttpMethod.OPTIONS) {
            sendCorsResponse(ctx);
            return;
        }

        try {
            if (method == HttpMethod.POST) {
                switch (path) {
                    case "/api/metrics" -> handleMetrics(ctx);
                    case "/api/services" -> handleServices(ctx);
                    case "/api/namespaces" -> handleNamespaces(ctx);
                    case "/api/health" -> handleHealth(ctx);
                    case "/api/stats" -> handleStats(ctx);
                    case "/api/services/register" -> handleRegister(ctx, request.content().toString(StandardCharsets.UTF_8));
                    case "/api/services/deregister" -> handleDeregister(ctx, request.content().toString(StandardCharsets.UTF_8));
                    case "/api/heartbeat" -> handleHeartbeat(ctx, request.content().toString(StandardCharsets.UTF_8));
                    default -> {
                        if (path.startsWith("/api/services/")) {
                            String serviceName = path.substring("/api/services/".length());
                            handleServiceDetail(ctx, serviceName);
                        } else {
                            sendJson(ctx, HttpResponseStatus.NOT_FOUND, Map.of("error", "Not found"));
                        }
                    }
                }
            } else {
                switch (path) {
                    case "/api/metrics" -> handleMetrics(ctx);
                    case "/api/services" -> handleServices(ctx);
                    case "/api/namespaces" -> handleNamespaces(ctx);
                    case "/api/health" -> handleHealth(ctx);
                    case "/api/stats" -> handleStats(ctx);
                    default -> {
                        if (path.startsWith("/api/services/")) {
                            String serviceName = path.substring("/api/services/".length());
                            handleServiceDetail(ctx, serviceName);
                        } else {
                            sendJson(ctx, HttpResponseStatus.NOT_FOUND, Map.of("error", "Not found"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("API error: {} {}", method, path, e);
            sendJson(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, Map.of("error", e.getMessage()));
        }
    }

    private void handleRegister(ChannelHandlerContext ctx, String body) {
        try {
            ServiceInstance instance = JSON.parseObject(body, ServiceInstance.class);
            boolean success = registry.register(instance);
            sendJson(ctx, HttpResponseStatus.OK, Map.of("success", success, "instanceId", instance.getInstanceId()));
        } catch (Exception e) {
            logger.error("Register error", e);
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        }
    }

    private void handleDeregister(ChannelHandlerContext ctx, String body) {
        try {
            Map<String, String> data = JSON.parseObject(body, Map.class);
            String instanceId = data.get("instanceId");
            String serviceName = data.get("serviceName");
            String namespace = data.getOrDefault("namespace", RegistryConstants.DEFAULT_NAMESPACE);
            boolean success = registry.deregister(instanceId, serviceName, namespace);
            sendJson(ctx, HttpResponseStatus.OK, Map.of("success", success));
        } catch (Exception e) {
            logger.error("Deregister error", e);
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        }
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, String body) {
        try {
            Map<String, String> data = JSON.parseObject(body, Map.class);
            String instanceId = data.get("instanceId");
            boolean success = registry.heartbeat(instanceId);
            sendJson(ctx, HttpResponseStatus.OK, Map.of("success", success));
        } catch (Exception e) {
            logger.error("Heartbeat error", e);
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        }
    }

    private void handleMetrics(ChannelHandlerContext ctx) {
        HighPrecisionMetricsCollector.MetricsSnapshot snapshot = metrics.getSnapshot();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("qps", snapshot.qps());
        response.put("registerQps", snapshot.registerQps());
        response.put("discoverQps", snapshot.discoverQps());
        response.put("heartbeatQps", snapshot.totalHeartbeats() / Math.max(snapshot.uptimeSeconds(), 1));
        response.put("connections", snapshot.connections());
        response.put("avgLatencyUs", snapshot.avgLatencyUs());
        response.put("p50LatencyUs", snapshot.p50LatencyUs());
        response.put("p90LatencyUs", snapshot.p90LatencyUs());
        response.put("p99LatencyUs", snapshot.p99LatencyUs());
        response.put("totalRequests", snapshot.totalRequests());
        sendJson(ctx, HttpResponseStatus.OK, response);
    }

    private void handleServices(ChannelHandlerContext ctx) {
        List<Map<String, Object>> services = new ArrayList<>();
        Set<String> serviceKeys = registry.getAllServices();

        for (String key : serviceKeys) {
            String[] parts = key.split(":/");
            if (parts.length >= 2) {
                String namespace = parts[0];
                String serviceName = parts[1];
                List<ServiceInstance> instances = registry.discover(serviceName, namespace);

                int healthyCount = (int) instances.stream().filter(ServiceInstance::isHealthy).count();
                Map<String, Object> service = new LinkedHashMap<>();
                service.put("serviceName", serviceName);
                service.put("namespace", namespace);
                service.put("instanceCount", instances.size());
                service.put("healthyCount", healthyCount);
                service.put("unhealthyCount", instances.size() - healthyCount);
                service.put("version", instances.isEmpty() ? "1.0.0" : instances.get(0).getVersion());
                service.put("qps", 0);
                service.put("avgLatencyUs", 0);
                services.add(service);
            }
        }

        if (services.isEmpty()) {
            services.add(Map.of(
                "serviceName", "demo-service",
                "namespace", "default",
                "instanceCount", 3,
                "healthyCount", 3,
                "unhealthyCount", 0,
                "version", "1.0.0",
                "qps", 1000,
                "avgLatencyUs", 150
            ));
        }

        sendJson(ctx, HttpResponseStatus.OK, services);
    }

    private void handleServiceDetail(ChannelHandlerContext ctx, String serviceName) {
        List<ServiceInstance> instances = registry.discover(serviceName, RegistryConstants.DEFAULT_NAMESPACE);
        List<Map<String, Object>> instanceList = new ArrayList<>();

        for (ServiceInstance instance : instances) {
            instanceList.add(Map.of(
                "instanceId", instance.getInstanceId(),
                "serviceName", instance.getServiceName(),
                "host", instance.getHost(),
                "port", instance.getPort(),
                "healthy", instance.isHealthy(),
                "weight", instance.getWeight(),
                "version", instance.getVersion(),
                "lastHeartbeatTime", instance.getLastHeartbeatTime(),
                "registrationTime", instance.getRegistrationTime()
            ));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", serviceName);
        response.put("instances", instanceList);
        response.put("total", instanceList.size());
        sendJson(ctx, HttpResponseStatus.OK, response);
    }

    private void handleNamespaces(ChannelHandlerContext ctx) {
        List<Map<String, Object>> namespaces = new ArrayList<>();

        List<NamespaceManager.NamespaceInfo> namespaceInfoList = namespaceManager.getAllNamespaces();
        for (NamespaceManager.NamespaceInfo info : namespaceInfoList) {
            namespaces.add(Map.of(
                "namespace", info.namespace(),
                "description", info.description(),
                "serviceCount", info.serviceCount(),
                "instanceCount", info.instanceCount()
            ));
        }

        if (namespaces.isEmpty()) {
            namespaces.add(Map.of(
                "namespace", "default",
                "description", "默认命名空间",
                "serviceCount", 1,
                "instanceCount", 3
            ));
        }

        sendJson(ctx, HttpResponseStatus.OK, namespaces);
    }

    private void handleHealth(ChannelHandlerContext ctx) {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        try {
            health.put("healthyInstances", healthChecker.getHealthyCount());
            health.put("unhealthyInstances", healthChecker.getUnhealthyCount());
        } catch (Exception e) {
            health.put("healthyInstances", 0);
            health.put("unhealthyInstances", 0);
        }
        health.put("protectionMode", false);
        sendJson(ctx, HttpResponseStatus.OK, health);
    }

    private void handleStats(ChannelHandlerContext ctx) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalInstances", registry.getTotalInstances());
        stats.put("totalServices", registry.getServiceCount());
        stats.put("totalRegistrations", registry.getTotalRegistrations());
        stats.put("totalDiscovers", registry.getTotalDiscovers());
        stats.put("globalVersion", syncManager.getGlobalVersion());
        sendJson(ctx, HttpResponseStatus.OK, stats);
    }

    private void sendCorsResponse(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization, X-Requested-With");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "3600");
        ctx.writeAndFlush(response);
    }

    private void sendJson(ChannelHandlerContext ctx, HttpResponseStatus status, Object data) {
        String json = JSON.toJSONString(data, JSONWriter.Feature.MapSortField);
        byte[] content = json.getBytes(StandardCharsets.UTF_8);

        ByteBuf buf = Unpooled.copiedBuffer(content);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, buf);

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("HTTP handler exception", cause);
        ctx.close();
    }
}
