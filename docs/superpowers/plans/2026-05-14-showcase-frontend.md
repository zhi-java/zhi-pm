# Showcase Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a unified single-page demo app with macOS Cupertino glassmorphism UI that showcases all zhi-pm features through 7 interactive modules.

**Architecture:** Single HTML file served by a new `sample-showcase` Spring Boot app on port 8088. The backend aggregates REST endpoints from all feature modules (presence, push, danmaku, chat, orders, admin). Frontend uses vanilla JS + CSS with hash-based routing and a WebSocket client with auto-reconnect.

**Tech Stack:** Java 21, Spring Boot 3.3.5, WebFlux, Vanilla JS, CSS (glassmorphism), Lucide Icons (CDN)

**Spec:** `docs/superpowers/specs/2026-05-14-showcase-frontend-design.md`

---

## File Structure

| File | Responsibility |
|------|---------------|
| `samples/showcase/pom.xml` | Parent POM for showcase samples |
| `samples/showcase/sample-showcase/pom.xml` | Module POM with all feature dependencies |
| `samples/showcase/sample-showcase/src/main/java/.../SampleShowcaseApplication.java` | Spring Boot entry point |
| `samples/showcase/sample-showcase/src/main/java/.../ShowcaseController.java` | Aggregated REST endpoints for all modules |
| `samples/showcase/sample-showcase/src/main/resources/application.yml` | Configuration (port 8088, all features enabled) |
| `samples/showcase/sample-showcase/src/main/resources/static/index.html` | Complete frontend (CSS + HTML + JS) |
| `samples/showcase/sample-showcase/src/test/java/.../SampleShowcaseApplicationTest.java` | Context load test |
| `samples/pom.xml` | Modified: add `showcase` module |
| `pom.xml` | Modified: add `samples/showcase` module to samples list if needed |

---

### Task 1: Create Maven Module Skeleton

**Files:**
- Create: `samples/showcase/pom.xml`
- Create: `samples/showcase/sample-showcase/pom.xml`
- Modify: `samples/pom.xml`
- Create: `samples/showcase/sample-showcase/src/main/java/io/github/zhi/pm/sample/showcase/SampleShowcaseApplication.java`
- Create: `samples/showcase/sample-showcase/src/test/java/io/github/zhi/pm/sample/showcase/SampleShowcaseApplicationTest.java`
- Create: `samples/showcase/sample-showcase/src/main/resources/application.yml`

- [ ] **Step 1: Create showcase parent POM**

Create `samples/showcase/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent><groupId>io.github.zhi</groupId><artifactId>samples</artifactId><version>0.1.0-SNAPSHOT</version></parent>
  <artifactId>samples-showcase</artifactId><packaging>pom</packaging>
  <modules>
    <module>sample-showcase</module>
  </modules>
</project>
```

- [ ] **Step 2: Create sample-showcase module POM**

Create `samples/showcase/sample-showcase/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent><groupId>io.github.zhi</groupId><artifactId>samples-showcase</artifactId><version>0.1.0-SNAPSHOT</version></parent>
  <artifactId>sample-showcase</artifactId>
  <dependencies>
    <dependency><groupId>io.github.zhi</groupId><artifactId>zhi-pm-spring-boot-starter</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>io.github.zhi</groupId><artifactId>zhi-pm-room</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>io.github.zhi</groupId><artifactId>zhi-pm-danmaku</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>io.github.zhi</groupId><artifactId>zhi-pm-chat</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>io.github.zhi</groupId><artifactId>zhi-pm-observability</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>io.github.zhi</groupId><artifactId>zhi-pm-admin-api</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webflux</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3: Update samples parent POM**

Edit `samples/pom.xml` to add the showcase module:

```xml
<modules>
  <module>basic</module>
  <module>business</module>
  <module>realtime</module>
  <module>operations</module>
  <module>showcase</module>
</modules>
```

- [ ] **Step 4: Create application class**

Create `samples/showcase/sample-showcase/src/main/java/io/github/zhi/pm/sample/showcase/SampleShowcaseApplication.java`:

```java
package io.github.zhi.pm.sample.showcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleShowcaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleShowcaseApplication.class, args);
    }
}
```

- [ ] **Step 5: Create test class**

Create `samples/showcase/sample-showcase/src/test/java/io/github/zhi/pm/sample/showcase/SampleShowcaseApplicationTest.java`:

```java
package io.github.zhi.pm.sample.showcase;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleShowcaseApplicationTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 6: Create application.yml**

Create `samples/showcase/sample-showcase/src/main/resources/application.yml`:

```yaml
server:
  port: 8088

realtime:
  websocket:
    path: /ws
    outbound-buffer-size: 256
    auth:
      enabled: true
      token-param-name: access_token
      demo-tokens:
        alice-token: alice
        bob-token: bob
        charlie-token: charlie
    heartbeat:
      enabled: true
      client-timeout: 60s
      check-interval: 30s
    room:
      enabled: true
      max-rooms-per-user: 50
      max-members-per-room: 100000
  danmaku:
    enabled: true
    max-content-length: 100
    max-message-per-user-per-second: 5
    sensitive-words: [spam, ads]
  chat:
    enabled: true
    ack-enabled: true
    max-message-length: 2000
    max-history-per-conversation: 200
  admin:
    enabled: true
  observability:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

- [ ] **Step 7: Verify module compiles**

Run: `JAVA_HOME="/c/Program Files/Java/jdk-21" PATH="/c/Program Files/Java/jdk-21/bin:$PATH" mvn compile -pl samples/showcase/sample-showcase -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add samples/showcase/ samples/pom.xml
git commit -m "feat(showcase): 创建 sample-showcase 模块骨架"
```

---

### Task 2: Create ShowcaseController

**Files:**
- Create: `samples/showcase/sample-showcase/src/main/java/io/github/zhi/pm/sample/showcase/ShowcaseController.java`

- [ ] **Step 1: Create the aggregated controller**

Create `samples/showcase/sample-showcase/src/main/java/io/github/zhi/pm/sample/showcase/ShowcaseController.java`:

```java
package io.github.zhi.pm.sample.showcase;

import io.github.zhi.pm.core.chat.ChatService;
import io.github.zhi.pm.core.danmaku.DanmakuService;
import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.danmaku.DanmakuServiceImpl;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ShowcaseController {
    private final ConnectionRegistry registry;
    private final MessageSender sender;
    private final DanmakuServiceImpl danmakuService;
    private final ChatService chatService;
    private final Map<String, OrderInfo> orders = new ConcurrentHashMap<>();

    public ShowcaseController(ConnectionRegistry registry, MessageSender sender,
                              DanmakuService danmakuService, ChatService chatService) {
        this.registry = registry;
        this.sender = sender;
        this.danmakuService = (DanmakuServiceImpl) danmakuService;
        this.chatService = chatService;
    }

    // === Push endpoints ===

    @PostMapping("/push/users/{userId}")
    public Mono<Map<String, Object>> pushToUser(@PathVariable("userId") String userId,
                                                 @RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = WsMessage.of("showcase.user-push", payload == null ? Map.of("message", "Hello!") : payload);
        return sender.sendToUser(userId, message).map(sent -> Map.of("sent", sent, "userId", userId));
    }

    @PostMapping("/push/broadcast")
    public Mono<Map<String, Object>> broadcast(@RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = WsMessage.of("showcase.broadcast", payload == null ? Map.of("message", "Broadcast!") : payload);
        return sender.broadcast(message).map(count -> Map.of("sent", count));
    }

    @PostMapping("/push/rooms/{roomId}")
    public Mono<Map<String, Object>> pushToRoom(@PathVariable("roomId") String roomId,
                                                 @RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = WsMessage.of("showcase.room-push", payload == null ? Map.of("message", "Room msg") : payload);
        return sender.sendToRoom(roomId, message).map(count -> Map.of("sent", count, "roomId", roomId));
    }

    // === Presence endpoints ===

    @GetMapping("/presence/connections/count")
    public Mono<Map<String, Object>> connectionCount() {
        return registry.countConnections().map(count -> Map.of("count", count));
    }

    @GetMapping("/presence/users/{userId}/online")
    public Mono<Map<String, Object>> isOnline(@PathVariable("userId") String userId) {
        return registry.isOnline(userId).map(online -> Map.of("userId", userId, "online", online));
    }

    @GetMapping("/presence/rooms/{roomId}/count")
    public Mono<Map<String, Object>> roomCount(@PathVariable("roomId") String roomId) {
        return registry.countRoomConnections(roomId).map(count -> Map.of("roomId", roomId, "count", count));
    }

    // === Danmaku endpoints ===

    @PostMapping("/danmaku/rooms/{roomId}")
    public Mono<Map<String, Object>> sendDanmaku(@PathVariable("roomId") String roomId,
                                                  @RequestParam("content") String content,
                                                  @RequestParam(value = "userId", defaultValue = "system") String userId) {
        WsMessage<Map<String, Object>> message = new WsMessage<>(null, "danmaku.message", null, null, userId, null, roomId, Instant.now(), Map.of("content", content));
        return sender.sendToRoom(roomId, message).map(count -> Map.of("roomId", roomId, "sent", count));
    }

    @PostMapping("/danmaku/rooms/{roomId}/mute/{userId}")
    public Map<String, Object> muteUser(@PathVariable("roomId") String roomId, @PathVariable("userId") String userId) {
        danmakuService.getMuteService().mute(roomId, userId);
        return Map.of("roomId", roomId, "userId", userId, "muted", true);
    }

    @PostMapping("/danmaku/rooms/{roomId}/unmute/{userId}")
    public Map<String, Object> unmuteUser(@PathVariable("roomId") String roomId, @PathVariable("userId") String userId) {
        danmakuService.getMuteService().unmute(roomId, userId);
        return Map.of("roomId", roomId, "userId", userId, "muted", false);
    }

    @GetMapping("/danmaku/rooms/{roomId}/muted")
    public Map<String, Object> getMutedUsers(@PathVariable("roomId") String roomId) {
        Set<String> muted = danmakuService.getMuteService().getMutedUsers(roomId);
        return Map.of("roomId", roomId, "mutedUsers", muted);
    }

    // === Chat endpoints ===

    @GetMapping("/chat/conversations/{conversationId}/history")
    public Mono<Map<String, Object>> getHistory(@PathVariable("conversationId") String conversationId,
                                                 @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return chatService.getHistory(conversationId, limit)
                .map(messages -> Map.of("conversationId", conversationId, "messages", messages, "count", messages.size()));
    }

    @GetMapping("/chat/conversations/{conversationId}/unread/{userId}")
    public Mono<Map<String, Object>> getUnreadCount(@PathVariable("conversationId") String conversationId,
                                                     @PathVariable("userId") String userId) {
        return chatService.getUnreadCount(conversationId, userId)
                .map(count -> Map.of("conversationId", conversationId, "userId", userId, "unreadCount", count));
    }

    // === Order endpoints ===

    @PostMapping("/orders")
    public Mono<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OrderInfo order = new OrderInfo(orderId, request.userId(), request.product(), request.quantity(), "CREATED", Instant.now());
        orders.put(orderId, order);
        WsMessage<Map<String, Object>> message = buildOrderMessage("order.created", order);
        return sender.sendToUser(request.userId(), message)
                .map(sent -> Map.of("orderId", orderId, "status", "CREATED", "sent", sent));
    }

    @PostMapping("/orders/{orderId}/status")
    public Mono<Map<String, Object>> updateOrderStatus(@PathVariable("orderId") String orderId,
                                                        @RequestBody UpdateStatusRequest request) {
        OrderInfo order = orders.get(orderId);
        if (order == null) {
            return Mono.just(Map.of("error", "Order not found", "orderId", orderId));
        }
        OrderInfo updated = new OrderInfo(order.orderId(), order.userId(), order.product(), order.quantity(), request.status(), Instant.now());
        orders.put(orderId, updated);
        WsMessage<Map<String, Object>> message = buildOrderMessage("order.status.changed", updated);
        return sender.sendToUser(updated.userId(), message)
                .map(sent -> Map.of("orderId", orderId, "status", request.status(), "sent", sent));
    }

    @GetMapping("/orders/{orderId}")
    public Mono<Map<String, Object>> getOrder(@PathVariable("orderId") String orderId) {
        OrderInfo order = orders.get(orderId);
        if (order == null) return Mono.just(Map.of("error", "Order not found"));
        return Mono.just(Map.of("orderId", order.orderId(), "userId", order.userId(), "product", order.product(),
                "quantity", order.quantity(), "status", order.status(), "updatedAt", order.updatedAt().toString()));
    }

    @GetMapping("/orders/user/{userId}")
    public Mono<Map<String, Object>> getUserOrders(@PathVariable("userId") String userId) {
        var userOrders = orders.values().stream().filter(o -> o.userId().equals(userId)).toList();
        return Mono.just(Map.of("userId", userId, "orders", userOrders, "count", userOrders.size()));
    }

    private WsMessage<Map<String, Object>> buildOrderMessage(String type, OrderInfo order) {
        Map<String, Object> payload = Map.of("orderId", order.orderId(), "userId", order.userId(),
                "product", order.product(), "quantity", order.quantity(), "status", order.status(), "updatedAt", order.updatedAt().toString());
        return new WsMessage<>(null, type, null, Instant.now(), payload);
    }

    public record CreateOrderRequest(String userId, String product, int quantity) {}
    public record UpdateStatusRequest(String status) {}
    public record OrderInfo(String orderId, String userId, String product, int quantity, String status, Instant updatedAt) {}
}
```

- [ ] **Step 2: Verify compilation**

Run: `JAVA_HOME="/c/Program Files/Java/jdk-21" PATH="/c/Program Files/Java/jdk-21/bin:$PATH" mvn compile -pl samples/showcase/sample-showcase -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/java/io/github/zhi/pm/sample/showcase/ShowcaseController.java
git commit -m "feat(showcase): 实现 ShowcaseController 聚合所有功能端点"
```

---

### Task 3: Create Frontend — HTML Shell + CSS Theme

**Files:**
- Create: `samples/showcase/sample-showcase/src/main/resources/static/index.html`

This task creates the HTML structure and the complete CSS theme. Subsequent tasks add the JS modules.

- [ ] **Step 1: Create index.html with CSS theme**

Create `samples/showcase/sample-showcase/src/main/resources/static/index.html`:

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Zhi PM — Reactive WebSocket Gateway</title>
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        *, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }

        :root {
            --bg-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            --sidebar-bg: rgba(255,255,255,0.15);
            --sidebar-blur: blur(20px);
            --card-bg: rgba(255,255,255,0.7);
            --card-blur: blur(10px);
            --card-shadow: 0 8px 32px rgba(0,0,0,0.1);
            --text-primary: #1d1d1f;
            --text-secondary: #86868b;
            --accent: #007AFF;
            --accent-hover: #0056CC;
            --success: #34C759;
            --warning: #FF9500;
            --error: #FF3B30;
            --border: rgba(0,0,0,0.06);
            --input-bg: rgba(255,255,255,0.5);
            --sidebar-text: rgba(255,255,255,0.7);
            --sidebar-text-active: #fff;
            --sidebar-hover: rgba(255,255,255,0.1);
            --sidebar-active: rgba(255,255,255,0.2);
            --bubble-sent: #007AFF;
            --bubble-sent-text: #fff;
            --bubble-received: rgba(255,255,255,0.8);
            --bubble-received-text: #1d1d1f;
            --log-bg: rgba(0,0,0,0.03);
            --danmaku-bg: rgba(0,0,0,0.6);
        }

        [data-theme="dark"] {
            --bg-gradient: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            --sidebar-bg: rgba(0,0,0,0.3);
            --card-bg: rgba(30,30,30,0.8);
            --text-primary: #f5f5f7;
            --text-secondary: #86868b;
            --border: rgba(255,255,255,0.08);
            --input-bg: rgba(255,255,255,0.08);
            --sidebar-text: rgba(255,255,255,0.6);
            --sidebar-hover: rgba(255,255,255,0.05);
            --sidebar-active: rgba(255,255,255,0.1);
            --bubble-received: rgba(50,50,50,0.8);
            --bubble-received-text: #f5f5f7;
            --log-bg: rgba(255,255,255,0.03);
            --danmaku-bg: rgba(0,0,0,0.8);
        }

        html, body { height: 100%; overflow: hidden; }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'Segoe UI', Roboto, sans-serif;
            background: var(--bg-gradient);
            color: var(--text-primary);
            display: flex;
            flex-direction: column;
        }

        /* Topbar */
        .topbar {
            height: 48px;
            background: var(--sidebar-bg);
            backdrop-filter: var(--sidebar-blur);
            -webkit-backdrop-filter: var(--sidebar-blur);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 20px;
            border-bottom: 1px solid var(--border);
            z-index: 10;
            flex-shrink: 0;
        }
        .topbar-title {
            font-size: 15px;
            font-weight: 600;
            color: var(--sidebar-text-active);
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .topbar-right {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .ws-status {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
            color: var(--sidebar-text);
        }
        .ws-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--error);
            transition: background 0.3s;
        }
        .ws-dot.connected { background: var(--success); }
        .ws-dot.reconnecting { background: var(--warning); animation: pulse 1s infinite; }
        @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
        .theme-toggle {
            background: none;
            border: none;
            color: var(--sidebar-text);
            cursor: pointer;
            padding: 4px;
            border-radius: 6px;
            display: flex;
            align-items: center;
            transition: background 0.2s;
        }
        .theme-toggle:hover { background: var(--sidebar-hover); }

        /* Main layout */
        .main {
            display: flex;
            flex: 1;
            overflow: hidden;
        }

        /* Sidebar */
        .sidebar {
            width: 220px;
            background: var(--sidebar-bg);
            backdrop-filter: var(--sidebar-blur);
            -webkit-backdrop-filter: var(--sidebar-blur);
            border-right: 1px solid var(--border);
            padding: 16px 12px;
            display: flex;
            flex-direction: column;
            gap: 2px;
            flex-shrink: 0;
            overflow-y: auto;
        }
        .sidebar-section {
            font-size: 11px;
            font-weight: 600;
            color: var(--sidebar-text);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            padding: 12px 12px 6px;
        }
        .nav-item {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 10px 12px;
            border-radius: 8px;
            color: var(--sidebar-text);
            text-decoration: none;
            font-size: 14px;
            cursor: pointer;
            transition: all 0.2s;
            user-select: none;
        }
        .nav-item:hover { background: var(--sidebar-hover); color: var(--sidebar-text-active); }
        .nav-item.active { background: var(--sidebar-active); color: var(--sidebar-text-active); font-weight: 500; }
        .nav-item i { width: 18px; height: 18px; }

        /* Content */
        .content {
            flex: 1;
            overflow-y: auto;
            padding: 24px;
        }
        .content-inner {
            max-width: 1200px;
            margin: 0 auto;
        }

        /* Panel */
        .panel {
            display: none;
            animation: fadeIn 0.3s ease;
        }
        .panel.active { display: block; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

        /* Cards */
        .card {
            background: var(--card-bg);
            backdrop-filter: var(--card-blur);
            -webkit-backdrop-filter: var(--card-blur);
            border-radius: 16px;
            padding: 20px;
            box-shadow: var(--card-shadow);
            border: 1px solid var(--border);
            margin-bottom: 16px;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .card:hover { transform: translateY(-2px); box-shadow: 0 12px 40px rgba(0,0,0,0.15); }
        .card-title {
            font-size: 18px;
            font-weight: 600;
            margin-bottom: 16px;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .card-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 16px;
            margin-bottom: 16px;
        }

        /* Stats */
        .stat-card {
            background: var(--card-bg);
            backdrop-filter: var(--card-blur);
            border-radius: 12px;
            padding: 16px;
            box-shadow: var(--card-shadow);
            border: 1px solid var(--border);
        }
        .stat-label { font-size: 13px; color: var(--text-secondary); margin-bottom: 4px; }
        .stat-value { font-size: 28px; font-weight: 700; }

        /* Buttons */
        .btn {
            padding: 8px 16px;
            border: none;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }
        .btn-primary { background: var(--accent); color: #fff; }
        .btn-primary:hover { background: var(--accent-hover); }
        .btn-danger { background: var(--error); color: #fff; }
        .btn-danger:hover { background: #CC2F26; }
        .btn-ghost { background: transparent; color: var(--accent); border: 1px solid var(--accent); }
        .btn-ghost:hover { background: rgba(0,122,255,0.1); }
        .btn-sm { padding: 5px 10px; font-size: 12px; }
        .btn:disabled { opacity: 0.5; cursor: not-allowed; }

        /* Inputs */
        .input, .select, .textarea {
            padding: 10px 14px;
            border: 1px solid var(--border);
            border-radius: 10px;
            background: var(--input-bg);
            color: var(--text-primary);
            font-size: 14px;
            font-family: inherit;
            outline: none;
            transition: border-color 0.2s;
            width: 100%;
        }
        .input:focus, .select:focus, .textarea:focus { border-color: var(--accent); }
        .textarea { resize: vertical; min-height: 80px; }
        .form-row { display: flex; gap: 8px; margin-bottom: 12px; }
        .form-row .input, .form-row .select { flex: 1; }
        .form-label { font-size: 13px; font-weight: 500; color: var(--text-secondary); margin-bottom: 4px; display: block; }
        .form-group { margin-bottom: 12px; }

        /* Tables */
        .table { width: 100%; border-collapse: collapse; }
        .table th, .table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--border); font-size: 13px; }
        .table th { font-weight: 600; color: var(--text-secondary); font-size: 12px; text-transform: uppercase; letter-spacing: 0.3px; }

        /* Badges */
        .badge {
            display: inline-flex;
            align-items: center;
            padding: 3px 10px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 500;
        }
        .badge-success { background: rgba(52,199,89,0.15); color: var(--success); }
        .badge-warning { background: rgba(255,149,0,0.15); color: var(--warning); }
        .badge-error { background: rgba(255,59,48,0.15); color: var(--error); }
        .badge-info { background: rgba(0,122,255,0.15); color: var(--accent); }

        /* User cards */
        .user-cards { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
        .user-card {
            background: var(--card-bg);
            border-radius: 12px;
            padding: 16px 20px;
            box-shadow: var(--card-shadow);
            border: 1px solid var(--border);
            display: flex;
            align-items: center;
            gap: 12px;
            cursor: pointer;
            transition: all 0.2s;
            min-width: 180px;
        }
        .user-card:hover { transform: translateY(-2px); }
        .user-card.online { border-color: var(--success); }
        .user-avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background: var(--accent);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
            font-weight: 600;
            font-size: 16px;
        }
        .user-info { flex: 1; }
        .user-name { font-weight: 600; font-size: 14px; }
        .user-status { font-size: 12px; color: var(--text-secondary); }

        /* Chat */
        .chat-layout { display: flex; gap: 0; height: calc(100vh - 140px); min-height: 400px; }
        .chat-sidebar {
            width: 240px;
            background: var(--card-bg);
            border-radius: 16px 0 0 16px;
            border-right: 1px solid var(--border);
            overflow-y: auto;
        }
        .chat-main {
            flex: 1;
            display: flex;
            flex-direction: column;
            background: var(--card-bg);
            border-radius: 0 16px 16px 0;
        }
        .conv-item {
            padding: 12px 16px;
            cursor: pointer;
            border-bottom: 1px solid var(--border);
            transition: background 0.2s;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .conv-item:hover { background: var(--sidebar-hover); }
        .conv-item.active { background: var(--sidebar-active); }
        .conv-name { font-weight: 500; font-size: 14px; }
        .conv-preview { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
        .unread-badge {
            background: var(--error);
            color: #fff;
            font-size: 11px;
            font-weight: 600;
            padding: 2px 7px;
            border-radius: 10px;
            min-width: 18px;
            text-align: center;
        }
        .chat-header {
            padding: 12px 16px;
            border-bottom: 1px solid var(--border);
            font-weight: 600;
            font-size: 15px;
        }
        .chat-messages {
            flex: 1;
            overflow-y: auto;
            padding: 16px;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .message-bubble {
            max-width: 70%;
            padding: 10px 14px;
            border-radius: 16px;
            font-size: 14px;
            line-height: 1.4;
            animation: fadeIn 0.3s ease;
        }
        .message-bubble.sent {
            background: var(--bubble-sent);
            color: var(--bubble-sent-text);
            align-self: flex-end;
            border-bottom-right-radius: 4px;
        }
        .message-bubble.received {
            background: var(--bubble-received);
            color: var(--bubble-received-text);
            align-self: flex-start;
            border-bottom-left-radius: 4px;
        }
        .message-meta {
            font-size: 11px;
            color: var(--text-secondary);
            margin-top: 4px;
        }
        .message-bubble.sent .message-meta { color: rgba(255,255,255,0.7); text-align: right; }
        .chat-input {
            padding: 12px 16px;
            border-top: 1px solid var(--border);
            display: flex;
            gap: 8px;
        }
        .chat-input .input { flex: 1; }

        /* Danmaku */
        .danmaku-area {
            position: relative;
            height: 300px;
            background: var(--danmaku-bg);
            border-radius: 12px;
            overflow: hidden;
            margin-bottom: 16px;
        }
        .danmaku-overlay {
            position: absolute;
            top: 12px;
            left: 16px;
            color: rgba(255,255,255,0.8);
            font-size: 14px;
            font-weight: 500;
        }
        .danmaku-item {
            position: absolute;
            white-space: nowrap;
            color: #fff;
            font-size: 18px;
            font-weight: 500;
            text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
            animation: danmakuFloat 8s linear forwards;
        }
        @keyframes danmakuFloat {
            from { transform: translateX(100vw); }
            to { transform: translateX(-100%); }
        }
        .danmaku-input { display: flex; gap: 8px; margin-bottom: 16px; }
        .danmaku-input .input { flex: 1; }

        /* Order cards */
        .order-card {
            background: var(--card-bg);
            border-radius: 12px;
            padding: 16px;
            box-shadow: var(--card-shadow);
            border: 1px solid var(--border);
            margin-bottom: 12px;
        }
        .order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
        .order-id { font-weight: 600; font-size: 14px; }
        .order-detail { font-size: 13px; color: var(--text-secondary); }
        .status-timeline {
            display: flex;
            gap: 0;
            margin-top: 12px;
            position: relative;
        }
        .status-step {
            flex: 1;
            text-align: center;
            font-size: 11px;
            color: var(--text-secondary);
            position: relative;
            padding-top: 20px;
        }
        .status-step::before {
            content: '';
            position: absolute;
            top: 6px;
            left: 0;
            right: 0;
            height: 3px;
            background: var(--border);
            border-radius: 2px;
        }
        .status-step.active::before { background: var(--accent); }
        .status-step.active { color: var(--accent); font-weight: 600; }

        /* Log panel */
        .log-panel {
            background: var(--log-bg);
            border-radius: 12px;
            border: 1px solid var(--border);
            margin-top: 16px;
            overflow: hidden;
        }
        .log-header {
            padding: 10px 16px;
            font-size: 13px;
            font-weight: 600;
            color: var(--text-secondary);
            border-bottom: 1px solid var(--border);
            display: flex;
            justify-content: space-between;
            align-items: center;
            cursor: pointer;
        }
        .log-body {
            max-height: 200px;
            overflow-y: auto;
            padding: 8px 16px;
            font-family: 'SF Mono', 'Fira Code', monospace;
            font-size: 12px;
            line-height: 1.6;
        }
        .log-entry { color: var(--text-secondary); }
        .log-entry .log-time { color: var(--text-secondary); margin-right: 8px; }
        .log-entry .log-type { color: var(--accent); margin-right: 8px; font-weight: 500; }
        .log-entry .log-data { color: var(--text-primary); }

        /* Metrics charts */
        .chart-container {
            background: var(--card-bg);
            border-radius: 12px;
            padding: 16px;
            box-shadow: var(--card-shadow);
            border: 1px solid var(--border);
            margin-bottom: 16px;
        }
        .chart-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
        canvas { width: 100%; height: 200px; }

        /* Toast */
        .toast-container {
            position: fixed;
            top: 60px;
            right: 20px;
            z-index: 1000;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .toast {
            background: var(--card-bg);
            backdrop-filter: var(--card-blur);
            border-radius: 12px;
            padding: 12px 16px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.2);
            border: 1px solid var(--border);
            font-size: 14px;
            animation: slideIn 0.3s ease;
            max-width: 320px;
        }
        @keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }

        /* Responsive */
        @media (max-width: 768px) {
            .sidebar { width: 60px; padding: 12px 6px; }
            .nav-item span { display: none; }
            .sidebar-section { display: none; }
            .chat-layout { flex-direction: column; height: auto; }
            .chat-sidebar { width: 100%; border-radius: 16px 16px 0 0; max-height: 200px; }
            .chat-main { border-radius: 0 0 16px 16px; }
        }

        /* Scrollbar */
        ::-webkit-scrollbar { width: 6px; }
        ::-webkit-scrollbar-track { background: transparent; }
        ::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.15); border-radius: 3px; }
        ::-webkit-scrollbar-thumb:hover { background: rgba(0,0,0,0.25); }
    </style>
</head>
<body>
    <!-- Topbar -->
    <div class="topbar">
        <div class="topbar-title">
            <i data-lucide="radio" style="width:20px;height:20px"></i>
            Zhi PM Showcase
        </div>
        <div class="topbar-right">
            <div class="ws-status">
                <div class="ws-dot" id="ws-dot"></div>
                <span id="ws-label">Disconnected</span>
            </div>
            <button class="theme-toggle" onclick="toggleTheme()" title="Toggle theme">
                <i data-lucide="moon" id="theme-icon" style="width:18px;height:18px"></i>
            </button>
        </div>
    </div>

    <div class="main">
        <!-- Sidebar -->
        <nav class="sidebar">
            <div class="sidebar-section">Modules</div>
            <a class="nav-item active" data-panel="presence" onclick="navigate('presence')">
                <i data-lucide="activity"></i><span>Online Presence</span>
            </a>
            <a class="nav-item" data-panel="push" onclick="navigate('push')">
                <i data-lucide="send"></i><span>Message Push</span>
            </a>
            <a class="nav-item" data-panel="danmaku" onclick="navigate('danmaku')">
                <i data-lucide="radio"></i><span>Live Danmaku</span>
            </a>
            <a class="nav-item" data-panel="chat" onclick="navigate('chat')">
                <i data-lucide="message-square"></i><span>Chat Room</span>
            </a>
            <a class="nav-item" data-panel="orders" onclick="navigate('orders')">
                <i data-lucide="shopping-cart"></i><span>Order Tracking</span>
            </a>
            <div class="sidebar-section">Admin</div>
            <a class="nav-item" data-panel="admin" onclick="navigate('admin')">
                <i data-lucide="settings"></i><span>Admin Panel</span>
            </a>
            <a class="nav-item" data-panel="metrics" onclick="navigate('metrics')">
                <i data-lucide="bar-chart-3"></i><span>Metrics</span>
            </a>
        </nav>

        <!-- Content -->
        <div class="content">
            <div class="content-inner">
                <!-- Panels will be inserted here by JS -->
                <div id="panel-presence" class="panel active"></div>
                <div id="panel-push" class="panel"></div>
                <div id="panel-danmaku" class="panel"></div>
                <div id="panel-chat" class="panel"></div>
                <div id="panel-orders" class="panel"></div>
                <div id="panel-admin" class="panel"></div>
                <div id="panel-metrics" class="panel"></div>
            </div>
        </div>
    </div>

    <div class="toast-container" id="toasts"></div>

    <script>
    // ============================================================
    // JS modules will be added in subsequent tasks
    // ============================================================
    </script>
</body>
</html>
```

- [ ] **Step 2: Verify the file loads in a browser**

The file should render the sidebar, topbar, and empty content area with the glassmorphism background gradient.

- [ ] **Step 3: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/resources/static/index.html
git commit -m "feat(showcase): 创建前端 HTML 结构和 Cupertino 毛玻璃 CSS 主题"
```

---

### Task 4: Add JS — Core Utilities (Store, Router, WebSocket Client, Toast)

**Files:**
- Modify: `samples/showcase/sample-showcase/src/main/resources/static/index.html`

- [ ] **Step 1: Add core JS modules into the `<script>` tag**

Replace the placeholder script content with:

```javascript
// === Config ===
const BASE = '';  // same origin
const WS_BASE = `ws://${location.host}`;

// === Store ===
const Store = {
    currentUser: null,
    wsClients: {},
    logs: [],
    orders: [],
    theme: localStorage.getItem('theme') || 'light',
    listeners: [],

    set(key, value) {
        this[key] = value;
        this.listeners.forEach(fn => fn(key, value));
    },

    subscribe(fn) {
        this.listeners.push(fn);
    },

    addLog(type, data, module) {
        const entry = { time: new Date().toLocaleTimeString(), type, data, module };
        this.logs.unshift(entry);
        if (this.logs.length > 200) this.logs.pop();
        this.listeners.forEach(fn => fn('log', entry));
    }
};

// === Toast ===
function showToast(message, type = 'info') {
    const container = document.getElementById('toasts');
    const toast = document.createElement('div');
    toast.className = 'toast';
    const colors = { info: 'var(--accent)', success: 'var(--success)', warning: 'var(--warning)', error: 'var(--error)' };
    toast.style.borderLeft = `3px solid ${colors[type] || colors.info}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(() => toast.remove(), 300); }, 3000);
}

// === WebSocket Client ===
class WsClient {
    constructor(name, url, userId) {
        this.name = name;
        this.url = url;
        this.userId = userId;
        this.ws = null;
        this.handlers = new Map();
        this.reconnectDelay = 1000;
        this.maxReconnectDelay = 30000;
        this.connected = false;
        this.shouldConnect = true;
    }

    connect() {
        if (this.ws && this.ws.readyState <= 1) return;
        this.ws = new WebSocket(this.url);
        this.ws.onopen = () => {
            this.connected = true;
            this.reconnectDelay = 1000;
            updateWsStatus();
            Store.addLog('CONNECT', `${this.name}: ${this.userId}`, this.name);
            showToast(`${this.name} connected as ${this.userId}`, 'success');
        };
        this.ws.onmessage = (e) => {
            try {
                const msg = JSON.parse(e.data);
                Store.addLog('RECV', `${msg.type}: ${JSON.stringify(msg.payload || {}).substring(0, 80)}`, this.name);
                const handler = this.handlers.get(msg.type);
                if (handler) handler(msg);
                const wildcard = this.handlers.get('*');
                if (wildcard) wildcard(msg);
            } catch (err) {
                Store.addLog('ERROR', `Parse error: ${e.data}`, this.name);
            }
        };
        this.ws.onclose = () => {
            this.connected = false;
            updateWsStatus();
            Store.addLog('DISCONNECT', `${this.name}: ${this.userId}`, this.name);
            if (this.shouldConnect) {
                setTimeout(() => this.connect(), this.reconnectDelay);
                this.reconnectDelay = Math.min(this.reconnectDelay * 2, this.maxReconnectDelay);
            }
        };
        this.ws.onerror = () => {};
    }

    send(type, payload) {
        if (!this.ws || this.ws.readyState !== 1) {
            showToast('Not connected', 'error');
            return;
        }
        const msg = { type, payload, from: this.userId, timestamp: new Date().toISOString() };
        this.ws.send(JSON.stringify(msg));
        Store.addLog('SEND', `${type}: ${JSON.stringify(payload || {}).substring(0, 80)}`, this.name);
    }

    on(type, handler) {
        this.handlers.set(type, handler);
    }

    disconnect() {
        this.shouldConnect = false;
        if (this.ws) this.ws.close();
        this.connected = false;
        updateWsStatus();
    }
}

function updateWsStatus() {
    const clients = Object.values(Store.wsClients);
    const connected = clients.filter(c => c.connected).length;
    const dot = document.getElementById('ws-dot');
    const label = document.getElementById('ws-label');
    if (connected === 0) {
        dot.className = 'ws-dot';
        label.textContent = 'Disconnected';
    } else if (connected < clients.length) {
        dot.className = 'ws-dot reconnecting';
        label.textContent = `${connected}/${clients.length} connected`;
    } else {
        dot.className = 'ws-dot connected';
        label.textContent = `${connected} connection${connected > 1 ? 's' : ''}`;
    }
}

// === Router ===
function navigate(panel) {
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    const el = document.getElementById('panel-' + panel);
    const nav = document.querySelector(`.nav-item[data-panel="${panel}"]`);
    if (el) el.classList.add('active');
    if (nav) nav.classList.add('active');
    location.hash = panel;
    if (typeof window['render_' + panel] === 'function') window['render_' + panel]();
}

function handleHash() {
    const hash = location.hash.replace('#', '') || 'presence';
    navigate(hash);
}

// === Theme ===
function toggleTheme() {
    Store.theme = Store.theme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', Store.theme);
    localStorage.setItem('theme', Store.theme);
    const icon = document.getElementById('theme-icon');
    icon.setAttribute('data-lucide', Store.theme === 'dark' ? 'sun' : 'moon');
    lucide.createIcons();
}

// === API helpers ===
async function api(path, options = {}) {
    try {
        const resp = await fetch(BASE + path, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
        return await resp.json();
    } catch (e) {
        showToast(`API error: ${e.message}`, 'error');
        return null;
    }
}

// === Init ===
document.addEventListener('DOMContentLoaded', () => {
    if (Store.theme === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
        const icon = document.getElementById('theme-icon');
        icon.setAttribute('data-lucide', 'sun');
    }
    lucide.createIcons();
    handleHash();
});

window.addEventListener('hashchange', handleHash);
```

- [ ] **Step 2: Verify navigation works**

Clicking sidebar items should switch panels and update the URL hash.

- [ ] **Step 3: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/resources/static/index.html
git commit -m "feat(showcase): 实现核心 JS 工具(Store, Router, WsClient, Toast)"
```

---

### Task 5: Add Presence Module

**Files:**
- Modify: `samples/showcase/sample-showcase/src/main/resources/static/index.html`

- [ ] **Step 1: Add presence rendering function**

Add before the closing `</script>` tag:

```javascript
// === Presence Module ===
const PRESENCE_PORT = location.port || '8088';
const PRESENCE_WS = `${WS_BASE}/ws`;

function render_presence() {
    const panel = document.getElementById('panel-presence');
    panel.innerHTML = `
        <div class="card">
            <div class="card-title"><i data-lucide="activity" style="width:20px;height:20px"></i> Online Presence</div>
            <p style="color:var(--text-secondary);margin-bottom:16px;font-size:14px">
                Click user cards to connect/disconnect. Send push notifications and broadcasts.
            </p>
            <div class="user-cards" id="presence-users"></div>
        </div>
        <div class="card-grid">
            <div class="stat-card"><div class="stat-label">Connections</div><div class="stat-value" id="presence-stat-conn">-</div></div>
            <div class="stat-card"><div class="stat-label">Online Users</div><div class="stat-value" id="presence-stat-users">-</div></div>
        </div>
        <div class="card">
            <div class="card-title">Send Notification</div>
            <div class="form-row">
                <select class="select" id="presence-target-user" style="max-width:150px">
                    <option value="alice">Alice</option><option value="bob">Bob</option><option value="charlie">Charlie</option>
                </select>
                <input class="input" id="presence-push-msg" placeholder="Message..." value="Hello from showcase!">
                <button class="btn btn-primary" onclick="presencePush()"><i data-lucide="send" style="width:14px;height:14px"></i> Send</button>
            </div>
            <div class="form-row">
                <input class="input" id="presence-broadcast-msg" placeholder="Broadcast message..." value="Broadcast to all!">
                <button class="btn btn-ghost" onclick="presenceBroadcast()"><i data-lucide="radio" style="width:14px;height:14px"></i> Broadcast</button>
            </div>
        </div>
        <div class="log-panel">
            <div class="log-header">Message Log <button class="btn btn-sm btn-ghost" onclick="clearLog('presence')">Clear</button></div>
            <div class="log-body" id="presence-log"></div>
        </div>
    `;
    lucide.createIcons();
    renderPresenceUsers();
    presenceRefreshStats();
}

function renderPresenceUsers() {
    const users = ['alice', 'bob', 'charlie'];
    const container = document.getElementById('presence-users');
    container.innerHTML = users.map(u => {
        const client = Store.wsClients['presence-' + u];
        const online = client && client.connected;
        return `
            <div class="user-card ${online ? 'online' : ''}" onclick="presenceToggleUser('${u}')">
                <div class="user-avatar">${u[0].toUpperCase()}</div>
                <div class="user-info">
                    <div class="user-name">${u.charAt(0).toUpperCase() + u.slice(1)}</div>
                    <div class="user-status">${online ? 'Online' : 'Offline'}</div>
                </div>
                <span class="badge ${online ? 'badge-success' : 'badge-error'}">${online ? 'ON' : 'OFF'}</span>
            </div>
        `;
    }).join('');
}

function presenceToggleUser(userId) {
    const key = 'presence-' + userId;
    if (Store.wsClients[key] && Store.wsClients[key].connected) {
        Store.wsClients[key].disconnect();
        delete Store.wsClients[key];
    } else {
        const client = new WsClient('presence', `${PRESENCE_WS}?access_token=${userId}-token`, userId);
        client.on('*', (msg) => {
            renderPresenceLog(msg);
            showToast(`[${userId}] ${msg.type}: ${JSON.stringify(msg.payload || {}).substring(0, 50)}`, 'info');
        });
        Store.wsClients[key] = client;
        client.connect();
    }
    setTimeout(() => { renderPresenceUsers(); presenceRefreshStats(); }, 500);
}

async function presencePush() {
    const userId = document.getElementById('presence-target-user').value;
    const message = document.getElementById('presence-push-msg').value;
    await api(`/api/push/users/${userId}`, { method: 'POST', body: JSON.stringify({ message }) });
    showToast(`Pushed to ${userId}`, 'success');
}

async function presenceBroadcast() {
    const message = document.getElementById('presence-broadcast-msg').value;
    const result = await api(`/api/push/broadcast`, { method: 'POST', body: JSON.stringify({ message }) });
    showToast(`Broadcast sent to ${result?.sent || 0} connections`, 'success');
}

async function presenceRefreshStats() {
    const count = await api('/api/presence/connections/count');
    if (count) document.getElementById('presence-stat-conn').textContent = count.count || 0;
    const users = ['alice', 'bob', 'charlie'];
    let onlineCount = 0;
    for (const u of users) {
        const status = await api(`/api/presence/users/${u}/online`);
        if (status && status.online) onlineCount++;
    }
    document.getElementById('presence-stat-users').textContent = onlineCount;
}

function renderPresenceLog(msg) {
    const log = document.getElementById('presence-log');
    if (!log) return;
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    entry.innerHTML = `<span class="log-time">${new Date().toLocaleTimeString()}</span><span class="log-type">${msg.type}</span><span class="log-data">${JSON.stringify(msg.payload || {}).substring(0, 100)}</span>`;
    log.prepend(entry);
}

function clearLog(module) {
    const log = document.getElementById(module + '-log');
    if (log) log.innerHTML = '';
}
```

- [ ] **Step 2: Verify presence module renders**

Navigate to `#presence`. User cards should appear. Clicking a card should attempt WebSocket connection.

- [ ] **Step 3: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/resources/static/index.html
git commit -m "feat(showcase): 实现在线状态模块(连接/断开/推送/广播)"
```

---

### Task 6: Add Push Module

**Files:**
- Modify: `samples/showcase/sample-showcase/src/main/resources/static/index.html`

- [ ] **Step 1: Add push rendering function**

```javascript
// === Push Module ===
function render_push() {
    const panel = document.getElementById('panel-push');
    panel.innerHTML = `
        <div class="card">
            <div class="card-title"><i data-lucide="send" style="width:20px;height:20px"></i> Message Push</div>
            <p style="color:var(--text-secondary);margin-bottom:16px;font-size:14px">
                Connect as a user, then send messages via REST API. Messages arrive via WebSocket echo.
            </p>
            <div class="form-row">
                <select class="select" id="push-user" style="max-width:150px" onchange="pushConnect()">
                    <option value="">Select user...</option>
                    <option value="alice">Alice</option><option value="bob">Bob</option><option value="charlie">Charlie</option>
                </select>
                <span id="push-status" class="badge badge-error">Not connected</span>
            </div>
        </div>
        <div class="card">
            <div class="card-title">Send Push</div>
            <div class="form-group">
                <label class="form-label">Target Type</label>
                <select class="select" id="push-type">
                    <option value="user">User</option>
                    <option value="broadcast">Broadcast</option>
                    <option value="room">Room</option>
                </select>
            </div>
            <div class="form-group" id="push-target-group">
                <label class="form-label">Target</label>
                <input class="input" id="push-target" placeholder="User ID or Room ID">
            </div>
            <div class="form-group">
                <label class="form-label">Message</label>
                <input class="input" id="push-message" placeholder="Message content" value="Hello from push module!">
            </div>
            <button class="btn btn-primary" onclick="pushSend()"><i data-lucide="send" style="width:14px;height:14px"></i> Send</button>
        </div>
        <div class="log-panel">
            <div class="log-header">Received Messages <button class="btn btn-sm btn-ghost" onclick="clearLog('push')">Clear</button></div>
            <div class="log-body" id="push-log"></div>
        </div>
    `;
    lucide.createIcons();
}

function pushConnect() {
    const userId = document.getElementById('push-user').value;
    if (Store.wsClients['push']) Store.wsClients['push'].disconnect();
    if (!userId) return;
    const client = new WsClient('push', `${WS_BASE}/ws?access_token=${userId}-token`, userId);
    client.on('*', (msg) => {
        const log = document.getElementById('push-log');
        if (log) {
            const entry = document.createElement('div');
            entry.className = 'log-entry';
            entry.innerHTML = `<span class="log-time">${new Date().toLocaleTimeString()}</span><span class="log-type">${msg.type}</span><span class="log-data">${JSON.stringify(msg.payload || {})}</span>`;
            log.prepend(entry);
        }
    });
    Store.wsClients['push'] = client;
    client.connect();
    setTimeout(() => {
        const status = document.getElementById('push-status');
        if (status) {
            status.textContent = client.connected ? `Connected as ${userId}` : 'Connecting...';
            status.className = client.connected ? 'badge badge-success' : 'badge badge-warning';
        }
    }, 500);
}

async function pushSend() {
    const type = document.getElementById('push-type').value;
    const target = document.getElementById('push-target').value;
    const message = document.getElementById('push-message').value;
    const body = JSON.stringify({ message });
    let url = '';
    if (type === 'user') url = `/api/push/users/${target}`;
    else if (type === 'broadcast') url = '/api/push/broadcast';
    else url = `/api/push/rooms/${target}`;
    const result = await api(url, { method: 'POST', body });
    showToast(`Sent to ${result?.sent || 0} connections`, 'success');
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/resources/static/index.html
git commit -m "feat(showcase): 实现消息推送模块(用户/广播/房间)"
```

---

### Task 7: Add Danmaku Module

**Files:**
- Modify: `samples/showcase/sample-showcase/src/main/resources/static/index.html`

- [ ] **Step 1: Add danmaku rendering function**

```javascript
// === Danmaku Module ===
let danmakuUser = null;

function render_danmaku() {
    const panel = document.getElementById('panel-danmaku');
    panel.innerHTML = `
        <div class="card">
            <div class="card-title"><i data-lucide="radio" style="width:20px;height:20px"></i> Live Danmaku</div>
            <p style="color:var(--text-secondary);margin-bottom:16px;font-size:14px">
                Simulated live stream with floating danmaku messages. Rate limited to 5 msg/user/sec.
            </p>
            <div class="form-row">
                <select class="select" id="danmaku-user" style="max-width:150px" onchange="danmakuConnect()">
                    <option value="">Select user...</option>
                    <option value="alice">Alice</option><option value="bob">Bob</option><option value="charlie">Charlie</option>
                </select>
                <span id="danmaku-status" class="badge badge-error">Not connected</span>
                <span id="danmaku-count" class="badge badge-info">0 viewers</span>
            </div>
        </div>
        <div class="danmaku-area" id="danmaku-screen">
            <div class="danmaku-overlay">
                <div style="font-size:18px;font-weight:600">LIVE: Room live-room-1</div>
                <div style="font-size:12px;opacity:0.7;margin-top:4px">Zhi PM Danmaku Demo</div>
            </div>
        </div>
        <div class="danmaku-input">
            <input class="input" id="danmaku-content" placeholder="Send danmaku..." onkeydown="if(event.key==='Enter')danmakuSend()">
            <button class="btn btn-primary" onclick="danmakuSend()"><i data-lucide="send" style="width:14px;height:14px"></i></button>
        </div>
        <div class="card-grid">
            <div class="card">
                <div class="card-title" style="font-size:15px">Muted Users</div>
                <div id="danmaku-muted"><span style="color:var(--text-secondary);font-size:13px">None</span></div>
                <div class="form-row" style="margin-top:12px">
                    <input class="input" id="danmaku-mute-user" placeholder="User ID" style="max-width:120px">
                    <button class="btn btn-sm btn-danger" onclick="danmakuMute()">Mute</button>
                    <button class="btn btn-sm btn-ghost" onclick="danmakuUnmute()">Unmute</button>
                </div>
            </div>
            <div class="card">
                <div class="card-title" style="font-size:15px">Rate Limit Info</div>
                <p style="font-size:13px;color:var(--text-secondary)">Max 5 messages per user per second. Content filtered for sensitive words (spam, ads).</p>
                <div id="danmaku-rate-info" style="margin-top:8px;font-size:13px"></div>
            </div>
        </div>
    `;
    lucide.createIcons();
    danmakuRefreshMuted();
    danmakuRefreshCount();
}

function danmakuConnect() {
    const userId = document.getElementById('danmaku-user').value;
    if (Store.wsClients['danmaku']) Store.wsClients['danmaku'].disconnect();
    if (!userId) return;
    danmakuUser = userId;
    const client = new WsClient('danmaku', `${WS_BASE}/ws?access_token=${userId}-token`, userId);
    client.on('danmaku.message', (msg) => {
        const content = msg.payload?.content || '';
        const sender = msg.from || 'unknown';
        spawnDanmaku(`${sender}: ${content}`);
    });
    client.on('*', (msg) => {
        if (msg.type === 'danmaku.error') {
            showToast(`Danmaku error: ${msg.payload?.message || 'unknown'}`, 'error');
        }
    });
    Store.wsClients['danmaku'] = client;
    client.connect();
    client.send('room.join', { roomId: 'live-room-1' });
    setTimeout(() => {
        const status = document.getElementById('danmaku-status');
        if (status) {
            status.textContent = client.connected ? `${userId} in live-room-1` : 'Connecting...';
            status.className = client.connected ? 'badge badge-success' : 'badge badge-warning';
        }
    }, 500);
}

function spawnDanmaku(text) {
    const screen = document.getElementById('danmaku-screen');
    if (!screen) return;
    const el = document.createElement('div');
    el.className = 'danmaku-item';
    el.textContent = text;
    el.style.top = Math.random() * 250 + 20 + 'px';
    screen.appendChild(el);
    setTimeout(() => el.remove(), 8000);
}

function danmakuSend() {
    const input = document.getElementById('danmaku-content');
    const content = input.value.trim();
    if (!content) return;
    const client = Store.wsClients['danmaku'];
    if (!client || !client.connected) {
        showToast('Connect first', 'warning');
        return;
    }
    client.send('danmaku.send', { roomId: 'live-room-1', content, contentType: 'text' });
    input.value = '';
}

async function danmakuMute() {
    const userId = document.getElementById('danmaku-mute-user').value;
    if (!userId) return;
    await api(`/api/danmaku/rooms/live-room-1/mute/${userId}`, { method: 'POST' });
    showToast(`Muted ${userId}`, 'warning');
    danmakuRefreshMuted();
}

async function danmakuUnmute() {
    const userId = document.getElementById('danmaku-mute-user').value;
    if (!userId) return;
    await api(`/api/danmaku/rooms/live-room-1/unmute/${userId}`, { method: 'POST' });
    showToast(`Unmuted ${userId}`, 'success');
    danmakuRefreshMuted();
}

async function danmakuRefreshMuted() {
    const data = await api('/api/danmaku/rooms/live-room-1/muted');
    const el = document.getElementById('danmaku-muted');
    if (el && data) {
        const users = data.mutedUsers || [];
        el.innerHTML = users.length ? users.map(u => `<span class="badge badge-warning" style="margin-right:4px">${u}</span>`).join('') : '<span style="color:var(--text-secondary);font-size:13px">None</span>';
    }
}

async function danmakuRefreshCount() {
    const data = await api('/api/presence/rooms/live-room-1/count');
    const el = document.getElementById('danmaku-count');
    if (el && data) el.textContent = `${data.count || 0} viewers`;
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/resources/static/index.html
git commit -m "feat(showcase): 实现弹幕直播模块(飘屏/禁言/限流)"
```

---

### Task 8: Add Chat Module

**Files:**
- Modify: `samples/showcase/sample-showcase/src/main/resources/static/index.html`

- [ ] **Step 1: Add chat rendering function**

```javascript
// === Chat Module ===
let chatUser = null;
let chatConversations = [
    { id: 'conv-alice-bob', name: 'Alice ↔ Bob', type: 'single', members: ['alice', 'bob'] },
    { id: 'conv-alice-charlie', name: 'Alice ↔ Charlie', type: 'single', members: ['alice', 'charlie'] },
    { id: 'group-team', name: 'Team Chat', type: 'group', members: ['alice', 'bob', 'charlie'] }
];
let activeConv = null;
let chatMessages = {};

function render_chat() {
    const panel = document.getElementById('panel-chat');
    panel.innerHTML = `
        <div class="card" style="padding:12px;margin-bottom:12px">
            <div class="form-row" style="margin-bottom:0">
                <span style="font-weight:600;font-size:14px">Chat as:</span>
                <select class="select" id="chat-user" style="max-width:150px" onchange="chatSwitchUser()">
                    <option value="">Select user...</option>
                    <option value="alice">Alice</option><option value="bob">Bob</option><option value="charlie">Charlie</option>
                </select>
                <span id="chat-status" class="badge badge-error">Offline</span>
            </div>
        </div>
        <div class="chat-layout">
            <div class="chat-sidebar">
                <div style="padding:12px 16px;font-weight:600;font-size:14px;border-bottom:1px solid var(--border)">Conversations</div>
                <div id="chat-conv-list"></div>
            </div>
            <div class="chat-main">
                <div class="chat-header" id="chat-header">Select a conversation</div>
                <div class="chat-messages" id="chat-messages"><div style="text-align:center;color:var(--text-secondary);padding:40px">Select a conversation to start chatting</div></div>
                <div class="chat-input">
                    <input class="input" id="chat-input" placeholder="Type a message..." onkeydown="if(event.key==='Enter')chatSendMessage()">
                    <button class="btn btn-primary" onclick="chatSendMessage()"><i data-lucide="send" style="width:14px;height:14px"></i></button>
                </div>
            </div>
        </div>
    `;
    lucide.createIcons();
    renderConvList();
}

function renderConvList() {
    const list = document.getElementById('chat-conv-list');
    if (!list) return;
    list.innerHTML = chatConversations.map(c => {
        const msgs = chatMessages[c.id] || [];
        const last = msgs.length > 0 ? msgs[msgs.length - 1].content.substring(0, 30) : 'No messages';
        return `
            <div class="conv-item ${activeConv === c.id ? 'active' : ''}" onclick="chatSelectConv('${c.id}')">
                <div>
                    <div class="conv-name">${c.name}</div>
                    <div class="conv-preview">${last}</div>
                </div>
            </div>
        `;
    }).join('');
}

function chatSwitchUser() {
    const userId = document.getElementById('chat-user').value;
    if (Store.wsClients['chat']) Store.wsClients['chat'].disconnect();
    if (!userId) return;
    chatUser = userId;
    const client = new WsClient('chat', `${WS_BASE}/ws?access_token=${userId}-token`, userId);
    client.on('chat.message', (msg) => {
        const p = msg.payload || {};
        const convId = p.conversationId;
        if (!chatMessages[convId]) chatMessages[convId] = [];
        chatMessages[convId].push({ id: p.messageId, sender: p.senderId, content: p.content, time: new Date().toLocaleTimeString() });
        if (activeConv === convId) renderChatMessages(convId);
        renderConvList();
        // Auto ACK
        client.send('chat.ack', { messageId: p.messageId });
    });
    Store.wsClients['chat'] = client;
    client.connect();
    setTimeout(() => {
        const status = document.getElementById('chat-status');
        if (status) {
            status.textContent = client.connected ? userId : 'Connecting...';
            status.className = client.connected ? 'badge badge-success' : 'badge badge-warning';
        }
    }, 500);
}

function chatSelectConv(convId) {
    activeConv = convId;
    const conv = chatConversations.find(c => c.id === convId);
    document.getElementById('chat-header').textContent = conv ? conv.name : convId;
    // Join room if group
    if (conv && conv.type === 'group' && Store.wsClients['chat']) {
        Store.wsClients['chat'].send('room.join', { roomId: convId });
    }
    // Send read receipt
    if (Store.wsClients['chat']) {
        Store.wsClients['chat'].send('chat.read', { conversationId: convId });
    }
    renderConvList();
    renderChatMessages(convId);
    // Load history
    loadChatHistory(convId);
}

async function loadChatHistory(convId) {
    const data = await api(`/api/chat/conversations/${convId}/history?limit=20`);
    if (data && data.messages) {
        chatMessages[convId] = data.messages.map(m => ({
            id: m.messageId, sender: m.senderId, content: m.content,
            time: new Date(m.createdAt).toLocaleTimeString()
        }));
        renderChatMessages(convId);
    }
}

function renderChatMessages(convId) {
    const container = document.getElementById('chat-messages');
    if (!container) return;
    const msgs = chatMessages[convId] || [];
    if (msgs.length === 0) {
        container.innerHTML = '<div style="text-align:center;color:var(--text-secondary);padding:40px">No messages yet</div>';
        return;
    }
    container.innerHTML = msgs.map(m => `
        <div class="message-bubble ${m.sender === chatUser ? 'sent' : 'received'}">
            ${m.sender !== chatUser ? `<div style="font-size:12px;font-weight:600;margin-bottom:2px">${m.sender}</div>` : ''}
            <div>${m.content}</div>
            <div class="message-meta">${m.time}</div>
        </div>
    `).join('');
    container.scrollTop = container.scrollHeight;
}

function chatSendMessage() {
    const input = document.getElementById('chat-input');
    const content = input.value.trim();
    if (!content || !activeConv || !chatUser) return;
    const client = Store.wsClients['chat'];
    if (!client || !client.connected) {
        showToast('Connect first', 'warning');
        return;
    }
    const conv = chatConversations.find(c => c.id === activeConv);
    client.send('chat.send', {
        conversationId: activeConv,
        conversationType: conv ? conv.type : 'single',
        content,
        contentType: 'text'
    });
    // Optimistic local add
    if (!chatMessages[activeConv]) chatMessages[activeConv] = [];
    chatMessages[activeConv].push({ id: 'local-' + Date.now(), sender: chatUser, content, time: new Date().toLocaleTimeString() });
    renderChatMessages(activeConv);
    renderConvList();
    input.value = '';
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/resources/static/index.html
git commit -m "feat(showcase): 实现聊天室模块(iMessage 风格气泡/已读回执/历史)"
```

---

### Task 9: Add Order Tracking Module

**Files:**
- Modify: `samples/showcase/sample-showcase/src/main/resources/static/index.html`

- [ ] **Step 1: Add order rendering function**

```javascript
// === Order Module ===
let orderUser = null;
const ORDER_STATUSES = ['CREATED', 'PAID', 'SHIPPING', 'DELIVERED', 'COMPLETED'];

function render_orders() {
    const panel = document.getElementById('panel-orders');
    panel.innerHTML = `
        <div class="card">
            <div class="card-title"><i data-lucide="shopping-cart" style="width:20px;height:20px"></i> Order Tracking</div>
            <p style="color:var(--text-secondary);margin-bottom:16px;font-size:14px">
                Create orders and track status changes in real-time via WebSocket.
            </p>
            <div class="form-row">
                <select class="select" id="order-user" style="max-width:150px" onchange="orderConnect()">
                    <option value="">Select user...</option>
                    <option value="alice">Alice</option><option value="bob">Bob</option><option value="charlie">Charlie</option>
                </select>
                <span id="order-status" class="badge badge-error">Offline</span>
            </div>
        </div>
        <div class="card">
            <div class="card-title">Create Order</div>
            <div class="form-row">
                <input class="input" id="order-product" placeholder="Product name" value="MacBook Pro">
                <input class="input" id="order-quantity" type="number" placeholder="Qty" value="1" style="max-width:80px">
                <button class="btn btn-primary" onclick="orderCreate()"><i data-lucide="plus" style="width:14px;height:14px"></i> Create</button>
            </div>
        </div>
        <div id="order-list"></div>
    `;
    lucide.createIcons();
    orderRefreshList();
}

function orderConnect() {
    const userId = document.getElementById('order-user').value;
    if (Store.wsClients['orders']) Store.wsClients['orders'].disconnect();
    if (!userId) return;
    orderUser = userId;
    const client = new WsClient('orders', `${WS_BASE}/ws?access_token=${userId}-token`, userId);
    client.on('order.created', (msg) => {
        showToast(`Order created: ${msg.payload?.orderId}`, 'success');
        orderRefreshList();
    });
    client.on('order.status.changed', (msg) => {
        showToast(`Order ${msg.payload?.orderId}: ${msg.payload?.status}`, 'info');
        orderRefreshList();
    });
    Store.wsClients['orders'] = client;
    client.connect();
    setTimeout(() => {
        const status = document.getElementById('order-status');
        if (status) {
            status.textContent = client.connected ? userId : 'Connecting...';
            status.className = client.connected ? 'badge badge-success' : 'badge badge-warning';
        }
    }, 500);
}

async function orderCreate() {
    if (!orderUser) { showToast('Select a user first', 'warning'); return; }
    const product = document.getElementById('order-product').value;
    const quantity = parseInt(document.getElementById('order-quantity').value) || 1;
    const result = await api('/api/orders', { method: 'POST', body: JSON.stringify({ userId: orderUser, product, quantity }) });
    if (result && result.orderId) {
        showToast(`Order ${result.orderId} created`, 'success');
        orderRefreshList();
    }
}

async function orderRefreshList() {
    if (!orderUser) return;
    const data = await api(`/api/orders/user/${orderUser}`);
    const container = document.getElementById('order-list');
    if (!container || !data) return;
    const orders = data.orders || [];
    if (orders.length === 0) {
        container.innerHTML = '<div class="card"><div style="text-align:center;color:var(--text-secondary);padding:20px">No orders yet</div></div>';
        return;
    }
    container.innerHTML = orders.map(o => {
        const statusIndex = ORDER_STATUSES.indexOf(o.status);
        return `
            <div class="order-card">
                <div class="order-header">
                    <span class="order-id">${o.orderId}</span>
                    <span class="badge ${o.status === 'COMPLETED' ? 'badge-success' : 'badge-info'}">${o.status}</span>
                </div>
                <div class="order-detail">${o.product} × ${o.quantity}</div>
                <div class="status-timeline">
                    ${ORDER_STATUSES.map((s, i) => `<div class="status-step ${i <= statusIndex ? 'active' : ''}">${s}</div>`).join('')}
                </div>
                ${statusIndex < ORDER_STATUSES.length - 1 ? `
                    <div style="margin-top:12px">
                        <button class="btn btn-sm btn-ghost" onclick="orderAdvance('${o.orderId}', '${ORDER_STATUSES[statusIndex + 1]}')">
                            Advance to ${ORDER_STATUSES[statusIndex + 1]}
                        </button>
                    </div>
                ` : ''}
            </div>
        `;
    }).join('');
}

async function orderAdvance(orderId, status) {
    await api(`/api/orders/${orderId}/status`, { method: 'POST', body: JSON.stringify({ status }) });
    orderRefreshList();
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/resources/static/index.html
git commit -m "feat(showcase): 实现订单追踪模块(创建/状态变更/时间线)"
```

---

### Task 10: Add Admin Module

**Files:**
- Modify: `samples/showcase/sample-showcase/src/main/resources/static/index.html`

- [ ] **Step 1: Add admin rendering function**

```javascript
// === Admin Module ===
function render_admin() {
    const panel = document.getElementById('panel-admin');
    panel.innerHTML = `
        <div class="card-grid" id="admin-stats"></div>
        <div class="card">
            <div class="card-title"><i data-lucide="users" style="width:20px;height:20px"></i> Active Connections
                <button class="btn btn-sm btn-ghost" style="margin-left:auto" onclick="adminLoadConnections()">Refresh</button>
            </div>
            <table class="table">
                <thead><tr><th>Session</th><th>User</th><th>Connected</th><th>Heartbeat</th><th>Action</th></tr></thead>
                <tbody id="admin-conn-table"><tr><td colspan="5" style="text-align:center;color:var(--text-secondary)">Loading...</td></tr></tbody>
            </table>
        </div>
        <div class="card">
            <div class="card-title"><i data-lucide="home" style="width:20px;height:20px"></i> Active Rooms
                <button class="btn btn-sm btn-ghost" style="margin-left:auto" onclick="adminLoadRooms()">Refresh</button>
            </div>
            <table class="table">
                <thead><tr><th>Room ID</th><th>Members</th><th>Action</th></tr></thead>
                <tbody id="admin-room-table"><tr><td colspan="3" style="text-align:center;color:var(--text-secondary)">Loading...</td></tr></tbody>
            </table>
        </div>
        <div class="card">
            <div class="card-title">Broadcast Message</div>
            <div class="form-row">
                <input class="input" id="admin-broadcast-msg" placeholder="Broadcast message...">
                <button class="btn btn-primary" onclick="adminBroadcast()">Send</button>
            </div>
        </div>
    `;
    lucide.createIcons();
    adminLoadStats();
    adminLoadConnections();
    adminLoadRooms();
}

async function adminLoadStats() {
    const stats = await api('/admin/api/stats');
    const container = document.getElementById('admin-stats');
    if (!container || !stats) return;
    container.innerHTML = `
        <div class="stat-card"><div class="stat-label">Connections</div><div class="stat-value">${stats.activeConnections || 0}</div></div>
        <div class="stat-card"><div class="stat-label">Online Users</div><div class="stat-value">${stats.onlineUsers || 0}</div></div>
        <div class="stat-card"><div class="stat-label">Rooms</div><div class="stat-value">${stats.activeRooms || 0}</div></div>
        <div class="stat-card"><div class="stat-label">Success Rate</div><div class="stat-value">${((stats.pushSuccessRate || 1) * 100).toFixed(1)}%</div></div>
    `;
}

async function adminLoadConnections() {
    const conns = await api('/admin/api/connections');
    const tbody = document.getElementById('admin-conn-table');
    if (!tbody || !conns) return;
    if (conns.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--text-secondary)">No connections</td></tr>';
        return;
    }
    tbody.innerHTML = conns.map(c => `
        <tr>
            <td style="font-family:monospace;font-size:12px">${c.sessionId?.substring(0, 12)}...</td>
            <td>${c.userId}</td>
            <td>${c.connectedAt ? new Date(c.connectedAt).toLocaleTimeString() : '-'}</td>
            <td>${c.lastHeartbeatAt ? new Date(c.lastHeartbeatAt).toLocaleTimeString() : '-'}</td>
            <td><button class="btn btn-sm btn-danger" onclick="adminKick('${c.sessionId}')">Kick</button></td>
        </tr>
    `).join('');
}

async function adminLoadRooms() {
    const rooms = await api('/admin/api/rooms');
    const tbody = document.getElementById('admin-room-table');
    if (!tbody || !rooms) return;
    if (rooms.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" style="text-align:center;color:var(--text-secondary)">No rooms</td></tr>';
        return;
    }
    tbody.innerHTML = rooms.map(r => `
        <tr>
            <td>${r.roomId}</td>
            <td>${r.memberCount}</td>
            <td><button class="btn btn-sm btn-ghost" onclick="adminViewMembers('${r.roomId}')">Members</button></td>
        </tr>
    `).join('');
}

async function adminKick(sessionId) {
    if (!confirm(`Kick session ${sessionId}?`)) return;
    await api(`/admin/api/connections/${sessionId}`, { method: 'DELETE' });
    showToast('Connection kicked', 'warning');
    adminLoadConnections();
}

async function adminViewMembers(roomId) {
    const data = await api(`/admin/api/rooms/${roomId}/members`);
    if (data) {
        const members = data.map(m => m.userId).join(', ');
        showToast(`Room ${roomId}: ${members}`, 'info');
    }
}

async function adminBroadcast() {
    const msg = document.getElementById('admin-broadcast-msg').value;
    if (!msg) return;
    const result = await api('/admin/api/broadcast', { method: 'POST', body: JSON.stringify({ type: 'admin.broadcast', payload: { message: msg } }) });
    showToast(`Broadcast sent to ${result?.sent || 0}`, 'success');
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/resources/static/index.html
git commit -m "feat(showcase): 实现管理后台模块(连接/房间/踢出/广播)"
```

---

### Task 11: Add Metrics Module

**Files:**
- Modify: `samples/showcase/sample-showcase/src/main/resources/static/index.html`

- [ ] **Step 1: Add metrics rendering function with canvas charts**

```javascript
// === Metrics Module ===
let metricsInterval = null;
let metricsHistory = { connections: [], messages: [], successRate: [] };
const METRICS_MAX_POINTS = 60;

function render_metrics() {
    if (metricsInterval) clearInterval(metricsInterval);
    const panel = document.getElementById('panel-metrics');
    panel.innerHTML = `
        <div class="card-grid" id="metrics-stats"></div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
            <div class="chart-container">
                <div class="chart-title">Active Connections</div>
                <canvas id="chart-connections"></canvas>
            </div>
            <div class="chart-container">
                <div class="chart-title">Success Rate (%)</div>
                <canvas id="chart-success"></canvas>
            </div>
        </div>
    `;
    metricsHistory = { connections: [], messages: [], successRate: [] };
    metricsRefresh();
    metricsInterval = setInterval(metricsRefresh, 2000);
}

async function metricsRefresh() {
    const stats = await api('/admin/api/stats');
    if (!stats) return;
    const now = new Date().toLocaleTimeString();
    metricsHistory.connections.push(stats.activeConnections || 0);
    metricsHistory.successRate.push((stats.pushSuccessRate || 1) * 100);
    if (metricsHistory.connections.length > METRICS_MAX_POINTS) {
        metricsHistory.connections.shift();
        metricsHistory.successRate.shift();
    }
    const container = document.getElementById('metrics-stats');
    if (container) {
        container.innerHTML = `
            <div class="stat-card"><div class="stat-label">Connections</div><div class="stat-value">${stats.activeConnections || 0}</div></div>
            <div class="stat-card"><div class="stat-label">Users</div><div class="stat-value">${stats.onlineUsers || 0}</div></div>
            <div class="stat-card"><div class="stat-label">Rooms</div><div class="stat-value">${stats.activeRooms || 0}</div></div>
            <div class="stat-card"><div class="stat-label">Push Rate</div><div class="stat-value">${((stats.pushSuccessRate || 1) * 100).toFixed(1)}%</div></div>
        `;
    }
    drawChart('chart-connections', metricsHistory.connections, '#007AFF');
    drawChart('chart-success', metricsHistory.successRate, '#34C759', 0, 100);
}

function drawChart(canvasId, data, color, minVal, maxVal) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || data.length < 2) return;
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);
    const w = rect.width, h = rect.height;
    const padding = { top: 10, right: 10, bottom: 20, left: 40 };
    const plotW = w - padding.left - padding.right;
    const plotH = h - padding.top - padding.bottom;
    ctx.clearRect(0, 0, w, h);
    const min = minVal !== undefined ? minVal : Math.min(...data) * 0.9;
    const max = maxVal !== undefined ? maxVal : Math.max(...data) * 1.1 || 1;
    // Grid lines
    ctx.strokeStyle = 'rgba(0,0,0,0.06)';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
        const y = padding.top + (plotH / 4) * i;
        ctx.beginPath();
        ctx.moveTo(padding.left, y);
        ctx.lineTo(w - padding.right, y);
        ctx.stroke();
        ctx.fillStyle = '#86868b';
        ctx.font = '11px -apple-system, sans-serif';
        ctx.textAlign = 'right';
        ctx.fillText((max - (max - min) * (i / 4)).toFixed(0), padding.left - 6, y + 4);
    }
    // Line
    ctx.beginPath();
    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.lineJoin = 'round';
    data.forEach((val, i) => {
        const x = padding.left + (plotW / (data.length - 1)) * i;
        const y = padding.top + plotH - ((val - min) / (max - min)) * plotH;
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    });
    ctx.stroke();
    // Fill
    const lastX = padding.left + plotW;
    const lastY = padding.top + plotH - ((data[data.length - 1] - min) / (max - min)) * plotH;
    ctx.lineTo(lastX, padding.top + plotH);
    ctx.lineTo(padding.left, padding.top + plotH);
    ctx.closePath();
    ctx.fillStyle = color.replace(')', ',0.1)').replace('rgb', 'rgba');
    ctx.fill();
}
```

- [ ] **Step 2: Commit**

```bash
git add samples/showcase/sample-showcase/src/main/resources/static/index.html
git commit -m "feat(showcase): 实现实时指标模块(Canvas 折线图/自动刷新)"
```

---

### Task 12: Verify Build and Integration Test

**Files:**
- None (verification only)

- [ ] **Step 1: Run full Maven build**

Run: `JAVA_HOME="/c/Program Files/Java/jdk-21" PATH="/c/Program Files/Java/jdk-21/bin:$PATH" mvn test -pl samples/showcase/sample-showcase -am`
Expected: BUILD SUCCESS, SampleShowcaseApplicationTest passes

- [ ] **Step 2: Run full project test suite**

Run: `JAVA_HOME="/c/Program Files/Java/jdk-21" PATH="/c/Program Files/Java/jdk-21/bin:$PATH" mvn test`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 3: Start the app and verify frontend loads**

Run: `JAVA_HOME="/c/Program Files/Java/jdk-21" PATH="/c/Program Files/Java/jdk-21/bin:$PATH" mvn spring-boot:run -pl samples/showcase/sample-showcase -am`
Open: `http://localhost:8088/index.html`
Expected: Sidebar with 7 modules, glassmorphism UI renders correctly

- [ ] **Step 4: Commit any fixes**

If any issues were found and fixed during testing, commit them.
