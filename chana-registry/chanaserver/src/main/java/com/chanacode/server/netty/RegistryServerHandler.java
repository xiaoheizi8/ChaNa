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

import java.util.List;

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
            namespaceManager.registerService(namespace, instance);
            cacheManager.invalidate(namespace, request.getServiceName());
            metrics.incrementRegister();

            syncManager.notifyChange(namespace, request.getServiceName(), List.of(instance), List.of(), List.of());
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
}
