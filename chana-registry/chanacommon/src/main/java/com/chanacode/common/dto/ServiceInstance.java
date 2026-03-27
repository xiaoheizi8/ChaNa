package com.chanacode.common.dto;

import lombok.*;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务实例数据传输对象
 *
 * <p>表示一个微服务实例的完整信息，
 * 包含网络地址、健康状态、元数据、标签等。
 *
 * <p>实例生命周期：
 * <ol>
 *   <li>注册 - 实例首次上线时注册</li>
 *   <li>心跳 - 定期发送心跳维持活跃状态</li>
 *   <li>发现 - 被其他服务发现并调用</li>
 *   <li>注销 - 实例下线时主动注销</li>
 * </ol>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "instanceId")
public class ServiceInstance {
    
    /** 实例唯一ID */
    private String instanceId;
    
    /** 服务名称 */
    private String serviceName;
    
    /** 主机地址 */
    private String host;
    
    /** 端口号 */
    private int port;
    
    /** 服务版本 */
    private String version;
    
    /** 分组名称 */
    private String group;
    
    /** 命名空间 */
    private String namespace;
    
    /** 元数据信息 */
    private Map<String, String> metadata;
    
    /** 注册时间 */
    private long registrationTime;
    
    /** 最后心跳时间 */
    private long lastHeartbeatTime;
    
    /** 权重 */
    private int weight;
    
    /** 健康状态 */
    private volatile boolean healthy;
    
    /** 是否启用 */
    private volatile boolean enabled;
    
    /** CPU核心数 */
    private int cpuCores;
    
    /** 内存MB */
    private int memoryMb;
    
    /** 标签列表 */
    private CopyOnWriteArrayList<String> tags;

    /**
     * @methodName: isExpired
     * @description: 检查实例是否过期
     * @param: [currentTime]
     * @return: boolean
     */
    public boolean isExpired(long currentTime) {
        return (currentTime - lastHeartbeatTime) > 15_000;
    }

    /**
     * @methodName: updateHeartbeat
     * @description: 更新心跳时间
     * @param: [currentTime]
     * @return: void
     */
    public void updateHeartbeat(long currentTime) {
        this.lastHeartbeatTime = currentTime;
        this.healthy = true;
    }

    /**
     * @methodName: getAddress
     * @description: 获取地址
     * @return: java.lang.String
     */
    public String getAddress() {
        return host + ":" + port;
    }
}
