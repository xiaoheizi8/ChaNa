package com.chanacode.server.config;

import com.chanacode.core.cache.RegistryCacheManager;
import com.chanacode.core.health.SlidingWindowHealthChecker;
import com.chanacode.core.metrics.HighPrecisionMetricsCollector;
import com.chanacode.core.namespace.NamespaceManager;
import com.chanacode.core.registry.ServiceRegistry;
import com.chanacode.core.sync.IncrementalSyncManager;
import com.chanacode.server.bootstrap.ChaNaServer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ChaNaProperties.class)
public class ChaNaServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServiceRegistry serviceRegistry() {
        return new ServiceRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistryCacheManager registryCacheManager() {
        return new RegistryCacheManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public SlidingWindowHealthChecker slidingWindowHealthChecker() {
        return new SlidingWindowHealthChecker();
    }

    @Bean
    @ConditionalOnMissingBean
    public NamespaceManager namespaceManager() {
        return new NamespaceManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public IncrementalSyncManager incrementalSyncManager() {
        return new IncrementalSyncManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public HighPrecisionMetricsCollector highPrecisionMetricsCollector() {
        return new HighPrecisionMetricsCollector();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChaNaServer chaNaServer(ChaNaProperties properties,
                                    ServiceRegistry registry,
                                    RegistryCacheManager cacheManager,
                                    SlidingWindowHealthChecker healthChecker,
                                    NamespaceManager namespaceManager,
                                    IncrementalSyncManager syncManager,
                                    HighPrecisionMetricsCollector metrics) {
        return new ChaNaServer(properties, registry, cacheManager, healthChecker, namespaceManager, syncManager, metrics);
    }
}
