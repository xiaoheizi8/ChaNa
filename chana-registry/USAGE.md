# ChaNa Registry 3.0.0 使用指南

## 目录
1. [简介](#1-简介)
2. [快速开始](#2-快速开始)
3. [服务端部署](#3-服务端部署)
4. [客户端集成](#4-客户端集成)
5. [配置详解](#5-配置详解)
6. [API参考](#6-api参考)
7. [高级特性](#7-高级特性)
8. [最佳实践](#8-最佳实践)
9. [常见问题](#9-常见问题)

---

## 1. 简介

### 1.1 什么是ChaNa Registry

ChaNa Registry 是一个超高性能的服务注册与发现中心，专为微服务架构设计。

### 1.2 核心特性

- **超高性能**: 写入QPS 50,000+，读取QPS 100,000+
- **低延迟**: P99延迟 < 1ms
- **高并发**: 支持 50,000+ 长连接
- **多语言**: 提供Java客户端SDK
- **Spring Boot**: 原生支持Spring Boot自动配置

### 1.3 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      ChaNa Registry                         │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │
│  │   Netty     │  │   HTTP API   │  │  三级缓存       │   │
│  │   Server    │  │   Server     │  │  Manager        │   │
│  │  (9999)     │  │   (9998)     │  │                 │   │
│  └──────┬──────┘  └──────┬──────┘  └────────┬────────┘   │
│         │                │                   │             │
│  ┌──────▼────────────────▼───────────────────▼────────┐  │
│  │              Service Registry (O(1))                 │  │
│  └───────────────────────────────────────────────────────┘  │
│                          │                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐    │
│  │  健康检查    │  │  命名空间    │  │  增量同步       │    │
│  └─────────────┘  └─────────────┘  └─────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 快速开始

### 2.1 环境要求

- JDK 17+
- Maven 3.6+

### 2.2 构建项目

```bash
git clone https://github.com/chanacode/chana-registry.git
cd chana-registry
mvn clean install -DskipTests
```

### 2.3 启动服务端

```bash
java -jar chanaserver/target/chanaserver-3.0.0.jar
```

### 2.4 客户端集成（5分钟入门）

**Step 1: 添加依赖**

```xml
<dependency>
    <groupId>com.chanacode</groupId>
    <artifactId>chana-spring-boot-starter</artifactId>
    <version>3.0.0</version>
</dependency>
```

**Step 2: 配置**

```yaml
chana:
  client:
    server-host: localhost
    server-port: 9999
    namespace: default
```

**Step 3: 启用客户端**

```java
@EnableChaNaClient
@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

**Step 4: 注册服务**

```java
@ChaNaService(name = "order-service", version = "1.0.0")
public class OrderServiceImpl implements OrderService {
    // 服务实现
}
```

完成！现在你的服务已经自动注册到ChaNa注册中心了。

---

## 3. 服务端部署

### 3.1 独立部署

```bash
# 构建
mvn clean package -DskipTests -pl chanaserver

# 运行
java -jar chanaserver/target/chanaserver-3.0.0.jar

# 或指定端口
java -jar chanaserver/target/chanaserver-3.0.0.jar \
    --chana.netty.port=9999 \
    --chana.http.port=9998
```

### 3.2 嵌入式部署

```java
@SpringBootApplication
@EnableChaNaServer
public class MyRegistryApp {
    public static void main(String[] args) {
        SpringApplication.run(MyRegistryApp.class, args);
    }
}
```

### 3.3 Docker部署

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY chanaserver/target/chanaserver-3.0.0.jar app.jar
EXPOSE 9999 9998
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t chana-registry:3.0.0 .
docker run -d -p 9999:9999 -p 9998:9998 chana-registry:3.0.0
```

---

## 4. 客户端集成

### 4.1 Spring Boot Starter

#### 4.1.1 添加依赖

```xml
<dependency>
    <groupId>com.chanacode</groupId>
    <artifactId>chana-spring-boot-starter</artifactId>
    <version>3.0.0</version>
</dependency>
```

#### 4.1.2 配置application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: order-service

chana:
  client:
    enabled: true                    # 是否启用客户端
    server-host: localhost           # 注册中心地址
    server-port: 9999                # 注册中心端口
    namespace: production            # 命名空间
    group: DEFAULT_GROUP             # 分组
    auto-register: true              # 是否自动注册
    heartbeat-interval: 5000         # 心跳间隔(ms)
    connect-timeout: 5000            # 连接超时(ms)
    read-timeout: 30000              # 读取超时(ms)
```

#### 4.1.3 启用客户端

```java
@EnableChaNaClient
@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

#### 4.1.4 注册服务

**方式一: 注解自动注册**

```java
@ChaNaService(
    name = "order-service",
    version = "2.0.0",
    group = "production",
    weight = 100,
    enabled = true,
    initMethod = "init"
)
public class OrderServiceImpl implements OrderService {

    public void init() {
        // 服务初始化逻辑
    }
}
```

**方式二: 手动注册**

```java
@RestController
public class OrderController {

    @Autowired
    private ChaNaServiceRegistry registry;

    @PostMapping("/register")
    public String register() {
        registry.registerService("order-service", "1.0.0", "production", 100);
        return "registered";
    }

    @GetMapping("/discover")
    public List<ServiceInstance> discover() {
        return registry.discoverService("order-service");
    }
}
```

### 4.2 非Spring Boot使用

```java
ChaNaClient client = new ChaNaClient("localhost", 9999);
client.connect();

ServiceInstance instance = ServiceInstance.builder()
    .instanceId(UUID.randomUUID().toString())
    .serviceName("order-service")
    .host("192.168.1.100")
    .port(8080)
    .namespace("production")
    .build();

client.register(instance);
client.startHeartbeat(5000);

List<ServiceInstance> instances = client.discover("order-service", "production");

client.close();
```

---

## 5. 配置详解

### 5.1 服务端配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| chana.netty.port | 9999 | Netty TCP端口 |
| chana.netty.boss-threads | 1 | Boss线程数 |
| chana.netty.worker-threads | CPU*2 | Worker线程数 |
| chana.http.port | 9998 | HTTP API端口 |
| chana.health-check.interval-seconds | 5 | 健康检查间隔 |
| chana.health-check.heartbeat-timeout-seconds | 30 | 心跳超时时间 |

### 5.2 客户端配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| chana.client.enabled | true | 是否启用 |
| chana.client.server-host | localhost | 注册中心地址 |
| chana.client.server-port | 9999 | 注册中心端口 |
| chana.client.namespace | default | 默认命名空间 |
| chana.client.group | DEFAULT_GROUP | 默认分组 |
| chana.client.auto-register | true | 自动注册 |
| chana.client.heartbeat-interval | 5000 | 心跳间隔(ms) |
| chana.client.version | 1.0.0 | 默认版本 |
| chana.client.weight | 100 | 默认权重 |
| chana.client.connect-timeout | 5000 | 连接超时(ms) |
| chana.client.read-timeout | 30000 | 读取超时(ms) |
| chana.client.discovery.enabled | true | 启用服务发现 |
| chana.client.discovery.refresh-interval | 30000 | 刷新间隔(ms) |

---

## 6. API参考

### 6.1 HTTP API

基础路径: `http://host:9998/api`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/services | 获取服务列表 |
| GET | /api/services/{name} | 获取服务详情 |
| GET | /api/services/{name}/instances | 获取服务实例 |
| POST | /api/services/register | 注册服务 |
| POST | /api/services/deregister | 注销服务 |
| POST | /api/heartbeat | 发送心跳 |
| GET | /api/health | 健康检查 |
| GET | /api/metrics | 获取指标 |

### 6.2 客户端SDK API

```java
// 注册服务
boolean register(ServiceInstance instance)

// 注销服务
boolean deregister(ServiceInstance instance)

// 发现服务
List<ServiceInstance> discover(String serviceName, String namespace)

// 批量发现
Map<String, List<ServiceInstance>> batchDiscover(List<String> serviceNames, String namespace)

// 更新元数据
boolean updateMetadata(String instanceId, Map<String, String> metadata)

// 续约租约
boolean renewLease(String instanceId, int ttlSeconds)

// 发送心跳
void startHeartbeat(long intervalMs)

// 检查连接状态
boolean isConnected()
```

---

## 7. 高级特性

### 7.1 自定义服务实例构建器

```java
@Configuration
public class ChaNaConfig {

    @Autowired
    private ChaNaServiceRegistry registry;

    @PostConstruct
    public void init() {
        registry.registerCustomBuilder("custom", (serviceName, version, group, weight) -> {
            return ServiceInstance.builder()
                    .instanceId(UUID.randomUUID().toString())
                    .serviceName(serviceName)
                    .host(getHost())
                    .port(getPort())
                    .version(version)
                    .group(group)
                    .weight(weight)
                    .metadata(Map.of("custom-key", "custom-value"))
                    .build();
        });
    }
}
```

### 7.2 注册回调

```java
@Bean
public ChaNaServiceAnnotationBeanPostProcessor postProcessor() {
    ChaNaServiceAnnotationBeanPostProcessor processor = 
        new ChaNaServiceAnnotationBeanPostProcessor(registry);
    
    processor.setRegisterCallback((bean, beanName, annotation) -> {
        System.out.println("Service registered: " + beanName);
    });
    
    return processor;
}
```

### 7.3 多注册中心

```yaml
chana:
  client:
    server-host: ${CHANA_SERVER_HOST:localhost}
    server-port: ${CHANA_SERVER_PORT:9999}
```

### 7.4 关闭时注销服务

```java
@PreDestroy
public void cleanup() {
    registry.deregisterService("order-service", instanceId);
}
```

---

## 8. 最佳实践

### 8.1 服务命名规范

- 使用小写字母
- 使用连字符分隔: `order-service`
- 避免使用特殊字符

### 8.2 版本管理

```java
@ChaNaService(name = "order-service", version = "${app.version:1.0.0}")
public class OrderServiceImpl {}
```

### 8.3 权重设置

根据服务器性能设置权重:
```java
@ChaNaService(name = "order-service", weight = 100)  // 高性能服务器
@ChaNaService(name = "order-service", weight = 50)  // 低性能服务器
```

### 8.4 心跳间隔

- 生产环境: 5000ms
- 测试环境: 10000ms
- 开发环境: 30000ms

---

## 9. 常见问题

### Q1: 启动时报错 "Failed to connect to ChaNa server"

**解决方案:**
1. 检查服务端是否启动
2. 检查防火墙是否开放端口
3. 验证配置的主机和端口是否正确

### Q2: 服务注册成功但发现不了

**解决方案:**
1. 检查namespace是否一致
2. 检查服务是否发送心跳
3. 查看服务端日志

### Q3: 如何实现服务熔断?

**解决方案:**
1. 使用Spring Cloud Circuit Breaker
2. 在发现服务时捕获异常
3. 使用本地缓存作为降级方案

### Q4: 如何监控服务状态?

**解决方案:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

访问: `http://localhost:9998/actuator/health`

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 3.0.0 | 2026-03-28 | 全新设计，支持Spring Boot自动配置 |
| 1.0.0 | 2026-03-27 | 初始版本 |

---

> **"一次心跳、每一次注册与发现，都发生在一个个精准的'刹那'之间"**
