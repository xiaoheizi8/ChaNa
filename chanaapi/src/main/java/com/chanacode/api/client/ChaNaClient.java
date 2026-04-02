package com.chanacode.api.client;

import com.chanacode.common.constant.RegistryConstants;
import com.chanacode.common.dto.RegistryRequest;
import com.chanacode.common.dto.RegistryResponse;
import com.chanacode.common.dto.ServiceInstance;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;

public class ChaNaClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ChaNaClient.class);

    private final String host;
    private final int port;
    private final EventLoopGroup group;
    private volatile Channel channel;
    private final ConcurrentHashMap<Long, CompletableFuture<RegistryResponse>> pendingRequests;
    private final AtomicLong requestIdGenerator;
    private final ScheduledExecutorService heartbeatScheduler;
    private final CopyOnWriteArrayList<ServiceInstance> registeredInstances;
    private volatile boolean connected;

    public ChaNaClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.group = new NioEventLoopGroup(4);
        this.pendingRequests = new ConcurrentHashMap<>();
        this.requestIdGenerator = new AtomicLong(0);
        this.heartbeatScheduler = Executors.newScheduledThreadPool(2);
        this.registeredInstances = new CopyOnWriteArrayList<>();
        this.connected = false;
    }

    public synchronized void connect() {
        if (connected && channel != null && channel.isActive()) {
            return;
        }
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("decoder", new InboundResponseDecoder());
                            pipeline.addLast("encoder", new OutboundRequestEncoder());
                            pipeline.addLast("handler", createClientHandler());
                        }
                    });

            this.channel = bootstrap.connect(host, port).sync().channel();
            this.connected = true;
            logger.info("Connected to ChaNa registry at {}:{}", host, port);
        } catch (Exception e) {
            logger.error("Failed to connect to ChaNa registry", e);
            throw new RuntimeException("Failed to connect to registry", e);
        }
    }

    private ChannelInboundHandlerAdapter createClientHandler() {
        return new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof RegistryResponse) {
                    RegistryResponse response = (RegistryResponse) msg;
                    CompletableFuture<RegistryResponse> future = pendingRequests.remove(response.getRequestId());
                    if (future != null) {
                        future.complete(response);
                    }
                }
            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                connected = true;
                logger.info("Channel active");
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                connected = false;
                IOException ex = new IOException("ChaNa registry connection closed");
                List<CompletableFuture<RegistryResponse>> snapshot = new ArrayList<>(pendingRequests.values());
                pendingRequests.clear();
                for (CompletableFuture<RegistryResponse> f : snapshot) {
                    f.completeExceptionally(ex);
                }
                logger.warn("Channel inactive, pending RPCs failed: {}", ex.getMessage());
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                logger.error("Channel exception", cause);
                ctx.close();
            }
        };
    }

    public boolean register(ServiceInstance instance) {
        if (!connected) {
            connect();
        }

        long requestId = requestIdGenerator.incrementAndGet();
        RegistryRequest request = RegistryRequest.register(instance);
        request.setRequestId(requestId);

        try {
            RegistryResponse response = sendRequest(request);
            if (response.isSuccess()) {
                registeredInstances.add(instance);
                logger.info("Service registered: {} - {}", instance.getServiceName(), instance.getInstanceId());
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to register service: {}", instance.getServiceName(), e);
        }
        return false;
    }

    public boolean deregister(ServiceInstance instance) {
        if (!connected) {
            return false;
        }

        long requestId = requestIdGenerator.incrementAndGet();
        RegistryRequest request = RegistryRequest.deregister(instance, instance.getServiceName(), instance.getNamespace());
        request.setRequestId(requestId);

        try {
            RegistryResponse response = sendRequest(request);
            if (response.isSuccess()) {
                registeredInstances.remove(instance);
                logger.info("Service deregistered: {}", instance.getServiceName());
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to deregister service: {}", instance.getServiceName(), e);
        }
        return false;
    }

    public List<ServiceInstance> discover(String serviceName, String namespace) {
        if (!connected) {
            connect();
        }

        long requestId = requestIdGenerator.incrementAndGet();
        RegistryRequest request = RegistryRequest.discover(serviceName, namespace);
        request.setRequestId(requestId);

        try {
            RegistryResponse response = sendRequest(request);
            if (response.isSuccess()) {
                return response.getInstances();
            }
        } catch (Exception e) {
            logger.error("Failed to discover service: {}", serviceName, e);
        }
        return new ArrayList<>();
    }

    public List<ServiceInstance> discover(String serviceName) {
        return discover(serviceName, RegistryConstants.DEFAULT_NAMESPACE);
    }

    private RegistryResponse sendRequest(RegistryRequest request) throws Exception {
        CompletableFuture<RegistryResponse> future = new CompletableFuture<>();
        pendingRequests.put(request.getRequestId(), future);
        channel.writeAndFlush(request);
        return future.get(30, TimeUnit.SECONDS);
    }

    public void startHeartbeat(long intervalMs) {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            for (ServiceInstance instance : registeredInstances) {
                long requestId = requestIdGenerator.incrementAndGet();
                RegistryRequest request = RegistryRequest.heartbeat(
                    instance.getInstanceId(),
                    instance.getServiceName(),
                    instance.getNamespace()
                );
                request.setRequestId(requestId);
                channel.writeAndFlush(request);
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public int batchRegister(List<ServiceInstance> instances) {
        if (!connected) {
            connect();
        }

        long requestId = requestIdGenerator.incrementAndGet();
        RegistryRequest request = RegistryRequest.batchRegister(instances);
        request.setRequestId(requestId);

        try {
            RegistryResponse response = sendRequest(request);
            if (response.isSuccess() && response.getExt() != null) {
                Object count = response.getExt().get("successCount");
                int successCount = count != null ? ((Number) count).intValue() : 0;
                registeredInstances.addAll(instances);
                return successCount;
            }
        } catch (Exception e) {
            logger.error("Failed to batch register services", e);
        }
        return 0;
    }

    public Map<String, List<ServiceInstance>> batchDiscover(List<String> serviceNames, String namespace) {
        if (!connected) {
            connect();
        }

        long requestId = requestIdGenerator.incrementAndGet();
        RegistryRequest request = RegistryRequest.batchDiscover(serviceNames, namespace);
        request.setRequestId(requestId);

        try {
            RegistryResponse response = sendRequest(request);
            if (response.isSuccess() && response.getBatchInstances() != null) {
                return response.getBatchInstances();
            }
        } catch (Exception e) {
            logger.error("Failed to batch discover services", e);
        }
        return new ConcurrentHashMap<>();
    }

    public boolean updateMetadata(String instanceId, Map<String, String> metadata) {
        if (!connected) {
            return false;
        }

        long requestId = requestIdGenerator.incrementAndGet();
        RegistryRequest request = RegistryRequest.metadataUpdate(instanceId, metadata);
        request.setRequestId(requestId);

        try {
            RegistryResponse response = sendRequest(request);
            return response.isSuccess();
        } catch (Exception e) {
            logger.error("Failed to update metadata for instance: {}", instanceId, e);
        }
        return false;
    }

    public boolean renewLease(String instanceId, int ttlSeconds) {
        if (!connected) {
            return false;
        }

        long requestId = requestIdGenerator.incrementAndGet();
        RegistryRequest request = RegistryRequest.leaseRenew(instanceId, ttlSeconds);
        request.setRequestId(requestId);

        try {
            RegistryResponse response = sendRequest(request);
            return response.isSuccess();
        } catch (Exception e) {
            logger.error("Failed to renew lease for instance: {}", instanceId, e);
        }
        return false;
    }

    public boolean isConnected() {
        return connected && channel != null && channel.isActive();
    }

    @Override
    public void close() {
        for (ServiceInstance instance : registeredInstances) {
            deregister(instance);
        }
        heartbeatScheduler.shutdown();
        group.shutdownGracefully();
        connected = false;
        logger.info("ChaNa client closed");
    }

    private static class InboundResponseDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (in.readableBytes() < 4) {
                return;
            }
            in.markReaderIndex();
            int length = in.readInt();
            if (in.readableBytes() < length - 4) {
                in.resetReaderIndex();
                return;
            }
            byte[] data = new byte[length - 4];
            in.readBytes(data);
            try {
                RegistryResponse response = JSON.parseObject(data, RegistryResponse.class);
                if (response != null) {
                    out.add(response);
                }
            } catch (Exception e) {
                logger.error("InboundResponseDecoder: invalid JSON frame ({} bytes): {}", data.length, e.getMessage());
                throw e;
            }
        }
    }

    private static class OutboundRequestEncoder extends MessageToByteEncoder<RegistryRequest> {
        @Override
        protected void encode(ChannelHandlerContext ctx, RegistryRequest msg, ByteBuf out) {
            String json = JSON.toJSONString(msg);
            byte[] data = json.getBytes();
            out.writeInt(4 + data.length);
            out.writeBytes(data);
        }
    }
}
