# Zhi Push Message - Reactive WebSocket Gateway

基于 Spring Boot WebFlux + Reactor Netty 的响应式 WebSocket 实时消息网关。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)

---

## Introduction

Zhi Push Message (zhi-pm) 是一个开源、响应式、高性能、可观测、可扩展的 Spring Boot WebSocket 实时消息网关。

它不是一个简单的 WebSocket Demo，而是一个面向真实业务场景的实时通信网关，覆盖：

- 企业实时通知
- 用户在线状态
- 单聊 / 群聊聊天室
- 房间广播
- 直播间弹幕
- 订单状态推送
- 多实例 WebSocket 消息分发
- 实时监控管理页面
- Prometheus / Grafana 可观测能力
- Spring Boot Starter 快速集成

---

## Features

### 核心能力

- **响应式架构** - Spring WebFlux + Reactor Netty，全链路非阻塞
- **连接管理** - 基于内存的高性能连接注册表，支持多端连接
- **用户推送** - 单用户、多用户、全站广播
- **房间广播** - 房间加入/退出、成员管理、在线人数统计
- **直播弹幕** - 高吞吐弹幕、用户级/房间级限流、敏感词过滤、禁言
- **聊天室** - 单聊/群聊、消息 ACK、离线补偿、未读数
- **多实例分发** - Redis Pub/Sub 低延迟广播、Kafka 可靠消息
- **Token 鉴权** - 可插拔认证，支持 Token/JWT/Ticket
- **应用层心跳** - 客户端驱动心跳、超时清理
- **管理控制台** - 实时 Dashboard、连接管理、消息追踪
- **可观测性** - Micrometer 指标、Prometheus 端点、Grafana Dashboard

### 技术特性

- Java 21 LTS
- Spring Boot 3.3.5
- Reactor Sinks.Many 每连接独立出站流
- 可配置的背压缓冲区
- 幂等会话清理（正常关闭、异常、取消）
- 条件化自动配置，支持业务覆盖默认 Bean
- Docker / Helm Chart 一键部署

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Layer                           │
│  Browser / Mobile / Desktop / CLI                           │
└─────────────────────────┬───────────────────────────────────┘
                          │ WebSocket
┌─────────────────────────▼───────────────────────────────────┐
│                  zhi-pm-server                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              WebSocketHandler                        │   │
│  │  Auth ──► Decode ──► Route ──► Encode ──► Send      │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Connection   │  │   Message    │  │   Heartbeat  │     │
│  │  Registry     │  │   Sender     │  │   Service    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                   Message Broker                            │
│  ┌──────────────────┐        ┌──────────────────┐          │
│  │  Redis Pub/Sub   │        │  Kafka           │          │
│  │  (低延迟广播)     │        │  (可靠消息)       │          │
│  └──────────────────┘        └──────────────────┘          │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                   Feature Modules                           │
│  ┌────────┐  ┌─────────┐  ┌────────┐  ┌──────────────┐    │
│  │  Room  │  │ Danmaku │  │  Chat  │  │ Observability │    │
│  └────────┘  └─────────┘  └────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker (optional, for containerized deployment)
- Redis (optional, for multi-instance mode)
- Kafka (optional, for reliable message mode)

### 方式一：Maven 直接运行

```bash
# 克隆项目
git clone https://github.com/zhi-pm/zhi-pm.git
cd zhi-pm

# 编译打包
mvn clean package -DskipTests

# 运行示例
java -jar samples/basic/sample-basic-echo/target/sample-basic-echo-*.jar
```

应用启动后，WebSocket 服务运行在 `ws://localhost:8080/ws`

### 方式二：Docker Compose 运行

```bash
# 启动 Redis + Gateway
docker compose up -d

# 查看日志
docker compose logs -f gateway

# 停止服务
docker compose down
```

### 方式三：Docker Compose + Kafka

```bash
# 启动 Redis + Kafka + Gateway
docker compose -f docker-compose.yml -f docker-compose.kafka.yml up -d
```

### 测试 WebSocket 连接

使用 `websocat` 命令行工具：

```bash
# 连接（带 Token）
websocat "ws://localhost:8080/ws?access_token=alice-token"

# 发送心跳
{"type":"heartbeat.ping","timestamp":1710000000000}

# 发送消息
{"type":"echo","payload":{"content":"hello"}}
```

使用 REST API 推送消息：

```bash
# 推送到用户
curl -X POST http://localhost:8080/api/push/user/alice \
  -H "Content-Type: application/json" \
  -d '{"type":"notification","payload":{"title":"Hello","content":"World"}}'

# 全站广播
curl -X POST http://localhost:8080/api/push/broadcast \
  -H "Content-Type: application/json" \
  -d '{"type":"announcement","payload":{"content":"System maintenance in 5 minutes"}}'
```

---

## Module List

### 核心模块

| 模块 | 说明 |
|------|------|
| `zhi-pm-core` | 核心协议、连接管理、消息路由、发送抽象 |
| `zhi-pm-spring-boot-autoconfigure` | 自动配置、配置属性、默认 Bean |
| `zhi-pm-spring-boot-starter` | Starter 依赖聚合 |
| `zhi-pm-server` | 独立部署的 Push Message 网关服务 |

### 功能模块

| 模块 | 说明 |
|------|------|
| `zhi-pm-room` | 房间加入、退出、广播、人数统计 |
| `zhi-pm-danmaku` | 弹幕、限流、过滤、禁言、热门房间优化 |
| `zhi-pm-chat` | 单聊、群聊、ACK、历史消息、离线补偿 |

### 集成模块

| 模块 | 说明 |
|------|------|
| `zhi-pm-broker-redis` | Redis Pub/Sub 多实例分发 |
| `zhi-pm-broker-kafka` | Kafka 可靠消息分发 |
| `zhi-pm-observability` | Micrometer 指标、Prometheus 端点 |
| `zhi-pm-admin-api` | 管理接口 |
| `zhi-pm-admin-ui` | 管理页面 |

---

## Samples

### Basic Samples

| 示例 | 端口 | 说明 |
|------|------|------|
| `sample-basic-echo` | 8080 | 最小 WebSocket 连接示例，Echo + Token + 心跳 |
| `sample-presence` | 8081 | 在线状态、心跳、多端连接、房间管理 |

### Business Samples

| 示例 | 端口 | 说明 |
|------|------|------|
| `sample-notification-center` | 8082 | 企业通知、用户推送、未读数 |
| `sample-live-danmaku` | 8083 | 房间弹幕、限流、敏感词过滤、广播 |
| `sample-chat-room` | 8084 | 单聊、群聊、ACK、历史消息 |
| `sample-order-tracking` | 8086 | Kafka Topic 订阅、订单状态推送 |

### Operations Samples

| 示例 | 端口 | 说明 |
|------|------|------|
| `sample-admin-console` | 8085 | 实时监控、连接治理、Prometheus 指标 |

---

## Configuration Reference

### WebSocket 配置

```yaml
realtime:
  websocket:
    # WebSocket 路径
    path: /ws
    # 出站缓冲区大小
    outbound-buffer-size: 256
    # 最大帧负载长度
    max-frame-payload-length: 65536

    # 认证配置
    auth:
      enabled: true
      # Token 查询参数名
      token-param-name: access_token
      # Token 请求头名
      header-name: Authorization
      # Demo Token 映射（仅用于开发测试）
      demo-tokens:
        alice-token: alice
        bob-token: bob

    # 心跳配置
    heartbeat:
      enabled: true
      # 客户端超时时间
      client-timeout: 60s
      # 检查间隔
      check-interval: 30s
```

### Broker 配置

```yaml
realtime:
  broker:
    # 类型：redis 或 kafka
    type: redis
    redis:
      # Redis Pub/Sub 主题
      topic: realtime:ws:message
    kafka:
      # Kafka 主题
      topic: realtime-ws-message
      # 消费者组
      consumer-group: realtime-ws-gateway
```

### 房间配置

```yaml
realtime:
  room:
    enabled: true
    # 每用户最大房间数
    max-rooms-per-user: 20
    # 每房间最大成员数
    max-members-per-room: 100000
```

### 弹幕配置

```yaml
realtime:
  danmaku:
    enabled: true
    # 最大内容长度
    max-content-length: 100
    # 每用户每秒最大消息数
    max-message-per-user-per-second: 2
    # 每房间每秒最大消息数
    max-message-per-room-per-second: 5000
    # 过载时丢弃
    drop-when-overloaded: true
    # 敏感词列表
    sensitive-words:
      - spam
      - ads
```

### 聊天配置

```yaml
realtime:
  chat:
    enabled: true
    # 启用 ACK
    ack-enabled: true
    # 启用离线消息
    offline-message-enabled: true
    # 最大消息长度
    max-message-length: 2000
```

### 管理配置

```yaml
realtime:
  admin:
    enabled: true
    # 管理 API 路径
    path: /admin/api
    # 实时推送配置
    live:
      enabled: true
      push-interval-ms: 1000
      max-connections: 20
    # 消息追踪配置
    message-trace:
      enabled: true
      sample-rate: 0.01
      max-buffer-size: 1000
      include-payload: false
```

---

## Message Protocol

### 消息格式

```json
{
  "id": "msg_123456",
  "type": "chat.group.message",
  "traceId": "trace_abc",
  "timestamp": 1710000000000,
  "payload": {
    "content": "hello"
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 服务端消息 ID |
| `clientMessageId` | string | 客户端消息 ID，用于幂等 |
| `type` | string | 消息类型 |
| `traceId` | string | 链路追踪 ID |
| `tenantId` | string | 租户 ID |
| `from` | string | 发送者 |
| `to` | string | 接收者 |
| `roomId` | string | 房间 ID |
| `timestamp` | long | 时间戳（毫秒） |
| `payload` | object | 业务负载 |

### 内置消息类型

| 类型 | 说明 |
|------|------|
| `heartbeat.ping` | 客户端心跳 |
| `heartbeat.pong` | 服务端心跳响应 |
| `echo` | Echo 回显 |
| `room.join` | 加入房间 |
| `room.leave` | 离开房间 |
| `room.broadcast` | 房间广播 |
| `danmaku.send` | 发送弹幕 |
| `danmaku.message` | 弹幕消息 |
| `chat.message` | 聊天消息 |
| `chat.ack` | 消息确认 |

---

## Admin API

### 连接管理

```bash
# 获取所有连接
GET /admin/api/connections

# 获取连接详情
GET /admin/api/connections/{sessionId}

# 踢下线
DELETE /admin/api/connections/{sessionId}
```

### 房间管理

```bash
# 获取所有房间
GET /admin/api/rooms

# 获取房间成员
GET /admin/api/rooms/{roomId}/members

# 获取房间在线人数
GET /admin/api/rooms/{roomId}/count
```

### 用户管理

```bash
# 查询用户在线状态
GET /admin/api/users/{userId}/online

# 获取用户连接列表
GET /admin/api/users/{userId}/connections

# 推送到用户
POST /admin/api/push/user/{userId}
```

### 广播

```bash
# 全站广播
POST /admin/api/push/broadcast
```

### 统计

```bash
# 获取统计数据
GET /admin/api/stats
```

---

## Metrics

### Prometheus 端点

```
http://localhost:8080/actuator/prometheus
```

### 核心指标

| 指标 | 类型 | 说明 |
|------|------|------|
| `ws.connections.active` | Gauge | 当前活跃连接数 |
| `ws.connections.total` | Counter | 总连接数 |
| `ws.users.online` | Gauge | 在线用户数 |
| `ws.rooms.active` | Gauge | 活跃房间数 |
| `ws.room.members.active` | Gauge | 房间成员数 |
| `ws.messages.inbound.total` | Counter | 入站消息总数 |
| `ws.messages.outbound.total` | Counter | 出站消息总数 |
| `ws.messages.failed.total` | Counter | 失败消息总数 |
| `ws.messages.dropped.total` | Counter | 丢弃消息总数 |
| `ws.danmaku.inbound.total` | Counter | 弹幕入站总数 |
| `ws.danmaku.filtered.total` | Counter | 弹幕过滤总数 |
| `ws.danmaku.limited.total` | Counter | 弹幕限流总数 |
| `ws.chat.messages.total` | Counter | 聊天消息总数 |
| `ws.heartbeat.timeout.total` | Counter | 心跳超时总数 |
| `ws.broker.publish.latency` | Timer | Broker 发布延迟 |
| `ws.broker.consume.latency` | Timer | Broker 消费延迟 |

---

## Grafana Dashboard

项目提供预配置的 Grafana Dashboard JSON 文件，可直接导入。

Dashboard 文件位置：`docs/grafana/zhi-pm-dashboard.json`

导入步骤：

1. 打开 Grafana
2. 进入 Dashboards > Import
3. 上传 JSON 文件或粘贴内容
4. 选择 Prometheus 数据源
5. 点击 Import

---

## Docker Compose

### 完整配置

```yaml
# docker-compose.yml
version: "3.8"

services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  gateway:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - redis
    environment:
      - SPRING_DATA_REDIS_HOST=redis
      - REALTIME_BROKER_TYPE=redis

volumes:
  redis-data:
```

### Kafka 配置

```bash
# 启动完整 Kafka 环境
docker compose -f docker-compose.yml -f docker-compose.kafka.yml up -d
```

---

## Helm Chart

### 安装

```bash
# 添加 Helm 仓库（如果发布到仓库）
helm repo add zhi-pm https://zhi-pm.github.io/helm-charts

# 安装
helm install zhi-pm ./deploy/helm/zhi-pm \
  --namespace zhi-pm \
  --create-namespace \
  --set config.broker.type=redis
```

### 自定义配置

```bash
# 使用自定义 values 文件
helm install zhi-pm ./deploy/helm/zhi-pm \
  -f my-values.yaml \
  --namespace zhi-pm
```

### 升级

```bash
helm upgrade zhi-pm ./deploy/helm/zhi-pm \
  --namespace zhi-pm \
  --set image.tag=1.0.0
```

---

## JDK 8 集成方案

对于运行在 JDK 8 上的业务系统，推荐独立部署 `zhi-pm-server`，通过 HTTP API 推送消息：

```bash
# 独立部署 zhi-pm-server
java -jar zhi-pm-server.jar

# JDK 8 业务系统通过 HTTP 调用推送接口
curl -X POST http://gateway:8080/api/push/user/{userId} \
  -H "Content-Type: application/json" \
  -d '{"type":"order.status","payload":{"orderId":"12345","status":"SHIPPED"}}'
```

---

## Contributing

欢迎贡献代码！请遵循以下步骤：

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 开发环境

```bash
# 克隆项目
git clone https://github.com/zhi-pm/zhi-pm.git
cd zhi-pm

# 编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn package -DskipTests
```

### 代码规范

- 遵循现有代码风格
- 添加必要的测试
- 更新相关文档
- 不要提交敏感信息

---

## Roadmap

### 0.1.0 MVP (已完成)

- 核心 WebSocket 网关
- 连接管理
- 用户推送
- Token 鉴权
- 应用层心跳
- 基础示例

### 0.2.0 Room + Redis (已完成)

- 房间加入/退出
- 房间广播
- Redis Pub/Sub 多实例分发
- 在线人数统计

### 0.3.0 Danmaku (已完成)

- 弹幕发送/接收
- 用户级/房间级限流
- 敏感词过滤
- 禁言功能

### 0.4.0 Chat (已完成)

- 单聊/群聊
- 消息 ACK
- 离线补偿
- 未读数

### 0.5.0 Admin + Observability (已完成)

- 管理控制台
- 连接管理
- 消息追踪
- Prometheus 指标

### 0.6.0 Kafka Reliable Mode (已完成)

- Kafka Broker
- 消息重试
- 死信消息
- 订单状态推送

### 1.0.0 Production Ready (当前版本)

- 稳定公共 API
- Docker Compose
- Helm Chart
- 完整文档
- 生产就绪配置

---

## License

本项目采用 MIT License - 详见 [LICENSE](LICENSE) 文件。

---

## Acknowledgments

- [Spring Framework WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux-websocket.html)
- [Reactor Netty](https://projectreactor.io/docs/netty/release/reference/)
- [Reactor Sinks](https://projectreactor.io/docs/core/release/reference/coreFeatures/sinks.html)
- [Redis Pub/Sub](https://redis.io/docs/latest/develop/pubsub/)
- [Apache Kafka](https://kafka.apache.org/documentation/)
- [Micrometer](https://micrometer.io/)
- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
