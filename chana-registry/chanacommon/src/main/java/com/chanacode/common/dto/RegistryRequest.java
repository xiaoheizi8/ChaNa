package com.chanacode.common.dto;

import lombok.*;
import java.util.Map;

import static com.chanacode.common.constant.MessageType.*;

/**
 * 注册中心请求消息封装
 *
 * <p>客户端向服务端发送的所有请求都使用此对象封装，
 * 包含请求类型、服务信息、实例数据等。
 *
 * <p>支持的消息类型：
 * <ul>
 *   <li>{@link com.chanacode.common.constant.MessageType#REGISTER} - 服务注册</li>
 *   <li>{@link com.chanacode.common.constant.MessageType#DEREGISTER} - 服务注销</li>
 *   <li>{@link com.chanacode.common.constant.MessageType#HEARTBEAT} - 心跳保活</li>
 *   <li>{@link com.chanacode.common.constant.MessageType#DISCOVER} - 服务发现</li>
 * </ul>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryRequest {
    
    /** 请求ID */
    private long requestId;
    
    /** 消息类型 */
    private byte type;
    
    /** 服务名称 */
    private String serviceName;
    
    /** 命名空间 */
    private String namespace;
    
    /** 实例ID */
    private String instanceId;
    
    /** 服务实例 */
    private ServiceInstance instance;
    
    /** 元数据 */
    private Map<String, String> metadata;
    
    /** 时间戳 */
    private long timestamp;
    
    /** 追踪ID */
    private String traceId;

    /**
     * @methodName: register
     * @description: 创建注册请求
     * @param: [instance]
     * @return: RegistryRequest
     */
    public static RegistryRequest register(ServiceInstance instance) {
        String ns = instance.getNamespace() != null ? instance.getNamespace() : "default";
        return RegistryRequest.builder()
                .type(REGISTER)
                .instance(instance)
                .serviceName(instance.getServiceName())
                .namespace(ns)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * @methodName: heartbeat
     * @description: 创建心跳请求
     * @param: [instanceId, serviceName, namespace]
     * @return: RegistryRequest
     */
    public static RegistryRequest heartbeat(String instanceId, String serviceName, String namespace) {
        return RegistryRequest.builder()
                .type(HEARTBEAT)
                .instanceId(instanceId)
                .serviceName(serviceName)
                .namespace(namespace != null ? namespace : "default")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * @methodName: discover
     * @description: 创建发现请求
     * @param: [serviceName, namespace]
     * @return: RegistryRequest
     */
    public static RegistryRequest discover(String serviceName, String namespace) {
        return RegistryRequest.builder()
                .type(DISCOVER)
                .serviceName(serviceName)
                .namespace(namespace != null ? namespace : "default")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * @methodName: deregister
     * @description: 创建注销请求
     * @param: [instance, serviceName, namespace]
     * @return: RegistryRequest
     */
    public static RegistryRequest deregister(ServiceInstance instance, String serviceName, String namespace) {
        return RegistryRequest.builder()
                .type(DEREGISTER)
                .instance(instance)
                .serviceName(serviceName)
                .namespace(namespace != null ? namespace : "default")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
