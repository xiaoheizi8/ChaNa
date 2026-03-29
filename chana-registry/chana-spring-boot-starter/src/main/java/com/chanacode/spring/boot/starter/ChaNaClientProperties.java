package com.chanacode.spring.boot.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ChaNa客户端配置属性
 *
 * <p>用于配置ChaNa服务注册中心客户端的各种参数。
 * 所有配置项以 {@code chana.client} 为前缀。
 *
 * <p>配置示例:
 * <pre>
 * chana:
 *   client:
 *     server-host: localhost
 *     server-port: 9999
 *     namespace: production
 *     heartbeat-interval: 5000
 * </pre>
 *
 * @author ChaNa Team
 * @version 3.0.0
 * @since 3.0.0
 */
@Data
@ConfigurationProperties(prefix = "chana.client")
public class ChaNaClientProperties {

    /**
     * 注册中心服务器地址
     */
    private String serverHost = "localhost";

    /**
     * 注册中心服务器端口
     */
    private int serverPort = 9999;

    /**
     * 连接超时时间(毫秒)
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时时间(毫秒)
     */
    private int readTimeout = 30000;

    /**
     * 心跳发送间隔(毫秒)
     */
    private long heartbeatInterval = 5000;

    /**
     * 是否自动注册服务
     */
    private boolean autoRegister = true;

    /**
     * 默认命名空间
     */
    private String namespace = "default";

    /**
     * 默认分组
     */
    private String group = "DEFAULT_GROUP";

    /**
     * 默认权重
     */
    private int weight = 100;

    /**
     * 默认版本
     */
    private String version = "1.0.0";

    /**
     * 服务发现配置
     */
    private Discovery discovery = new Discovery();

    /**
     * 服务发现配置
     */
    @Data
    public static class Discovery {
        /**
         * 是否启用服务发现
         */
        private boolean enabled = true;

        /**
         * 服务列表刷新间隔(毫秒)
         */
        private long refreshInterval = 30000;
    }
}
