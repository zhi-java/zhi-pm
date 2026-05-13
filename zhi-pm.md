# Reactive WebSocket Gateway 目标与设计方案

> 文档版本：v0.1  
> 项目定位：基于 Spring Boot WebFlux + Reactor Netty 的响应式 WebSocket 实时消息网关

---

## 1. 项目定位

本项目定位为一个可持续开源、可被企业项目复用的实时消息基础设施：

**Reactive WebSocket Gateway**

中文定位：

**基于 Spring Boot WebFlux + Reactor Netty 的响应式 WebSocket 实时消息网关**

项目不是一个简单 WebSocket Demo，而是一个面向真实业务场景的实时通信网关，覆盖：

- 企业实时通知
- 用户在线状态
- 单聊 / 群聊聊天室
- 房间广播
- 直播间弹幕
- 订单状态推送
- 多实例 WebSocket 消息分发
- 实时监控管理页面
- Prometheus / Grafana 可观测能力
- Spring Boot Starter 快速集成；JDK 8 业务系统优先通过独立部署的 zhi-pm-server HTTP 接口集成

---

## 2. 技术底座

核心技术组合：

```text
Spring Boot WebFlux
+ Reactor Netty
+ WebSocketHandler
+ Sinks.Many
+ Redis Pub/Sub
+ Kafka
+ Token / JWT / Ticket 鉴权
+ 应用层心跳
+ Micrometer / Prometheus / Grafana
+ Admin Console
```

选择原因：

- Spring WebFlux 适合非阻塞、响应式、高并发长连接场景。
- Reactor Netty 是 Spring WebFlux 默认常用的高性能网络运行时。
- WebSocketHandler 适合直接控制 WebSocket 入站和出站响应式流。
- Sinks.Many 适合为每个连接维护独立出站消息流。
- Redis Pub/Sub 适合低延迟、多实例实时转发。
- Kafka 适合可靠消息、消息回放、业务事件流和审计。
- Micrometer + Prometheus + Grafana 适合生产级指标观测。
- Admin Console 用于开发调试和轻量运维治理。

---

## 3. 总体设计目标

### 3.1 高性能

目标：

- 支持大量 WebSocket 长连接。
- 支持高频消息推送。
- 支持房间广播和直播弹幕。
- 支持多实例横向扩展。
- 避免阻塞 Netty EventLoop。
- 管理页面不能影响业务消息链路。

设计原则：

- WebSocketHandler 中禁止执行阻塞 IO。
- 发送链路禁止使用 block()。
- 连接出站消息使用 Sinks.Many。
- 大房间广播需要限流、采样、降级。
- 管理页面消息监控必须采样和聚合。
- Redis 适合低延迟弱可靠广播。
- Kafka 适合强可靠业务消息。

---

### 3.2 企业可用

企业项目重点关注：

- 连接是否稳定。
- 消息是否成功送达。
- 失败是否可追踪。
- 多实例是否能正确分发。
- 用户在线状态是否准确。
- 是否支持鉴权和权限控制。
- 是否支持可观测、可治理。
- 是否可以通过 Starter 快速接入。

需要支持：

- Token / JWT / Ticket 鉴权
- 应用层心跳
- 用户在线状态
- 多端连接
- 单用户推送
- 房间推送
- 聊天室
- 弹幕
- ACK
- 离线补偿
- 限流
- 禁言
- 敏感词过滤
- 管理页面
- Prometheus 指标

---

### 3.3 开源友好

开源目标：

- 容易启动。
- 容易理解。
- 容易集成。
- 容易扩展。
- 容易贡献。
- 容易排查问题。

需要提供：

- README.md
- ROADMAP.md
- CHANGELOG.md
- CONTRIBUTING.md
- CODE_OF_CONDUCT.md
- SECURITY.md
- LICENSE
- Docker Compose
- 示例工程
- 文档站点
- 压测报告
- Grafana Dashboard
- GitHub Actions CI

---

## 4. 核心能力设计

### 4.1 连接管理

需要维护：

```text
sessionId -> connection
userId -> sessionId 集合
tenantId -> sessionId 集合
roomId -> sessionId 集合
instanceId -> sessionId 集合
```

核心能力：

- 注册连接
- 注销连接
- 查询用户连接
- 查询租户连接
- 查询房间连接
- 查询在线人数
- 多端连接管理
- 踢下线
- 心跳超时清理

核心接口：

```java
public interface ConnectionRegistry {

    Mono<Void> register(SessionConnection connection);

    Mono<Void> unregister(String sessionId);

    Mono<SessionConnection> getConnection(String sessionId);

    Flux<SessionConnection> getUserConnections(String userId);

    Flux<SessionConnection> getTenantConnections(String tenantId);

    Mono<Boolean> isOnline(String userId);

    Mono<Long> countConnections();
}
```

---

### 4.2 消息协议

统一消息协议：

```json
{
  "id": "msg_123456",
  "type": "chat.group.message",
  "traceId": "trace_abc",
  "tenantId": "tenant_001",
  "from": "1001",
  "to": "1002",
  "roomId": "group_10001",
  "timestamp": 1710000000000,
  "payload": {
    "content": "hello"
  }
}
```

基础字段：

| 字段            | 说明                    |
| --------------- | ----------------------- |
| id              | 服务端消息 ID           |
| clientMessageId | 客户端消息 ID，用于幂等 |
| type            | 消息类型                |
| traceId         | 链路追踪 ID             |
| tenantId        | 租户 ID                 |
| from            | 发送者                  |
| to              | 接收者                  |
| roomId          | 房间 ID                 |
| timestamp       | 时间戳                  |
| payload         | 业务负载                |

---

### 4.3 用户推送

核心能力：

- 单用户推送
- 多用户推送
- 多端同步
- 在线推送
- 离线补偿
- 未读数
- ACK

核心接口：

```java
public interface MessageSender {

    Mono<Boolean> sendToUser(String userId, WsMessage<?> message);

    Mono<Integer> sendToUsers(Collection<String> userIds, WsMessage<?> message);

    Mono<Integer> sendToRoom(String roomId, WsMessage<?> message);

    Mono<Integer> broadcast(WsMessage<?> message);
}
```

---

### 4.4 房间广播

房间能力是聊天室、弹幕、协作、互动投票的基础。

核心能力：

- 加入房间
- 退出房间
- 房间广播
- 房间在线人数
- 房间成员查询
- 房间禁言
- 房间限流

核心接口：

```java
public interface RoomRegistry {

    Mono<Void> joinRoom(String roomId, SessionConnection connection);

    Mono<Void> leaveRoom(String roomId, String sessionId);

    Mono<Void> leaveAllRooms(String sessionId);

    Flux<SessionConnection> getRoomConnections(String roomId);

    Mono<Long> countRoomConnections(String roomId);

    Mono<Boolean> isInRoom(String roomId, String sessionId);
}
```

---

### 4.5 房间弹幕

弹幕定位：

```text
高吞吐
弱可靠
低延迟
可采样
可降级
可丢弃
```

弹幕能力：

- danmaku.send
- danmaku.message
- 用户级限流
- 房间级限流
- 敏感词过滤
- 用户禁言
- 房间禁言
- 热门房间分片
- 弹幕 QPS 监控
- 弹幕采样展示

弹幕发送流程：

```text
客户端发送 danmaku.send
        ↓
Token 鉴权
        ↓
校验是否在房间内
        ↓
禁言校验
        ↓
敏感词过滤
        ↓
用户级限流
        ↓
房间级限流
        ↓
发布到 Redis / Kafka
        ↓
各实例消费
        ↓
推送本实例房间连接
        ↓
管理页面采样监控
```

---

### 4.6 聊天室

聊天室定位：

```text
强可靠
可持久化
可补偿
可追踪
支持 ACK
支持历史消息
```

支持三种模式：

- 单聊
- 群聊
- 客服会话

聊天室能力：

- 创建会话
- 加入群聊
- 退出群聊
- 文本消息
- 消息落库
- ACK
- 离线消息
- 未读数
- 已读回执
- 正在输入
- 消息撤回
- 消息删除
- 用户禁言
- 群禁言

推荐新增模块：

```text
zhi-pm-chat
zhi-pm-sample-chat-room
```

聊天室和弹幕区别：

| 能力         | 弹幕       | 聊天室 |
| ------------ | ---------- | ------ |
| 可靠性       | 弱可靠     | 强可靠 |
| 是否允许丢失 | 可以       | 不建议 |
| 是否持久化   | 可选       | 建议   |
| 是否 ACK     | 通常不需要 | 建议   |
| 是否离线补偿 | 通常不需要 | 需要   |
| 是否未读数   | 不需要     | 需要   |

---

### 4.7 多实例分发

多实例问题：

```text
用户 A 连接在实例 1
推送请求打到实例 2
实例 2 本地没有用户 A 的连接
```

解决方案：

```text
业务请求
  ↓
当前实例发布 Broker 消息
  ↓
Redis Pub/Sub 或 Kafka
  ↓
所有实例消费
  ↓
只有持有目标连接的实例执行本地推送
```

Broker 抽象：

```java
public interface MessageBroker {

    Mono<Void> publish(BrokerMessage message);

    Flux<BrokerMessage> subscribe();
}
```

Redis 模式：

- 适合低延迟广播。
- 适合在线通知。
- 适合房间弹幕。
- 不适合单独承载强可靠聊天消息。

Kafka 模式：

- 适合可靠消息。
- 适合聊天消息。
- 适合订单事件。
- 适合消息回放。
- 适合审计和削峰。

---

### 4.8 鉴权

支持方式：

- Token
- JWT
- OAuth2 Resource Server
- 一次性 WebSocket Ticket

推荐生产方案：

```text
HTTP 登录获得 access_token
        ↓
调用 /api/ws-ticket 获取短期 ticket
        ↓
WebSocket 使用 ticket 建连
        ↓
服务端校验 ticket 并立即失效
```

WebSocket 地址示例：

```text
ws://localhost:8080/ws?ticket=xxx
```

---

### 4.9 应用层心跳

客户端发送：

```json
{
  "type": "heartbeat.ping",
  "timestamp": 1710000000000
}
```

服务端返回：

```json
{
  "type": "heartbeat.pong",
  "timestamp": 1710000000001
}
```

服务端定时检测：

```text
当前时间 - lastHeartbeatAt > timeout
        ↓
关闭连接
        ↓
清理连接注册表
        ↓
清理房间成员
        ↓
更新在线状态
```

---

### 4.10 管理控制台

管理控制台定位：

```text
实时运维控制台
```

不是替代 Grafana，而是用于开发调试和轻量运维。

第一版页面：

- Dashboard 总览
- Connections 连接列表
- Rooms 房间列表
- Messages 消息追踪
- Instances 实例状态

核心指标：

- 当前连接数
- 在线用户数
- 房间数量
- 入站消息 QPS
- 出站消息 QPS
- 弹幕 QPS
- 推送成功率
- 推送失败率
- 心跳超时数
- Redis / Kafka 状态
- Kafka lag
- 实例连接分布

性能原则：

- 不展示全量消息。
- 成功消息采样。
- 失败消息全量记录。
- 慢消息全量记录。
- 管理 WebSocket 和业务 WebSocket 隔离。
- 管理流限流推送。

---

## 5. 推荐工程模块

```text
zhi-pm
├── zhi-pm-core
├── zhi-pm-spring-boot-autoconfigure
├── zhi-pm-spring-boot-starter
├── zhi-pm-server
├── zhi-pm-room
├── zhi-pm-danmaku
├── zhi-pm-chat
├── zhi-pm-broker-redis
├── zhi-pm-broker-kafka
├── zhi-pm-security
├── zhi-pm-observability
├── zhi-pm-admin-api
├── zhi-pm-admin-ui
├── zhi-pm-client-java
├── zhi-pm-client-js
├── samples
│   ├── basic
│   │   ├── sample-basic-echo
│   │   └── sample-presence
│   ├── business
│   │   ├── sample-notification-center
│   │   ├── sample-order-tracking
│   │   └── sample-customer-service
│   ├── realtime
│   │   ├── sample-chat-room
│   │   ├── sample-live-danmaku
│   │   └── sample-live-interaction
│   └── operations
│       ├── sample-admin-console
│       ├── sample-alert-center
│       └── sample-iot-monitor
└── docs
```

模块说明：

| 模块                          | 职责                                   |
| ----------------------------- | -------------------------------------- |
| zhi-pm-core                | 核心协议、连接管理、消息路由、发送抽象 |
| zhi-pm-spring-boot-autoconfigure | 自动配置、配置属性、默认 Bean          |
| zhi-pm-spring-boot-starter | Starter 依赖聚合                      |
| zhi-pm-server              | 独立部署的 Push Message 网关服务，供旧版 JDK 业务系统通过 HTTP 集成 |
| zhi-pm-room                | 房间加入、退出、广播、人数统计         |
| zhi-pm-danmaku             | 弹幕、限流、过滤、禁言、热门房间优化   |
| zhi-pm-chat                | 单聊、群聊、ACK、历史消息、离线补偿    |
| zhi-pm-broker-redis        | Redis Pub/Sub 多实例分发               |
| zhi-pm-broker-kafka        | Kafka 可靠消息分发                     |
| zhi-pm-security            | Token、JWT、Ticket 鉴权                |
| zhi-pm-observability       | Metrics、Tracing、Logging              |
| zhi-pm-admin-api           | 管理接口                               |
| zhi-pm-admin-ui            | 管理页面                               |
| zhi-pm-client-java         | Java 客户端 SDK                        |
| zhi-pm-client-js           | JS 客户端 SDK                          |
| samples                       | 示例项目                               |
| docs                          | 文档                                   |

---

## 6. 示例功能规划

第一阶段必须实现：

| 示例                       | 目标                       |
| -------------------------- | -------------------------- |
| sample-basic-echo          | 最小 WebSocket 连接示例    |
| sample-presence            | 在线状态、心跳、多端连接   |
| sample-notification-center | 企业通知、用户推送、未读数 |
| sample-chat-room           | 单聊、群聊、ACK、历史消息  |
| sample-live-danmaku        | 房间弹幕、限流、广播       |
| sample-order-tracking      | Topic 订阅、订单状态推送   |
| sample-admin-console       | 实时监控、连接治理         |

第二阶段实现：

| 示例                       | 目标                 |
| -------------------------- | -------------------- |
| sample-customer-service    | 客服会话、排队、转接 |
| sample-collaboration-board | 多人协作、事件同步   |
| sample-live-interaction    | 投票、问答、点赞     |
| sample-alert-center        | 实时告警、确认、恢复 |

第三阶段实现：

| 示例               | 目标                             |
| ------------------ | -------------------------------- |
| sample-iot-monitor | 高频设备数据、设备状态、实时曲线 |

---

## 7. 推荐配置规范

统一配置前缀：

```yaml
realtime:
  websocket:
    path: /ws
    max-frame-payload-length: 65536
    outbound-buffer-size: 256
    auth:
      enabled: true
      token-param-name: access_token
      header-name: Authorization
      demo-tokens:
        alice-token: alice
        bob-token: bob
      accept-non-blank-token-when-no-tokens-configured: false
    heartbeat:
      enabled: true
      client-timeout: 60s
      check-interval: 30s

  broker:
    type: redis
    redis:
      topic: realtime:ws:message
    kafka:
      topic: realtime-ws-message
      consumer-group: realtime-ws-gateway

  room:
    enabled: true
    max-rooms-per-user: 20
    max-members-per-room: 100000

  danmaku:
    enabled: true
    max-content-length: 100
    max-message-per-user-per-second: 2
    max-message-per-room-per-second: 5000
    drop-when-overloaded: true

  chat:
    enabled: true
    ack-enabled: true
    offline-message-enabled: true
    max-message-length: 2000

  limits:
    max-connections-per-user: 5
    max-connections-per-ip: 50
    max-message-per-second: 20

  admin:
    enabled: true
    path: /admin
    live:
      enabled: true
      push-interval-ms: 1000
      max-connections: 20
    message-trace:
      enabled: true
      sample-rate: 0.01
      max-buffer-size: 1000
      include-payload: false

  observability:
    enabled: true
```

---

## 8. 数据库表设计建议

### 8.1 聊天会话表

```sql
CREATE TABLE chat_conversation (
    id BIGINT PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    conversation_type VARCHAR(32) NOT NULL,
    tenant_id VARCHAR(64),
    title VARCHAR(128),
    avatar VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### 8.2 会话成员表

```sql
CREATE TABLE chat_conversation_member (
    id BIGINT PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    muted BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP NOT NULL,
    last_read_message_id VARCHAR(64),
    last_read_at TIMESTAMP
);
```

### 8.3 聊天消息表

```sql
CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    client_message_id VARCHAR(64),
    conversation_id VARCHAR(64) NOT NULL,
    conversation_type VARCHAR(32) NOT NULL,
    tenant_id VARCHAR(64),
    sender_id VARCHAR(64) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    content TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### 8.4 消息投递表

```sql
CREATE TABLE chat_message_delivery (
    id BIGINT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    receiver_id VARCHAR(64) NOT NULL,
    delivery_status VARCHAR(32) NOT NULL,
    delivered_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL
);
```

---

## 9. 观测指标设计

推荐指标：

```text
ws_connections_active
ws_connections_total
ws_users_online
ws_rooms_active
ws_room_members_active
ws_messages_inbound_total
ws_messages_outbound_total
ws_messages_failed_total
ws_messages_dropped_total
ws_danmaku_inbound_total
ws_danmaku_filtered_total
ws_danmaku_limited_total
ws_chat_messages_total
ws_chat_messages_failed_total
ws_heartbeat_timeout_total
ws_broker_publish_latency
ws_broker_consume_latency
ws_kafka_consumer_lag
```

推荐对接：

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- OpenTelemetry

---

## 10. 开源工程规范

仓库必须包含：

```text
README.md
LICENSE
CHANGELOG.md
CONTRIBUTING.md
CODE_OF_CONDUCT.md
SECURITY.md
ROADMAP.md
docs/
.github/ISSUE_TEMPLATE/
.github/PULL_REQUEST_TEMPLATE.md
.github/workflows/
```

README 推荐结构：

```text
Introduction
Features
Architecture
Quick Start
Maven Dependency
Configuration
Message Protocol
Authentication
User Push
Room Broadcast
Danmaku Mode
Chat Room
Redis Multi-instance Mode
Kafka Reliable Mode
Admin Console
Metrics
Grafana Dashboard
Docker Compose
Examples
Roadmap
Contributing
License
```

---

## 11. 版本路线图

### 0.1.0 MVP

目标：核心 WebSocket 网关跑起来。

功能：

- zhi-pm-core
- zhi-pm-spring-boot-starter
- zhi-pm-server 独立网关服务
- WebSocketHandler
- ConnectionRegistry
- Sinks.Many
- 用户推送
- 广播
- Token 鉴权
- 应用层心跳
- sample-basic-echo

---

### 0.2.0 Room + Redis

目标：支持多实例和房间广播。

功能：

- zhi-pm-room
- Redis Pub/Sub Broker
- room.join
- room.leave
- room.broadcast
- 在线人数
- sample-presence
- sample-notification-center

---

### 0.3.0 Danmaku

目标：形成项目特色。

功能：

- zhi-pm-danmaku
- danmaku.send
- danmaku.message
- 用户级限流
- 房间级限流
- 禁言
- 敏感词过滤
- 弹幕 QPS 指标
- sample-live-danmaku

---

### 0.4.0 Chat

目标：支持聊天室场景。

功能：

- zhi-pm-chat
- 单聊
- 群聊
- 消息落库
- ACK
- 离线补偿
- 未读数
- sample-chat-room

---

### 0.5.0 Admin + Observability

目标：提供可视化运维能力。

功能：

- zhi-pm-admin-api
- zhi-pm-admin-ui
- Dashboard
- Connections
- Rooms
- Messages
- Instances
- Prometheus 指标
- Grafana Dashboard
- sample-admin-console

---

### 0.6.0 Kafka Reliable Mode

目标：支持可靠消息和业务事件流。

功能：

- zhi-pm-broker-kafka
- Kafka Broker
- 消息重试
- 死信消息
- 订单状态推送
- sample-order-tracking

---

### 1.0.0 Production Ready

目标：达到生产可用标准。

功能：

- 稳定公共 API
- 完整文档
- 完整测试
- Docker Compose
- Helm Chart
- JS SDK
- Java SDK
- OpenTelemetry 示例
- 压测报告
- 安全策略
- 贡献规范

---

## 12. 实施顺序

建议从最小闭环开始：

```text
第 1 步：创建 Maven 多模块骨架
第 2 步：实现 zhi-pm-core
第 3 步：实现 zhi-pm-spring-boot-starter
第 4 步：实现 zhi-pm-server 独立网关服务
第 5 步：保留 sample-basic-echo 示例
第 6 步：实现连接注册与用户推送
第 7 步：实现 Token 鉴权
第 8 步：实现应用层心跳
第 9 步：实现 zhi-pm-room
第 10 步：实现 Redis Broker
第 11 步：实现 sample-presence
第 12 步：实现 sample-notification-center
第 13 步：实现 zhi-pm-danmaku
第 14 步：实现 sample-live-danmaku
第 15 步：实现 zhi-pm-chat
第 16 步：实现 sample-chat-room
第 17 步：实现 observability 指标
第 18 步：实现 admin-api
第 19 步：实现 admin-ui
第 20 步：实现 Kafka Broker
第 21 步：实现 sample-order-tracking
```

---

## 13. 第一阶段实施范围

第一阶段不要做太大，目标是做出一个能跑、能演示、能扩展的最小开源版本。

第一阶段模块：

```text
zhi-pm-core
zhi-pm-spring-boot-autoconfigure
zhi-pm-spring-boot-starter
zhi-pm-server
zhi-pm-room
zhi-pm-danmaku
zhi-pm-broker-redis
zhi-pm-observability
samples/basic/sample-basic-echo
samples/basic/sample-presence
samples/business/sample-notification-center
samples/realtime/sample-live-danmaku
```

第一阶段功能：

```text
WebSocket 接入
连接管理
用户推送
广播
房间加入 / 退出
房间广播
弹幕发送
Redis 多实例分发
Token 鉴权
应用层心跳
基础指标
基础示例
```

暂缓功能：

```text
Kafka
聊天室完整可靠消息
离线消息
复杂 Admin UI
JS SDK
Java SDK
Helm Chart
OpenTelemetry
```

---

## 14. 最终项目愿景

本项目最终要成为：

**一个开源、响应式、高性能、可观测、可扩展的 Spring Boot WebSocket 实时消息网关。**

它解决的问题：

- 企业项目想快速拥有 WebSocket 实时推送能力。
- 聊天室、弹幕、房间广播需要高性能消息分发。
- 多实例 WebSocket 需要统一转发机制。
- 线上 WebSocket 连接和消息需要可观测、可治理。
- Java 21 应用可通过 Starter 集成；JDK 8 业务系统通过独立部署的 zhi-pm-server HTTP 推送接口集成，而不是在进程内嵌入 Starter。

核心卖点：

```text
Spring Boot WebFlux + Reactor Netty
非阻塞响应式 WebSocket
用户级推送
房间广播
直播弹幕
聊天室
Redis Pub/Sub 多实例分发
Kafka 可靠消息模式
Token / JWT / Ticket 鉴权
应用层心跳
在线状态管理
高性能管理控制台
Prometheus / Grafana / OpenTelemetry
Spring Boot Starter 自动装配
Docker Compose 一键启动
示例工程完整
持续开源规范
```

---

## 15. 参考资料

- Spring Framework WebFlux WebSocket 文档：https://docs.spring.io/spring-framework/reference/web/webflux-websocket.html
- Reactor Sinks.Many API：https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Sinks.Many.html
- Reactor Sinks Reference：https://projectreactor.io/docs/core/release/reference/coreFeatures/sinks.html
- Redis Pub/Sub 文档：https://redis.io/docs/latest/develop/pubsub/
- Apache Kafka 文档：https://kafka.apache.org/documentation/
- Spring Boot Actuator Metrics 文档：https://docs.spring.io/spring-boot/reference/actuator/metrics.html
- Micrometer Prometheus 文档：https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html
- OpenTelemetry 文档：https://opentelemetry.io/docs/
- Semantic Versioning：https://semver.org/