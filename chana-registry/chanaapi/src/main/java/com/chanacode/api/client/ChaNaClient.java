package com.chanacode.api.client;

import com.chanacode.common.constant.MessageType;
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
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;

/**
 * ChaNa客户端SDK
 *
 * <p>用于微服务向注册中心进行服务注册与发现。
 *
 * <p>核心功能：
 * <ul>
 *   <li>服务注册 - register()</li>
 *   <li>服务注销 - deregister()</li>
 *   <li>服务发现 - discover()</li>
 *   <li>心跳保活 - startHeartbeat()</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * ChaNaClient client = new ChaNaClient("localhost", 9999);
 * client.connect();
 * client.register(instance);
 * client.startHeartbeat(5000);
 * List&lt;ServiceInstance&gt; instances = client.discover("order-service");
 * </pre>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
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

    /**
     * @methodName: connect
     * @description: 连接注册中心
     * @param: []
     * @return: void
     */
    public void connect() {
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
                            pipeline.addLast("idle", new IdleStateHandler(0, 10, 0));
                            pipeline.addLast("decoder", new RequestDecoder());
                            pipeline.addLast("encoder", new ResponseEncoder());
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
                if (msg instanceof RegistryResponse response) {
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
                logger.warn("Channel inactive");
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                logger.error("Channel exception", cause);
                ctx.close();
            }
        };
    }

    /**
     * @methodName: register
     * @description: 注册服务实例
     * @param: [instance]
     * @return: boolean
     */
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

    /**
     * @methodName: deregister
     * @description: 注销服务实例
     * @param: [instance]
     * @return: boolean
     */
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

    /**
     * @methodName: discover
     * @description: 发现服务实例
     * @param: [serviceName, namespace]
     * @return: java.util.List<com.chanacode.common.dto.ServiceInstance>
     */
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
        return List.of();
    }

    /**
     * @methodName: discover
     * @description: 发现服务实例(使用默认命名空间)
     * @param: [serviceName]
     * @return: java.util.List<com.chanacode.common.dto.ServiceInstance>
     */
    public List<ServiceInstance> discover(String serviceName) {
        return discover(serviceName, RegistryConstants.DEFAULT_NAMESPACE);
    }

    private RegistryResponse sendRequest(RegistryRequest request) throws Exception {
        CompletableFuture<RegistryResponse> future = new CompletableFuture<>();
        pendingRequests.put(request.getRequestId(), future);
        channel.writeAndFlush(request);
        return future.get(30, TimeUnit.SECONDS);
    }

    /**
     * @methodName: startHeartbeat
     * @description: 启动心跳
     * @param: [intervalMs]
     * @return: void
     */
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

    /**
     * @methodName: isConnected
     * @description: 是否已连接
     * @param: []
     * @return: boolean
     */
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

    private static class RequestDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (in.readableBytes() < 4) return;
            in.markReaderIndex();
            int length = in.readInt();
            if (in.readableBytes() < length - 4) {
                in.resetReaderIndex();
                return;
            }
            byte[] data = new byte[length - 4];
            in.readBytes(data);
            RegistryRequest request = JSON.parseObject(data, RegistryRequest.class, JSONReader.Feature.SupportAutoType);
            out.add(request);
        }
    }

    private static class ResponseEncoder extends MessageToByteEncoder<RegistryResponse> {
        @Override
        protected void encode(ChannelHandlerContext ctx, RegistryResponse msg, ByteBuf out) {
            byte[] data = JSON.toJSONBytes(msg, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.FieldBased);
            out.writeInt(4 + data.length);
            out.writeBytes(data);
        }
    }
}
