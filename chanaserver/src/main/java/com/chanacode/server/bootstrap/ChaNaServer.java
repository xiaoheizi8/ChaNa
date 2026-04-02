package com.chanacode.server.bootstrap;

import com.chanacode.core.registry.ServiceRegistry;
import com.chanacode.server.config.ChaNaProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaNaServer {

    private static final Logger logger = LoggerFactory.getLogger(ChaNaServer.class);
    
    private final ChaNaProperties properties;
    private final ServiceRegistry serviceRegistry;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel serverChannel;

    public ChaNaServer(ChaNaProperties properties, ServiceRegistry serviceRegistry) {
        this.properties = properties;
        this.serviceRegistry = serviceRegistry;
    }

    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(properties.getWorkerThreads());

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new com.chanacode.server.netty.ProtocolCodec());
                        pipeline.addLast(new com.chanacode.server.netty.RegistryServerHandler(serviceRegistry));
                    }
                });

        int port = properties.getPort();
        serverChannel = bootstrap.bind(port).sync().channel();
        logger.info("ChaNa Server started on port {}", port);
    }

    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        logger.info("ChaNa Server stopped");
    }
}
