package com.chanacode.common.constant;

/**
 * ChaNa协议消息类型定义
 *
 * <p>定义客户端与服务端之间的通信协议类型，
 * 采用紧凑型二进制协议设计，支持高性能通信。
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
public final class MessageType {

    private MessageType() {}

    /** 服务注册 */
    public static final byte REGISTER = 0x01;
    
    /** 服务注销 */
    public static final byte DEREGISTER = 0x02;
    
    /** 心跳保活 */
    public static final byte HEARTBEAT = 0x03;
    
    /** 服务发现 */
    public static final byte DISCOVER = 0x04;
    
    /** 服务订阅 */
    public static final byte SUBSCRIBE = 0x05;
    
    /** 取消订阅 */
    public static final byte UNSUBSCRIBE = 0x06;
    
    /** 数据同步 */
    public static final byte SYNC = 0x07;
    
    /** 确认响应 */
    public static final byte ACK = 0x08;
    
    /** 服务变更推送 */
    public static final byte PUSH = 0x09;
    
    /** 批量注册 */
    public static final byte BATCH_REGISTER = 0x0A;
    
    /** 批量发现 */
    public static final byte BATCH_DISCOVER = 0x0B;
    
    /** 元数据更新 */
    public static final byte METADATA_UPDATE = 0x0C;
    
    /** 租约续约 */
    public static final byte LEASE_RENEW = 0x0D;
    
    /** 集群同步 */
    public static final byte CLUSTER_SYNC = 0x0E;
}
