package com.chanacode.server.config;

import com.chanacode.core.registry.ServiceRegistry;
import com.chanacode.server.bootstrap.ChaNaServer;
import com.chanacode.server.http.HttpApiHandler;
import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
@EnableConfigurationProperties(ChaNaProperties.class)
public class ChaNaServerAutoConfiguration {

    @Bean
    public ServiceRegistry serviceRegistry() {
        return new ServiceRegistry();
    }

    @Bean
    public ChaNaServer chaNaServer(ChaNaProperties properties, ServiceRegistry serviceRegistry) {
        return new ChaNaServer(properties, serviceRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "chana.server.http-enabled", havingValue = "true", matchIfMissing = true)
    public HttpServer httpServer(ChaNaProperties properties, ServiceRegistry serviceRegistry) throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(properties.getHttpPort()), 0);
        httpServer.createContext("/api/", new HttpApiHandler(serviceRegistry));
        httpServer.setExecutor(null);
        httpServer.start();
        return httpServer;
    }
}
