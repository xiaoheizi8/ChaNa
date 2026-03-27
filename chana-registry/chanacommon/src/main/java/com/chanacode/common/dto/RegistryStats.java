package com.chanacode.common.dto;

import lombok.*;

/**
 * 注册中心运行时统计信息
 *
 * <p>用于向管理界面或监控系统提供实时的运行时状态数据，
 * 包括QPS、延迟、连接数、内存使用等核心指标。
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryStats {
    
    /** 总请求数 */
    private long totalRequests;
    
    /** 总服务数 */
    private long totalServices;
    
    /** 总实例数 */
    private long totalInstances;
    
    /** 健康实例数 */
    private long healthyInstances;
    
    /** 不健康实例数 */
    private long unhealthyInstances;
    
    /** 平均处理时间(微秒) */
    private long avgProcessTimeUs;
    
    /** 最大处理时间(微秒) */
    private long maxProcessTimeUs;
    
    /** QPS */
    private long qps;
    
    /** 连接数 */
    private long connections;
    
    /** 事件队列大小 */
    private long eventQueueSize;
    
    /** 内存使用MB */
    private long memoryUsedMb;
    
    /** 运行时间(秒) */
    private long uptimeSeconds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStats {
        private String serviceName;
        private int instanceCount;
        private long requestsPerSecond;
        private double avgLatencyUs;
    }
}
