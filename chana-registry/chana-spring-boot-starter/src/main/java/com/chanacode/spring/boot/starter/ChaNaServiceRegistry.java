package com.chanacode.spring.boot.starter;

import com.chanacode.api.annotation.ChaNaService;
import com.chanacode.api.client.ChaNaClient;
import com.chanacode.common.dto.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * ChaNa服务注册表
 *
 * <p>负责服务注册与发现的核心类，提供服务注册、注销、发现等功能。
 * 使用单例模式确保全局只有一个注册表实例。
 *
 * <p>设计模式:
 * <ul>
 *   <li>单例模式 - 全局唯一注册表</li>
 *   <li>外观模式 - 封装底层客户端操作</li>
 *   <li>策略模式 - 支持自定义服务实例构建策略</li>
 * </ul>
 *
 * @author ChaNa Team
 * @version 3.0.0
 * @since 3.0.0
 */
@Slf4j
public class ChaNaServiceRegistry {

    /**
     * 服务实例构建策略接口
     */
    @FunctionalInterface
    public interface ServiceInstanceBuilder {
        /**
         * 构建服务实例
         *
         * @param serviceName 服务名称
         * @param version    版本
         * @param group      分组
         * @param weight     权重
         * @return 服务实例
         */
        ServiceInstance build(String serviceName, String version, String group, int weight);
    }

    private static ChaNaServiceRegistry instance;

    private final ChaNaClient client;
    private final ChaNaClientProperties properties;
    private final ConcurrentHashMap<String, ServiceInstanceBuilder> customBuilders = new ConcurrentHashMap<>();

    /**
     * 私有构造函数，防止外部直接实例化
     *
     * @param client     ChaNa客户端
     * @param properties 客户端配置
     */
    private ChaNaServiceRegistry(ChaNaClient client, ChaNaClientProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * 获取单例实例
     *
     * @param client     ChaNa客户端
     * @param properties 客户端配置
     * @return 注册表实例
     */
    public static synchronized ChaNaServiceRegistry getInstance(ChaNaClient client, ChaNaClientProperties properties) {
        if (instance == null) {
            instance = new ChaNaServiceRegistry(client, properties);
        }
        return instance;
    }

    /**
     * 获取单例实例（如果已存在）
     *
     * @return 注册表实例，如果未初始化则返回null
     */
    public static ChaNaServiceRegistry getInstance() {
        return instance;
    }

    /**
     * 注册服务实例
     *
     * <p>根据提供的参数构建服务实例并注册到服务中心。
     * 自动获取本机IP地址和服务器端口。
     *
     * @param serviceName 服务名称
     * @param version     服务版本
     * @param group       服务分组
     * @param weight      服务权重
     * @return 注册成功返回true，否则返回false
     */
    public boolean registerService(String serviceName, String version, String group, int weight) {
        try {
            ServiceInstance instance = buildServiceInstance(serviceName, version, group, weight);
            boolean success = client.register(instance);
            
            if (success) {
                log.info("ChaNa: Service registered successfully - {}://{}:{} (version: {}, group: {})",
                        serviceName, instance.getHost(), instance.getPort(), version, group);
            } else {
                log.warn("ChaNa: Failed to register service - {}", serviceName);
            }
            return success;
        } catch (Exception e) {
            log.error("ChaNa: Error registering service - {}", serviceName, e);
            return false;
        }
    }

    /**
     * 使用@ChaNaService注解注册服务
     *
     * @param serviceClass 服务类
     * @param annotation   注解信息
     */
    public void registerService(Class<?> serviceClass, ChaNaService annotation) {
        String serviceName = annotation.name().isEmpty() 
                ? serviceClass.getSimpleName() 
                : annotation.name();
        registerService(serviceName, annotation.version(), annotation.group(), annotation.weight());
    }

    /**
     * 注销服务实例
     *
     * @param serviceName 服务名称
     * @param instanceId   实例ID
     * @return 注销成功返回true，否则返回false
     */
    public boolean deregisterService(String serviceName, String instanceId) {
        try {
            ServiceInstance instance = ServiceInstance.builder()
                    .instanceId(instanceId)
                    .serviceName(serviceName)
                    .namespace(properties.getNamespace())
                    .build();
            
            boolean success = client.deregister(instance);
            if (success) {
                log.info("ChaNa: Service deregistered - {}:{}", serviceName, instanceId);
            }
            return success;
        } catch (Exception e) {
            log.error("ChaNa: Error deregistering service - {}:{}", serviceName, instanceId, e);
            return false;
        }
    }

    /**
     * 发现服务实例列表
     *
     * <p>使用默认命名空间发现服务
     *
     * @param serviceName 服务名称
     * @return 服务实例列表
     */
    public List<ServiceInstance> discoverService(String serviceName) {
        return discoverService(serviceName, properties.getNamespace());
    }

    /**
     * 发现服务实例列表（指定命名空间）
     *
     * @param serviceName 服务名称
     * @param namespace   命名空间
     * @return 服务实例列表
     */
    public List<ServiceInstance> discoverService(String serviceName, String namespace) {
        try {
            return client.discover(serviceName, namespace);
        } catch (Exception e) {
            log.error("ChaNa: Error discovering service - {} in namespace {}", serviceName, namespace, e);
            return List.of();
        }
    }

    /**
     * 批量发现服务
     *
     * @param serviceNames 服务名称列表
     * @param namespace    命名空间
     * @return 服务名称到实例列表的映射
     */
    public java.util.Map<String, List<ServiceInstance>> batchDiscover(
            List<String> serviceNames, String namespace) {
        try {
            return client.batchDiscover(serviceNames, namespace);
        } catch (Exception e) {
            log.error("ChaNa: Error batch discovering services", e);
            return java.util.Map.of();
        }
    }

    /**
     * 注册自定义服务实例构建器
     *
     * <p>允许用户自定义服务实例的构建逻辑
     *
     * @param name    构建器名称
     * @param builder 构建器
     */
    public void registerCustomBuilder(String name, ServiceInstanceBuilder builder) {
        customBuilders.put(name, builder);
    }

    /**
     * 使用自定义构建器构建并注册服务
     *
     * @param builderName 构建器名称
     * @return 注册成功返回true，否则返回false
     */
    public boolean registerWithCustomBuilder(String builderName) {
        ServiceInstanceBuilder builder = customBuilders.get(builderName);
        if (builder == null) {
            log.warn("ChaNa: Custom builder not found - {}", builderName);
            return false;
        }
        
        ServiceInstance instance = builder.build(
                "custom-service",
                properties.getVersion(),
                properties.getGroup(),
                properties.getWeight()
        );
        return client.register(instance);
    }

    /**
     * 获取ChaNa客户端实例
     *
     * @return ChaNa客户端
     */
    public ChaNaClient getClient() {
        return client;
    }

    /**
     * 检查是否已连接
     *
     * @return 已连接返回true，否则返回false
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * 构建服务实例
     *
     * @param serviceName 服务名称
     * @param version     版本
     * @param group       分组
     * @param weight      权重
     * @return 服务实例
     */
    private ServiceInstance buildServiceInstance(String serviceName, String version, String group, int weight) {
        String host = getHostAddress();
        int port = getServerPort();
        
        return ServiceInstance.builder()
                .instanceId(generateInstanceId(serviceName, host, port))
                .serviceName(serviceName)
                .host(host)
                .port(port)
                .namespace(properties.getNamespace())
                .group(group)
                .weight(weight)
                .version(version)
                .build();
    }

    /**
     * 获取本机IP地址
     *
     * @return IP地址
     */
    private String getHostAddress() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("ChaNa: Failed to get local host address, using 127.0.0.1");
            return "127.0.0.1";
        }
    }

    /**
     * 获取服务器端口
     *
     * @return 服务器端口
     */
    private int getServerPort() {
        String port = System.getProperty("server.port", "8080");
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            log.warn("ChaNa: Invalid server.port, using default 8080");
            return 8080;
        }
    }

    /**
     * 生成实例ID
     *
     * @param serviceName 服务名称
     * @param host        主机地址
     * @param port        端口
     * @return 实例ID
     */
    private String generateInstanceId(String serviceName, String host, int port) {
        return String.format("%s-%s-%d-%s", 
                serviceName, host, port, 
                UUID.randomUUID().toString().substring(0, 8));
    }
}
