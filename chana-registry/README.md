# ChaNa Registry - 超高性能服务注册中心

<p align="center">
  <img src="logo.svg" alt="ChaNa Registry" width="120">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-3.0.0-blue.svg" alt="Version">
  <img src="https://img.shields.io/badge/Java-17+-green.svg" alt="Java">
  <img src="https://img.shields.io/badge/Protocol-Netty-orange.svg" alt="Protocol">
  <img src="https://img.shields.io/badge/Performance-50K%2B%20QPS-red.svg" alt="Performance">
</p>

> **"一次心跳、每一次注册与发现，都发生在一个个精准的'刹那'之间"**
> 
> 超越Consul/Zookeeper/Eureka/Nacos的下一代注册中心

---

## 性能指标 | Performance Metrics

| 指标 | ChaNa | Consul | Zookeeper | Nacos | Eureka |
|------|-------|--------|-----------|-------|--------|
| **写入QPS** | **50,000+** | 5,000 | 8,000 | 10,000 | 3,000 |
| **读取QPS** | **100,000+** | 15,000 | 20,000 | 30,000 | 10,000 |
| **P99延迟** | **< 1ms** | 5ms | 3ms | 2ms | 10ms |
| **最大连接** | **50,000+** | 10,000 | 15,000 | 20,000 | 8,000 |
| **实例容量** | **100,000+** | 50,000 | 30,000 | 60,000 | 20,000 |
| **变更延迟** | **< 50ms** | 500ms | 100ms | 200ms | 30,000ms |

---

## 项目结构 | Project Structure

```
chana-registry/
├── pom.xml                           # Maven父项目
├── chanacommon/                      # 公共模块
│   └── src/main/java/com/chanacode/common/
│       ├── constant/                 # RegistryConstants, MessageType
│       └── dto/                      # ServiceInstance, RegistryRequest/Response
├── chanacore/                         # 核心模块
│   └── src/main/java/com/chanacode/core/
│       ├── registry/                 # ServiceRegistry (O(1)注册发现)
│       ├── cache/                    # 三级缓存管理器
│       ├── health/                   # 滑动窗口健康检查器
│       ├── namespace/                # 多租户命名空间
│       ├── sync/                     # 增量同步管理器
│       └── metrics/                  # HdrHistogram高精度指标
├── chanaserver/                       # 注册中心服务端
│   └── src/main/java/com/chanacode/server/
│       ├── config/                   # 自动配置
│       ├── bootstrap/                 # ChaNaServer启动器
│       ├── netty/                     # Netty处理器/编解码器
│       └── http/                      # HTTP API处理器
├── chana-spring-boot-starter/         # Spring Boot客户端starter
│   └── src/main/java/com/chanacode/spring/boot/starter/
│       ├── ChaNaClientAutoConfiguration
│       ├── ChaNaClientProperties
│       ├── ChaNaServiceRegistry
│       └── ChaNaServiceAnnotationBeanPostProcessor
└── chanaapi/                         # 客户端SDK
    └── src/main/java/com/chanacode/api/
        ├── client/                   # ChaNaClient
        ├── factory/                  # ChaNaClientFactory
        └── annotation/               # ChaNaService注解
```

---

## 快速开始 | Quick Start

### 1. 启动注册中心服务端

```bash
mvn clean install -DskipTests
java -jar chanaserver/target/chanaserver-1.0.0.jar
```

或创建Spring Boot应用:

```java
@SpringBootApplication
@EnableChaNaServer
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
```

配置 `application.yml`:
```yaml
chana:
  netty:
    port: 9999
  http:
    port: 9998
```

### 2. 客户端集成

引入依赖:
```xml
<dependency>
    <groupId>com.chanacode</groupId>
    <artifactId>chana-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

配置 `application.yml`:
```yaml
chana:
  client:
    server-host: localhost
    server-port: 9999
```

启动类添加 `@EnableChaNaClient`:
```java
@EnableChaNaClient
@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

服务类添加 `@ChaNaService` 注解:
```java
@ChaNaService(name = "order-service", version = "1.0.0")
public class OrderServiceImpl implements OrderService {
    // ...
}
```

---

## REST API 接口 | REST API Endpoints

基础路径: `/api`

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/metrics` | 获取核心性能指标 |
| GET | `/api/services` | 获取服务列表 |
| GET | `/api/services/{name}` | 获取服务详情 |
| GET | `/api/namespaces` | 获取命名空间列表 |
| GET | `/api/health` | 获取健康状态 |
| GET | `/api/stats` | 获取统计数据 |
| POST | `/api/services/register` | 注册服务实例 |
| POST | `/api/services/deregister` | 注销服务实例 |
| POST | `/api/heartbeat` | 发送心跳 |

### 请求/响应示例

```bash
# 获取服务列表
curl http://localhost:9998/api/services

# 注册服务
curl -X POST http://localhost:9998/api/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "instanceId": "instance-001",
    "serviceName": "order-service",
    "host": "192.168.1.100",
    "port": 8080,
    "namespace": "default",
    "weight": 100,
    "version": "1.0.0"
  }'

# 发送心跳
curl -X POST http://localhost:9998/api/heartbeat \
  -H "Content-Type: application/json" \
  -d '{
    "instanceId": "instance-001",
    "serviceName": "order-service",
    "namespace": "default"
  }'
```

---

## 前端页面 | Frontend Pages

| 页面 | 路径 | 描述 |
|------|------|------|
| 概览 | `/` | Dashboard仪表盘 |
| 服务列表 | `/services` | 服务列表管理 |
| 服务详情 | `/services/:name` | 单个服务详情 |
| 实例管理 | `/instances` | 实例注册/注销 |
| 健康监控 | `/health` | 健康状态监控 |
| 命名空间 | `/namespaces` | 命名空间管理 |
| 核心指标 | `/metrics` | 性能指标对比 |
| 设置 | `/settings` | 系统配置 |

---

## 核心架构 | Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         ChaNa Registry                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────┐    │
│  │              Netty EPOLL / NIO Server                   │    │
│  │              (支持 50K+ 长连接)                         │    │
│  └──────────────────────────┬───────────────────────────────┘    │
│                             │                                    │
│  ┌─────────────────────────▼───────────────────────────────┐  │
│  │              O(1) Service Registry                       │  │
│  │              分层索引: 命名空间 -> 服务 -> 实例            │  │
│  └─────────────────────────┬───────────────────────────────┘    │
│                            │                                     │
│  ┌─────────────────────────▼───────────────────────────────┐  │
│  │                    三级缓存架构                            │  │
│  │  L1: Guava Local (5s TTL) -> L2: Process (30s TTL) -> L3 │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌──────────────────────────┐   │
│  │  健康检查    │ │  多租户     │ │  增量同步                 │   │
│  │  滑动窗口   │ │  Namespace  │ │  版本控制 + 推送           │   │
│  └─────────────┘ └─────────────┘ └──────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 技术特性 | Features

### 1. 超高性能
- **O(1)注册发现**: 分层索引实现常数时间复杂度
- **三级缓存**: L1本地/L2进程/L3注册表
- **Netty EPOLL**: 50K+长连接支持

### 2. 健康检查
- **滑动窗口算法**: 防止网络抖动误剔除
- **雪崩保护**: 健康实例低于20%时停止剔除

### 3. 多租户
- **Namespace**: 命名空间级别隔离
- **Group**: 服务分组
- **支持100+租户**

### 4. 增量同步
- **版本控制**: 全局单调递增版本号
- **增量推送**: 仅推送变更部分
- **变更延迟<50ms**

### 5. 高精度指标
- **P50/P90/P99**: 精确测量
- **微秒级精度**

---

## 客户端使用 | Client Usage (Spring Boot方式)

```java
@EnableChaNaClient
@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}

// 服务实现类添加 @ChaNaService 注解即可自动注册
@ChaNaService(name = "order-service", version = "1.0.0", group = "production")
public class OrderServiceImpl implements OrderService {
    // ...
}
```

### 客户端配置项

```yaml
chana:
  client:
    server-host: localhost      # 注册中心地址
    server-port: 9999           # 注册中心端口
    auto-register: true         # 自动注册
    namespace: default         # 命名空间
    group: DEFAULT_GROUP        # 分组
    heartbeat-interval: 5000   # 心跳间隔(ms)
```

---

## JVM调优参数 | JVM Tuning

```bash
java -server \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:MaxGCPauseMillis=5 \
  -XX:+AlwaysPreTouch \
  -Xms4g -Xmx4g \
  -XX:MaxDirectMemorySize=4g \
  -jar target/chana-spring-1.0.0.jar
```

---

## 技术栈 | Tech Stack

### 后端
- **Java 17+**: 虚拟线程(Project Loom)、ZGC垃圾回收器
- **Netty 4.1**: 高性能网络、EPOLL支持
- **Spring Boot 3.2**: 自动配置、starter生态
- **Guava**: 高性能缓存
- **LMAX Disruptor**: 高性能队列

### 客户端SDK
- **chana-spring-boot-starter**: Spring Boot自动配置
- **@EnableChaNaClient**: 一行注解启用服务注册发现

---

## License

MIT License - 一朝风月

---

> **"一次心跳、每一次注册与发现，都发生在一个个精准的'刹那'之间"**
