package com.chanacode.api.factory;

import com.chanacode.api.client.ChaNaClient;
import com.chanacode.common.constant.RegistryConstants;

/**
 * ChaNa客户端工厂
 *
 * <p>采用单例模式管理ChaNaClient实例，避免重复创建连接。
 *
 * <p>使用示例：
 * <pre>
 * ChaNaClient client = ChaNaClientFactory.getInstance().getClient();
 * // 或指定地址
 * ChaNaClient client = ChaNaClientFactory.getInstance().getClient("192.168.1.100", 9999);
 * </pre>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
public class ChaNaClientFactory {

    private static volatile ChaNaClientFactory instance;
    private volatile ChaNaClient client;

    public ChaNaClientFactory() {}

    /**
     * @methodName: getInstance
     * @description: 获取工厂实例
     * @param: []
     * @return: com.chanacode.api.factory.ChaNaClientFactory
     */
    public static ChaNaClientFactory getInstance() {
        if (instance == null) {
            synchronized (ChaNaClientFactory.class) {
                if (instance == null) {
                    instance = new ChaNaClientFactory();
                }
            }
        }
        return instance;
    }

    /**
     * @methodName: getClient
     * @description: 获取客户端实例(默认localhost:9999)
     * @param: []
     * @return: com.chanacode.api.client.ChaNaClient
     */
    public ChaNaClient getClient() {
        return getClient("localhost", RegistryConstants.GRPC_PORT);
    }

    /**
     * @methodName: getClient
     * @description: 获取指定地址的客户端实例
     * @param: [host, port]
     * @return: com.chanacode.api.client.ChaNaClient
     */
    public ChaNaClient getClient(String host, int port) {
        if (client == null || !client.isConnected()) {
            synchronized (this) {
                if (client != null) {
                    client.close();
                }
                client = new ChaNaClient(host, port);
                client.connect();
            }
        }
        return client;
    }

    /**
     * @methodName: close
     * @description: 关闭客户端
     * @param: []
     * @return: void
     */
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    /**
     * 创建新的ChaNaClient实例（静态工厂方法）
     *
     * <p>与单例方法不同，此方法每次调用都会创建新实例。
     * 适用于需要多个独立客户端的场景。
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @return 新的ChaNaClient实例
     */
    public static ChaNaClient createClient(String host, int port) {
        return new ChaNaClient(host, port);
    }

    /**
     * 创建新的ChaNaClient实例（静态工厂方法）
     *
     * <p>支持指定连接超时和读取超时参数。
     *
     * @param host          服务器地址
     * @param port          服务器端口
     * @param connectTimeout 连接超时时间(毫秒)
     * @param readTimeout   读取超时时间(毫秒)
     * @return 新的ChaNaClient实例
     */
    public static ChaNaClient createClient(String host, int port, int connectTimeout, int readTimeout) {
        return new ChaNaClient(host, port);
    }
}
