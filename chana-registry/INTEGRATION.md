# ChaNa Registry 集成指南

## 目录
1. [服务端配置](#1-服务端配置)
2. [客户端集成](#2-客户端集成)
3. [React 前端集成](#3-react-前端集成)
4. [注意事项](#4-注意事项)

---

## 1. 服务端配置

### 1.1 配置文件

修改 `chana-spring/src/main/resources/application.yml`:

```yaml
server:
  port: 9998  # HTTP API 端口

chana:
  registry:
    host: localhost       # 服务端地址
    port: 9999           # Netty 端口
    http-port: 9998       # HTTP 端口
    heartbeat-timeout: 30000    # 心跳超时 (ms)
    heartbeat-interval: 5000    # 心跳间隔 (ms)
    auto-remove-unhealthy: true # 自动移除不健康实例
    protection-threshold: 0.2  # 保护阈值

  cache:
    l1-ttl: 5    # L1 缓存 TTL (秒)
    l2-ttl: 30   # L2 缓存 TTL (秒)

  health:
    window-size: 10       # 滑动窗口大小
    failure-threshold: 3  # 失败阈值

  namespace:
    default-namespace: default  # 默认命名空间
```

### 1.2 启动服务端

```bash
# Maven 构建
cd chana-registry
mvn clean package -DskipTests

# 启动 Spring Boot 服务
java -jar chana-spring/target/chana-spring-1.0.0.jar

# 或启动 Netty 服务
java -jar chanaserver/target/chanaserver-1.0.0.jar
```

### 1.3 端口说明

| 端口 | 协议 | 用途 |
|------|------|------|
| 9998 | HTTP | REST API (Spring Boot) |
| 9999 | TCP | Netty 长连接 |

---

## 2. 客户端集成

### 2.1 Java 客户端 (Netty)

```java
// 添加 Maven 依赖
// <dependency>
//     <groupId>com.chanacode</groupId>
//     <artifactId>chanaapi</artifactId>
//     <version>1.0.0</version>
// </dependency>

import com.chanacode.api.client.ChaNaClient;
import com.chanacode.common.dto.ServiceInstance;
import java.util.List;
import java.util.UUID;

public class DemoClient {
    public static void main(String[] args) {
        // 创建客户端 (连接到 Netty 端口)
        ChaNaClient client = new ChaNaClient("localhost", 9999);
        client.connect();

        // 构建服务实例
        ServiceInstance instance = ServiceInstance.builder()
            .instanceId(UUID.randomUUID().toString())
            .serviceName("order-service")
            .host("192.168.1.100")
            .port(8080)
            .namespace("production")
            .weight(100)
            .version("1.0.0")
            .build();

        // 注册服务
        client.register(instance);

        // 启动心跳 (每 5 秒)
        client.startHeartbeat(5000);

        // 发现服务
        List<ServiceInstance> instances = client.discover("order-service", "production");
        for (ServiceInstance inst : instances) {
            System.out.println(inst.getHost() + ":" + inst.getPort());
        }

        // 关闭客户端
        client.close();
    }
}
```

### 2.2 HTTP API 调用

如果你只需要 HTTP 接口，可以直接调用 REST API:

```java
import org.springframework.web.client.RestTemplate;

RestTemplate restTemplate = new RestTemplate();

// 注册服务
String registerUrl = "http://localhost:9998/api/v1/services/register";
restTemplate.postForObject(registerUrl, instance, Void.class);

// 发现服务
String discoverUrl = "http://localhost:9998/api/v1/services/order-service?namespace=default";
ServiceInstance[] instances = restTemplate.getForObject(discoverUrl, ServiceInstance[].class);

// 心跳
String heartbeatUrl = "http://localhost:9998/api/v1/heartbeat";
HeartbeatRequest request = new HeartbeatRequest();
request.setInstanceId(instanceId);
restTemplate.postForObject(heartbeatUrl, request, Void.class);
```

---

## 3. React 前端集成

### 3.1 环境配置

创建 `.env` 文件:

```bash
# 开发环境
VITE_API_BASE_URL=http://localhost:9998/api

# 生产环境
VITE_API_BASE_URL=http://your-server:9998/api
```

### 3.2 前端调用方式

前端通过 **HTTP REST API** 调用后端，不直接连接 Netty:

```
React (HTTP) → Spring Boot (9998) → 内部组件
                ↓
            Netty (9999) ← 其他服务 (Netty 客户端)
```

### 3.3 API 调用示例

```typescript
// src/services/api.ts

import { axiosClient } from '../utils/axios';

class ChanaApiService {
  // 获取服务列表
  async getServices() {
    return axiosClient.get('/v1/services');
  }

  // 注册服务
  async registerService(instance: ServiceInstance) {
    return axiosClient.post('/v1/services/register', instance);
  }

  // 发送心跳
  async sendHeartbeat(instanceId: string, serviceName: string, namespace = 'default') {
    return axiosClient.post('/v1/heartbeat', {
      instanceId,
      serviceName,
      namespace
    });
  }
}

export const apiService = new ChanaApiService();
```

### 3.4 React 组件示例

```tsx
// src/pages/ServiceList.tsx
import { useEffect, useState } from 'react';
import { apiService, ServiceInfo } from '../services/api';

export default function ServiceList() {
  const [services, setServices] = useState<ServiceInfo[]>([]);

  useEffect(() => {
    loadServices();
  }, []);

  const loadServices = async () => {
    const data = await apiService.getServices();
    setServices(data);
  };

  return (
    <div>
      <h1>服务列表</h1>
      {services.map(service => (
        <div key={service.serviceName}>
          {service.serviceName} - {service.instanceCount} 实例
        </div>
      ))}
    </div>
  );
}
```

---

## 4. 注意事项

### 4.1 前端不能直连 Netty

**原因**:
1. 浏览器不支持 TCP 协议
2. Netty 是二进制协议，不适合浏览器
3. 通过 Spring Boot HTTP API 转发更安全

**架构图**:
```
┌─────────────┐     HTTP      ┌─────────────────┐
│   React     │ ──────────→   │  Spring Boot   │
│  (浏览器)    │               │  (9998 端口)   │
└─────────────┘               └────────┬────────┘
                                        │
                                    内部调用
                                        │
                                       ↓
┌─────────────┐    Netty     ┌─────────────────┐
│ Java 客户端 │ ←───────────  │     Netty       │
│ (服务)      │               │    Server      │
└─────────────┘               │  (9999 端口)    │
                              └─────────────────┘
```

### 4.2 心跳机制

- 服务启动后必须定期发送心跳
- 默认心跳间隔: 5 秒
- 默认超时: 30 秒 (连续 6 次心跳失败则标记不健康)

### 4.3 版本兼容性

| 组件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Spring Boot | 3.x |
| Netty | 4.1.x |
| React | 18.x |

### 4.4 常见问题

1. **前端无法访问后端**
   - 检查 CORS 配置 (已配置允许所有)
   - 检查端口是否正确 (9998)

2. **心跳失败**
   - 确保客户端正确实现了心跳逻辑
   - 检查防火墙是否阻止了端口

3. **服务发现为空**
   - 确认服务已注册
   - 检查 namespace 是否匹配
