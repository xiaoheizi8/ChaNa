package com.chanacode.common.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * 注册中心响应消息封装
 *
 * <p>服务端对客户端请求的响应封装，
 * 包含状态码、实例列表、版本号等响应信息。
 *
 * <p>响应状态码约定：
 * <ul>
 *   <li>200 - 成功</li>
 *   <li>400 - 请求参数错误</li>
 *   <li>404 - 服务/实例不存在</li>
 *   <li>500 - 服务器内部错误</li>
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
public class RegistryResponse {
    
    /** 请求ID */
    private long requestId;
    
    /** 状态码 */
    private int code;
    
    /** 消息 */
    private String message;
    
    /** 服务实例列表 */
    private List<ServiceInstance> instances;
    
    /** 版本号 */
    private long version;
    
    /** 扩展信息 */
    private Map<String, Object> ext;
    
    /** 时间戳 */
    private long timestamp;
    
    /** 处理时间(微秒) */
    private long processTimeUs;
    
    /** 批量服务发现结果 Map<服务名, 实例列表> */
    private Map<String, List<ServiceInstance>> batchInstances;
    
    /** 服务变更类型 (ADD/UPDATE/DELETE) */
    private String changeType;
    
    /** 订阅ID */
    private String subscriptionId;
    
    /** TTL剩余时间(秒) */
    private int ttlSeconds;

    /**
     * @methodName: success
     * @description: 成功响应
     * @param: [requestId, instances]
     * @return: RegistryResponse
     */
    public static RegistryResponse success(long requestId, List<ServiceInstance> instances) {
        return RegistryResponse.builder()
                .requestId(requestId)
                .code(200)
                .message("success")
                .instances(instances)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * @methodName: success
     * @description: 成功响应(无实例)
     * @param: [requestId]
     * @return: RegistryResponse
     */
    public static RegistryResponse success(long requestId) {
        return RegistryResponse.builder()
                .requestId(requestId)
                .code(200)
                .message("success")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * @methodName: error
     * @description: 错误响应
     * @param: [requestId, code, message]
     * @return: RegistryResponse
     */
    public static RegistryResponse error(long requestId, int code, String message) {
        return RegistryResponse.builder()
                .requestId(requestId)
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * @methodName: isSuccess
     * @description: 是否成功
     * @return: boolean
     */
    public boolean isSuccess() {
        return code == 200;
    }

    /**
     * @methodName: batchSuccess
     * @description: 批量发现成功响应
     * @param: [requestId, batchInstances]
     * @return: RegistryResponse
     */
    public static RegistryResponse batchSuccess(long requestId, Map<String, List<ServiceInstance>> batchInstances) {
        return RegistryResponse.builder()
                .requestId(requestId)
                .code(200)
                .message("success")
                .batchInstances(batchInstances)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * @methodName: push
     * @description: 创建推送响应
     * @param: [subscriptionId, instances, changeType]
     * @return: RegistryResponse
     */
    public static RegistryResponse push(String subscriptionId, List<ServiceInstance> instances, String changeType) {
        return RegistryResponse.builder()
                .code(200)
                .message("push")
                .subscriptionId(subscriptionId)
                .instances(instances)
                .changeType(changeType)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * @methodName: leaseResponse
     * @description: 租约响应
     * @param: [requestId, ttlSeconds]
     * @return: RegistryResponse
     */
    public static RegistryResponse leaseResponse(long requestId, int ttlSeconds) {
        return RegistryResponse.builder()
                .requestId(requestId)
                .code(200)
                .message("lease renewed")
                .ttlSeconds(ttlSeconds)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
