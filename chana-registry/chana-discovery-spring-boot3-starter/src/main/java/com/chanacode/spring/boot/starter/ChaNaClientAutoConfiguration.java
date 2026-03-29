package com.chanacode.spring.boot.starter;

import com.chanacode.api.client.ChaNaClient;
import com.chanacode.api.factory.ChaNaClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * ChaNa客户端自动配置类
 *
 * <p>Spring Boot自动配置类，用于配置ChaNa服务注册中心客户端。
 * 当启用{@code chana.client.enabled=true}时自动配置。
 *
 * <p>设计模式:
 * <ul>
 *   <li>单例模式 - ChaNaClient全局唯一</li>
 *   <li>工厂模式 - 使用工厂创建客户端</li>
 *   <li>条件装配 - 支持开关控制</li>
 * </ul>
 *
 * <p>自动配置的Bean:
 * <ul>
 *   <li>{@link ChaNaClient} - ChaNa客户端</li>
 *   <li>{@link ChaNaServiceRegistry} - 服务注册表</li>
 *   <li>{@link ChaNaServiceAnnotationBeanPostProcessor} - 注解处理器</li>
 * </ul>
 *
 * @author ChaNa Team
 * @version 3.0.0
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ChaNaClientProperties.class)
@ConditionalOnProperty(prefix = "chana.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChaNaClientAutoConfiguration {

    /**
     * 创建ChaNaClient实例
     *
     * <p>如果不存在ChaNaClient Bean，则创建一个新的实例。
     * 客户端会根据配置自动连接到注册中心。
     *
     * @param properties 客户端配置
     * @return ChaNaClient实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ChaNaClient chaNaClient(ChaNaClientProperties properties) {
        log.info("ChaNa: Initializing ChaNaClient - server: {}:{}, namespace: {}",
                properties.getServerHost(), 
                properties.getServerPort(),
                properties.getNamespace());

        ChaNaClient client = ChaNaClientFactory.createClient(
                properties.getServerHost(),
                properties.getServerPort(),
                properties.getConnectTimeout(),
                properties.getReadTimeout()
        );
        
        try {
            client.connect();
            log.info("ChaNa: Client connected successfully");
        } catch (Exception e) {
            log.error("ChaNa: Failed to connect to server", e);
            throw new RuntimeException("Failed to connect to ChaNa server", e);
        }

        if (properties.isAutoRegister()) {
            client.startHeartbeat(properties.getHeartbeatInterval());
            log.info("ChaNa: Auto heartbeat enabled - interval: {}ms", properties.getHeartbeatInterval());
        }
        
        return client;
    }

    /**
     * 创建ChaNaServiceRegistry实例
     *
     * <p>服务注册表负责服务的注册与发现功能
     *
     * @param client     ChaNaClient
     * @param properties 客户端配置
     * @return ChaNaServiceRegistry实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChaNaClient.class)
    public ChaNaServiceRegistry chaNaServiceRegistry(ChaNaClient client, 
                                                       ChaNaClientProperties properties) {
        log.info("ChaNa: Creating ChaNaServiceRegistry");
        return ChaNaServiceRegistry.getInstance(client, properties);
    }

    /**
     * 创建ChaNaServiceAnnotationBeanPostProcessor实例
     *
     * <p>用于处理@ChaNaService注解，实现自动注册
     *
     * @param registry ChaNaServiceRegistry
     * @return ChaNaServiceAnnotationBeanPostProcessor实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChaNaServiceRegistry.class)
    public ChaNaServiceAnnotationBeanPostProcessor chaNaServiceAnnotationBeanPostProcessor(
            ChaNaServiceRegistry registry) {
        log.info("ChaNa: Creating ChaNaServiceAnnotationBeanPostProcessor");
        return new ChaNaServiceAnnotationBeanPostProcessor(registry);
    }

    /**
     * 创建ChaNaClientFactory实例
     *
     * <p>用于创建多个ChaNaClient实例
     *
     * @return ChaNaClientFactory实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ChaNaClientFactory chaNaClientFactory() {
        log.info("ChaNa: Creating ChaNaClientFactory");
        return new ChaNaClientFactory();
    }
}
