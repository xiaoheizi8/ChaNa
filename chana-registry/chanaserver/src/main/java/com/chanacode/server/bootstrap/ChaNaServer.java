package com.chanacode.server.bootstrap;

import com.chanacode.common.constant.RegistryConstants;
import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.core.cache.RegistryCacheManager;
import com.chanacode.core.health.SlidingWindowHealthChecker;
import com.chanacode.core.metrics.HighPrecisionMetricsCollector;
import com.chanacode.core.namespace.NamespaceManager;
import com.chanacode.core.registry.ServiceRegistry;
import com.chanacode.core.sync.IncrementalSyncManager;
import com.chanacode.server.config.ChaNaProperties;
import com.chanacode.server.http.HttpApiHandler;
import com.chanacode.server.netty.ProtocolCodec;
import com.chanacode.server.netty.RegistryServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ChaNa注册中心服务端启动器
 *
 * <p>基于Netty的高性能服务端，支持Epoll模型，支撑50K+长连接。
 *
 * <p>启动组件：
 * <ul>
 *   <li>Netty TCP Server - 端口9999，处理注册/发现/心跳请求</li>
 *   <li>HTTP API Server - 端口9998，提供管理接口</li>
 *   <li>健康检查任务 - 每5秒检查过期实例</li>
 *   <li>指标上报任务 - 每10秒输出运行指标</li>
 * </ul>
 *
 * <p>性能目标：
 * <ul>
 *   <li>写入QPS：50,000+ /s</li>
 *   <li>读取QPS：100,000+ /s</li>
 *   <li>P99延迟：小于1ms</li>
 *   <li>最大连接：50,000+</li>
 * </ul>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@Component
public class ChaNaServer {

    private static final Logger logger = LoggerFactory.getLogger(ChaNaServer.class);

    private final ChaNaProperties properties;
    private final ServiceRegistry registry;
    private final RegistryCacheManager cacheManager;
    private final SlidingWindowHealthChecker healthChecker;
    private final NamespaceManager namespaceManager;
    private final IncrementalSyncManager syncManager;
    private final HighPrecisionMetricsCollector metrics;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private EventLoopGroup httpGroup;
    private Channel serverChannel;
    private Channel httpChannel;

    public ChaNaServer(ChaNaProperties properties,
                       ServiceRegistry registry,
                       RegistryCacheManager cacheManager,
                       SlidingWindowHealthChecker healthChecker,
                       NamespaceManager namespaceManager,
                       IncrementalSyncManager syncManager,
                       HighPrecisionMetricsCollector metrics) {
        this.properties = properties;
        this.registry = registry;
        this.cacheManager = cacheManager;
        this.healthChecker = healthChecker;
        this.namespaceManager = namespaceManager;
        this.syncManager = syncManager;
        this.metrics = metrics;
        this.scheduler = Executors.newScheduledThreadPool(3);
        this.running = new AtomicBoolean(false);
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        start(properties.getNetty().getPort());
    }

    /**
     * @methodName: start
     * @description: 启动注册中心服务
     * @param: [port]
     * @return: void
     */
    public void start(int port) {
        if (!running.compareAndSet(false, true)) {
            logger.warn("ChaNa server is already running");
            return;
        }

        boolean useEpoll = Epoll.isAvailable();
        logger.info("Starting ChaNa Registry Server on port {} (Epoll: {})", port, useEpoll);

        try {
            if (useEpoll) {
                bossGroup = new EpollEventLoopGroup(RegistryConstants.BOSS_THREADS);
                workerGroup = new EpollEventLoopGroup(RegistryConstants.EPOLL_WORKERS);
            } else {
                bossGroup = new NioEventLoopGroup(RegistryConstants.BOSS_THREADS);
                workerGroup = new NioEventLoopGroup(RegistryConstants.WORKER_THREADS);
            }

            ServerBootstrap bootstrap = new ServerBootstrap();
            RegistryServerHandler handler = new RegistryServerHandler(
                registry, cacheManager, healthChecker, namespaceManager, syncManager, metrics);

            bootstrap.group(bossGroup, workerGroup)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .channel(useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("decoder", new ProtocolCodec.RegistryRequestDecoder());
                            pipeline.addLast("requestEncoder", new ProtocolCodec.RegistryRequestEncoder());
                            pipeline.addLast("responseEncoder", new ProtocolCodec.RegistryResponseEncoder());
                            pipeline.addLast("handler", handler);
                        }
                    });

            serverChannel = bootstrap.bind(port).sync().channel();

            startHttpServer(properties.getHttp().getPort());
            startHealthCheck();
            startMetricsReporter();

            logger.info("===========================================");
            logger.info("ChaNa Registry Server started successfully!");
            logger.info("Netty Port: {}", port);
            logger.info("HTTP API Port: {}", RegistryConstants.HTTP_PORT);
            logger.info("Performance Target:");
            logger.info("  - Write QPS: 50,000+ /s");
            logger.info("  - Read QPS: 100,000+ /s");
            logger.info("  - P99 Latency: < 1ms");
            logger.info("  - Max Connections: 50,000+");
            logger.info("===========================================");
        } catch (Exception e) {
            logger.error("Failed to start ChaNa server", e);
            shutdown();
        }
    }

    /**
     * @methodName: startHttpServer
     * @description: 启动HTTP API服务器
     * @param: [port]
     * @return: void
     */
    private void startHttpServer(int port) {
        try {
            httpGroup = new NioEventLoopGroup(2);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(httpGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("codec", new HttpServerCodec());
                            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                            pipeline.addLast("handler", new HttpApiHandler(
                                    registry, cacheManager, healthChecker, namespaceManager, syncManager, metrics));
                        }
                    });

            httpChannel = bootstrap.bind(port).sync().channel();
            logger.info("HTTP API Server started on port {}", port);
        } catch (Exception e) {
            logger.error("Failed to start HTTP API server", e);
        }
    }

    /**
     * @methodName: startHealthCheck
     * @description: 启动健康检查任务
     * @param: []
     * @return: void
     */
    private void startHealthCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                for (String serviceKey : registry.getAllServices()) {
                    String[] parts = serviceKey.split(":/");
                    if (parts.length >= 2) {
                        String namespace = parts[0];
                        String serviceName = parts[1];
                        for (ServiceInstance instance : registry.discover(serviceName, namespace)) {
                            if (instance.isExpired(now)) {
                                registry.markUnhealthy(instance.getInstanceId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Health check error", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * @methodName: startMetricsReporter
     * @description: 启动指标上报任务
     * @param: []
     * @return: void
     */
    private void startMetricsReporter() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                HighPrecisionMetricsCollector.MetricsSnapshot snapshot = metrics.getSnapshot();
                logger.info("ChaNa Metrics - QPS: {}, Register: {}, Discover: {}, Connections: {}, " +
                           "AvgLatency: {:.2f}us, P99: {:.2f}us",
                    snapshot.qps(), snapshot.registerQps(), snapshot.discoverQps(),
                    snapshot.connections(), snapshot.avgLatencyUs(), snapshot.p99LatencyUs());
            } catch (Exception e) {
                logger.error("Metrics report error", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * @methodName: shutdown
     * @description: 关闭注册中心服务
     * @param: []
     * @return: void
     */
    public void shutdown() {
        if (!running.compareAndSet(true, false)) return;
        logger.info("Shutting down ChaNa Registry Server...");

        if (serverChannel != null) {
            serverChannel.close();
        }
        if (httpChannel != null) {
            httpChannel.close();
        }
        if (httpGroup != null) {
            httpGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        scheduler.shutdown();

        logger.info("ChaNa Registry Server shutdown complete");
    }

    @PreDestroy
    public void preDestroy() {
        shutdown();
    }
}
