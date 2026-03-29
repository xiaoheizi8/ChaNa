package com.chanacode.common.constant;

/**
 * ChaNa注册中心常量定义
 *
 * <p>包含所有核心配置参数：
 * <ul>
 *   <li>网络配置 - 端口、线程数</li>
 *   <li>性能目标 - QPS、延迟指标</li>
 *   <li>健康检查 - 滑动窗口参数</li>
 *   <li>缓存配置 - 三级缓存TTL</li>
 * </ul>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
public final class RegistryConstants {

    private RegistryConstants() {}

    /** 服务端口 */
    public static final int GRPC_PORT = 9999;
    public static final int HTTP_PORT = 9998;
    public static final String HOST = "0.0.0.0";

    /** Netty线程配置 */
    public static final int BOSS_THREADS = 2;
    public static final int WORKER_THREADS = 16;
    public static final int EPOLL_WORKERS = 32;

    /** 超时配置 */
    public static final long DEFAULT_TIMEOUT_MS = 30_000;
    public static final long DEFAULT_HEARTBEAT_INTERVAL_MS = 5_000;
    public static final long DEFAULT_HEARTBEAT_TIMEOUT_MS = 15_000;

    /** 队列配置 */
    public static final int REGISTRY_QUEUE_SIZE = 102_400;
    public static final int EVENT_QUEUE_SIZE = 512_000;

    /** 容量配置 - 性能目标 */
    public static final int MAX_CONNECTIONS = 50_000;
    public static final int MAX_INSTANCES_PER_SERVICE = 10_000;
    public static final int MAX_TOTAL_INSTANCES = 100_000;
    
    /** 写入性能目标 */
    public static final int WRITE_QPS_TARGET = 50_000;
    /** 读取性能目标 */
    public static final int READ_QPS_TARGET = 100_000;
    /** 延迟目标 P99 < 5ms */
    public static final long P99_LATENCY_TARGET_US = 5_000;

    /** 健康检查配置 */
    public static final double PROTECTION_THRESHOLD = 0.2;
    public static final double UNHEALTHY_THRESHOLD = 0.3;
    public static final int HEALTH_CHECK_WINDOW_SECONDS = 30;
    public static final int HEALTH_CHECK_BUCKET_SIZE = 10;

    /** 缓存配置 */
    public static final int L1_CACHE_SIZE = 10_000;
    public static final int L1_CACHE_TTL_SECONDS = 5;
    public static final int L2_CACHE_SIZE = 50_000;
    public static final int L2_CACHE_TTL_SECONDS = 30;

    /** 命名空间默认值 */
    public static final String DEFAULT_NAMESPACE = "default";
    public static final String DEFAULT_GROUP = "DEFAULT_GROUP";
}
