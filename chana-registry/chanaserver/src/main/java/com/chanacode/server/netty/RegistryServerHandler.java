package com.chanacode.server.netty;

import com.chanacode.common.constant.MessageType;
import com.chanacode.common.dto.RegistryRequest;
import com.chanacode.common.dto.RegistryResponse;
import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.core.cache.RegistryCacheManager;
import com.chanacode.core.health.SlidingWindowHealthChecker;
import com.chanacode.core.metrics.HighPrecisionMetricsCollector;
import com.chanacode.core.namespace.NamespaceManager;
import com.chanacode.core.registry.ServiceRegistry;
import com.chanacode.core.sync.IncrementalSyncManager;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChaNa Netty请求处理器
 *
 * <p>处理客户端的注册/发现/心跳请求，所有操作均为O(1)时间复杂度。
 *
 * <p>支持的请求类型：
 * <ul>
 *   <li>REGISTER - 服务注册</li>
 *   <li>DEREGISTER - 服务注销</li>
 *   <li>HEARTBEAT - 心跳保活</li>
 *   <li>DISCOVER - 服务发现</li>
 * </ul>
 *
 * <p>处理流程：
 * <ol>
 *   <li>解析请求消息</li>
 *   <li>调用对应处理器</li>
 *   <li>更新缓存和命名空间</li>
 *   <li>记录指标和增量同步</li>
 *   <li>返回响应</li>
 * </ol>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@ChannelHandler.Sharable
public class RegistryServerHandler extends SimpleChannelInboundHandler<RegistryRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RegistryServerHandler.class);

    private final ServiceRegistry registry;
    private final RegistryCacheManager cacheManager;
    private final SlidingWindowHealthChecker healthChecker;
    private final NamespaceManager namespaceManager;
    private final IncrementalSyncManager syncManager;
    private final HighPrecisionMetricsCollector metrics;

    public RegistryServerHandler(ServiceRegistry registry, RegistryCacheManager cacheManager,
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
    protected void channelRead0(ChannelHandlerContext ctx, RegistryRequest request) {
        long startTime = System.nanoTime();
        try {
            switch (request.getType()) {
                case MessageType.REGISTER -> handleRegister(ctx, request);
                case MessageType.DEREGISTER -> handleDeregister(ctx, request);
                case MessageType.HEARTBEAT -> handleHeartbeat(ctx, request);
                case MessageType.DISCOVER -> handleDiscover(ctx, request);
                case MessageType.BATCH_REGISTER -> handleBatchRegister(ctx, request);
                case MessageType.BATCH_DISCOVER -> handleBatchDiscover(ctx, request);
                case MessageType.METADATA_UPDATE -> handleMetadataUpdate(ctx, request);
                case MessageType.LEASE_RENEW -> handleLeaseRenew(ctx, request);
                case MessageType.SUBSCRIBE -> handleSubscribe(ctx, request);
                case MessageType.UNSUBSCRIBE -> handleUnsubscribe(ctx, request);
                default -> sendError(ctx, request.getRequestId(), 400, "Unknown message type");
            }
        } finally {
            long processTime = System.nanoTime() - startTime;
            metrics.recordLatency(processTime);
        }
    }

    private void handleRegister(ChannelHandlerContext ctx, RegistryRequest request) {
        ServiceInstance instance = request.getInstance();
        if (instance == null) {
            sendError(ctx, request.getRequestId(), 400, "Invalid instance");
            return;
        }

        String namespace = instance.getNamespace() != null ? instance.getNamespace() : "default";
        boolean success = registry.register(instance);

        if (success) {
            try {
                namespaceManager.registerService(namespace, instance);
                cacheManager.invalidate(namespace, request.getServiceName());
                metrics.incrementRegister();
                syncManager.notifyChange(namespace, request.getServiceName(), List.of(instance), List.of(), List.of());
            } catch (Exception e) {
                logger.error("REGISTER: post-process failed for {} (instance still in core registry)", request.getServiceName(), e);
            }
        }

        RegistryResponse response = RegistryResponse.success(request.getRequestId());
        response.setProcessTimeUs(System.nanoTime() / 1000);
        sendResponse(ctx, response);
    }

    private void handleDeregister(ChannelHandlerContext ctx, RegistryRequest request) {
        ServiceInstance instance = request.getInstance();
        String namespace = request.getNamespace() != null ? request.getNamespace() : "default";

        if (instance == null) {
            sendError(ctx, request.getRequestId(), 400, "Invalid instance");
            return;
        }

        boolean success = registry.deregister(instance.getInstanceId(), request.getServiceName(), namespace);

        if (success) {
            namespaceManager.deregisterService(namespace, instance.getInstanceId(), request.getServiceName());
            cacheManager.invalidate(namespace, request.getServiceName());
            metrics.incrementDeregister();

            syncManager.notifyChange(namespace, request.getServiceName(), List.of(), List.of(instance.getInstanceId()), List.of());
        }

        sendResponse(ctx, RegistryResponse.success(request.getRequestId()));
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, RegistryRequest request) {
        String namespace = request.getNamespace() != null ? request.getNamespace() : "default";

        boolean success = registry.heartbeat(request.getInstanceId());
        healthChecker.recordHeartbeat(request.getInstanceId(), success);
        metrics.incrementHeartbeat();

        if (!success) {
            sendError(ctx, request.getRequestId(), 404, "Instance not found");
            return;
        }

        sendResponse(ctx, RegistryResponse.success(request.getRequestId()));
    }

    private void handleDiscover(ChannelHandlerContext ctx, RegistryRequest request) {
        String namespace = request.getNamespace() != null ? request.getNamespace() : "default";

        long version = registry.getVersion(request.getServiceName(), namespace);
        List<ServiceInstance> instances = registry.discoverHealthy(request.getServiceName(), namespace);

        metrics.incrementDiscover();
        metrics.recordServiceMetrics(request.getServiceName(), System.nanoTime() / 1000);

        RegistryResponse response = RegistryResponse.success(request.getRequestId(), instances);
        response.setVersion(version);
        sendResponse(ctx, response);
    }

    private void sendResponse(ChannelHandlerContext ctx, RegistryResponse response) {
        ctx.writeAndFlush(response);
    }

    private void sendError(ChannelHandlerContext ctx, long requestId, int code, String message) {
        sendResponse(ctx, RegistryResponse.error(requestId, code, message));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        metrics.incrementConnection();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        metrics.decrementConnection();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private void handleBatchRegister(ChannelHandlerContext ctx, RegistryRequest request) {
        List<ServiceInstance> instances = request.getInstances();
        if (instances == null || instances.isEmpty()) {
            sendError(ctx, request.getRequestId(), 400, "Empty instances");
            return;
        }

        int successCount = 0;
        for (ServiceInstance instance : instances) {
            if (registry.register(instance)) {
                namespaceManager.registerService(request.getNamespace(), instance);
                successCount++;
            }
        }

        cacheManager.invalidate(request.getNamespace(), request.getServiceName());
        metrics.incrementRegister();

        RegistryResponse response = RegistryResponse.success(request.getRequestId());
        response.setExt(Map.of("successCount", successCount, "totalCount", instances.size()));
        sendResponse(ctx, response);
    }

    private void handleBatchDiscover(ChannelHandlerContext ctx, RegistryRequest request) {
        List<String> serviceNames = request.getServiceNames();
        if (serviceNames == null || serviceNames.isEmpty()) {
            sendError(ctx, request.getRequestId(), 400, "Empty service names");
            return;
        }

        Map<String, List<ServiceInstance>> batchInstances = new java.util.HashMap<>();
        for (String serviceName : serviceNames) {
            List<ServiceInstance> instances = registry.discoverHealthy(serviceName, request.getNamespace());
            batchInstances.put(serviceName, instances);
        }

        metrics.incrementDiscover();
        RegistryResponse response = RegistryResponse.batchSuccess(request.getRequestId(), batchInstances);
        sendResponse(ctx, response);
    }

    private void handleMetadataUpdate(ChannelHandlerContext ctx, RegistryRequest request) {
        String instanceId = request.getInstanceId();
        ServiceInstance instance = registry.getInstance(instanceId);
        
        if (instance == null) {
            sendError(ctx, request.getRequestId(), 404, "Instance not found");
            return;
        }

        instance.getMetadata().putAll(request.getMetadata());
        cacheManager.invalidate(request.getNamespace(), instance.getServiceName());
        
        sendResponse(ctx, RegistryResponse.success(request.getRequestId()));
    }

    private void handleLeaseRenew(ChannelHandlerContext ctx, RegistryRequest request) {
        String instanceId = request.getInstanceId();
        ServiceInstance instance = registry.getInstance(instanceId);
        
        if (instance == null) {
            sendError(ctx, request.getRequestId(), 404, "Instance not found");
            return;
        }

        instance.updateHeartbeat(System.currentTimeMillis());
        int ttlSeconds = request.getTtlSeconds() > 0 ? request.getTtlSeconds() : 30;
        
        sendResponse(ctx, RegistryResponse.leaseResponse(request.getRequestId(), ttlSeconds));
    }

    private void handleSubscribe(ChannelHandlerContext ctx, RegistryRequest request) {
        String subscriptionId = request.getNamespace() + ":/" + request.getServiceName() + ":" + ctx.channel().id();
        RegistryResponse response = RegistryResponse.success(request.getRequestId());
        response.setSubscriptionId(subscriptionId);
        sendResponse(ctx, response);
    }

    private void handleUnsubscribe(ChannelHandlerContext ctx, RegistryRequest request) {
        sendResponse(ctx, RegistryResponse.success(request.getRequestId()));
    }
}
