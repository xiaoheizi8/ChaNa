# ChaNa Registry 集成指南

## 目录
1. [服务端配置](#1-服务端配置)
2. [客户端集成](#2-客户端集成)
3. [配置项说明](#3-配置项说明)

---

## 1. 服务端配置

### 1.1 快速启动

```bash
mvn clean install -DskipTests
java -jar chanaserver/target/chanaserver-1.0.0.jar
```

### 1.2 嵌入式启动

创建Spring Boot应用:

```java
@SpringBootApplication
@EnableChaNaServer
public class ChaNaRegistryApp {
    public static void main(String[] args) {
        SpringApplication.run(ChaNaRegistryApp.class, args);
    }
}
```

### 1.3 配置文件

```yaml
server:
  port: 8080

chana:
  netty:
    port: 9999
    boss-threads: 1
    worker-threads: 8
  http:
    port: 9998
  health-check:
    interval-seconds: 5
    heartbeat-timeout-seconds: 30
```

### 1.4 端口说明

| 端口 | 协议 | 用途 |
|------|------|------|
| 9998 | HTTP | REST API |
| 9999 | TCP | Netty 长连接 |

---

## 2. 客户端集成

### 2.1 引入依赖

```xml
<dependency>
    <groupId>com.chanacode</groupId>
    <artifactId>chana-discovery-spring-boot3-starter</artifactId>
    <version>3.0.0</version>
</dependency>
```

### 2.2 配置文件

```yaml
chana:
  discovery:
    server-host: localhost
    server-port: 9999
    namespace: default
    group: DEFAULT_GROUP
    heartbeat-interval: 5000
```

### 2.3 启用客户端

```java
@EnableChaNaDiscovery
@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

### 2.4 服务注册

在服务实现类添加 `@ChaNaService` 注解:

```java
@ChaNaService(name = "order-service", version = "1.0.0", group = "production")
public class OrderServiceImpl implements OrderService {
    // ...
}
```

---

## 3. 配置项说明

### 服务端配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| chana.netty.port | 9999 | Netty端口 |
| chana.netty.boss-threads | 1 | Boss线程数 |
| chana.netty.worker-threads | CPU*2 | Worker线程数 |
| chana.http.port | 9998 | HTTP API端口 |
| chana.health-check.interval-seconds | 5 | 健康检查间隔 |
| chana.health-check.heartbeat-timeout-seconds | 30 | 心跳超时 |

### 客户端配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| chana.client.server-host | localhost | 注册中心地址 |
| chana.client.server-port | 9999 | 注册中心端口 |
| chana.client.auto-register | true | 自动注册 |
| chana.client.namespace | default | 命名空间 |
| chana.client.group | DEFAULT_GROUP | 分组 |
| chana.client.heartbeat-interval | 5000 | 心跳间隔(ms) |

---

## 4. 版本兼容性

| 组件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Spring Boot | 3.x |
| Netty | 4.1.x |
