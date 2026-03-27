package com.chanacode.spring.config;

import com.chanacode.core.cache.RegistryCacheManager;
import com.chanacode.core.health.SlidingWindowHealthChecker;
import com.chanacode.core.metrics.HighPrecisionMetricsCollector;
import com.chanacode.core.namespace.NamespaceManager;
import com.chanacode.core.registry.ServiceRegistry;
import com.chanacode.core.sync.IncrementalSyncManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChaNa 核心组件配置
 *
 * <p>将 chanacore 模块的核心组件注册为 Spring Bean，
 * 供 Controller 和 Service 层使用。
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@Configuration
public class ChaNaCoreConfig {

    @Bean
    public ServiceRegistry serviceRegistry() {
        return new ServiceRegistry();
    }

    @Bean
    public RegistryCacheManager registryCacheManager() {
        return new RegistryCacheManager();
    }

    @Bean
    public SlidingWindowHealthChecker slidingWindowHealthChecker() {
        return new SlidingWindowHealthChecker();
    }

    @Bean
    public NamespaceManager namespaceManager() {
        return new NamespaceManager();
    }

    @Bean
    public IncrementalSyncManager incrementalSyncManager() {
        return new IncrementalSyncManager();
    }

    @Bean
    public HighPrecisionMetricsCollector highPrecisionMetricsCollector() {
        return new HighPrecisionMetricsCollector();
    }
}
