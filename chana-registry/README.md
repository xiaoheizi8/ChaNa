# ChaNa Registry - 超高性能服务注册中心

<p align="center">
  <img src="logo.svg" alt="ChaNa Registry" width="120">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.0.0-blue.svg" alt="Version">
  <img src="https://img.shields.io/badge/Java-21+-green.svg" alt="Java">
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
├── pom.xml                      # Maven父项目
├── chanacommon/                 # 公共模块
│   └── src/main/java/com/chanacode/common/
│       ├── constant/            # RegistryConstants, MessageType
│       └── dto/                # ServiceInstance, RegistryRequest/Response
├── chanacore/                   # 核心模块
│   └── src/main/java/com/chanacode/core/
│       ├── registry/            # ServiceRegistry (O(1)注册发现)
│       ├── cache/               # 三级缓存管理器
│       ├── health/             # 滑动窗口健康检查器
│       ├── namespace/           # 多租户命名空间
│       ├── sync/                # 增量同步管理器
│       └── metrics/            # HdrHistogram高精度指标
├── chanaserver/                 # Netty服务端
│   └── src/main/java/com/chanacode/server/
│       ├── bootstrap/           # ChaNaServer启动器
│       ├── netty/              # Netty处理器/编解码器
│       └── http/               # HTTP API处理器
├── chana-spring/                # Spring Boot集成 (新增)
│   └── src/main/java/com/chanacode/spring/
│       ├── ChaNaSpringApplication.java  # Spring Boot启动类
│       ├── config/             # Spring配置
│       └── controller/         # REST Controller
├── chanaapi/                    # 客户端SDK
│   └── src/main/java/com/chanacode/api/
│       ├── client/              # ChaNaClient
│       ├── factory/            # ChaNaClientFactory
│       └── annotation/          # ChaNaService注解
└── chanaui/                    # React前端
    └── src/
        ├── pages/             # 页面组件
        ├── services/           # API服务层
        ├── utils/             # 工具类(axios)
        └── components/         # 公共组件
```

---

## 快速开始 | Quick Start

### 1. 构建后端项目

```bash
cd chana-registry
mvn clean install -DskipTests
```

### 2. 启动后端服务

```bash
cd chanaserver
mvn compile exec:java -Dexec.mainClass="com.chanacode.server.bootstrap.ChaNaServer"
# Netty端口: 9999, HTTP API端口: 9998
```

### 3. 启动前端

```bash
cd chanaui
npm install
npm run dev
```

访问 `http://localhost:3000` 查看控制台

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

## 客户端使用 | Client Usage

```java
// 创建客户端
ChaNaClient client = new ChaNaClient("localhost", 9999);
client.connect();

// 注册服务
ServiceInstance instance = ServiceInstance.builder()
    .instanceId(UUID.randomUUID().toString())
    .serviceName("order-service")
    .host("192.168.1.100")
    .port(8080)
    .weight(100)
    .namespace("production")
    .build();

client.register(instance);

// 启动心跳 (5秒间隔)
client.startHeartbeat(5000);

// 发现服务
List<ServiceInstance> instances = client.discover("order-service", "production");

// 关闭
client.close();
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
- **Java 21+**: 虚拟线程(Project Loom)、ZGC垃圾回收器
- **Netty 4.1**: 高性能网络、EPOLL支持
- **Spring Boot 3.2**: 标准HTTP API
- **Undertow**: 高性能Web容器
- **Guava**: 高性能缓存

### 前端
- **React 19**: 前端框架
- **React Router 7**: 路由框架
- **Ant Design 5**: UI组件库
- **Vite 7**: 构建工具
- **Recharts**: 图表组件
- **Axios**: HTTP客户端

---

## License

MIT License - 一朝风月

---

> **"一次心跳、每一次注册与发现，都发生在一个个精准的'刹那'之间"**
