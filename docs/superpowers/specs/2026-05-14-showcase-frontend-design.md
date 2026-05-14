# Showcase Frontend Design

## Overview

A unified single-page application that demonstrates all zhi-pm features through a macOS Cupertino glassmorphism UI. Built with vanilla JS + CSS, zero dependencies except CDN icon library.

## Architecture

### Deployment

- New sample: `samples/showcase/sample-showcase` on port 8088
- Frontend: `samples/showcase/sample-showcase/src/main/resources/static/index.html`
- Backend: Spring Boot app with aggregated REST endpoints from all feature modules
- The showcase backend depends on zhi-pm-starter + all feature modules (room, danmaku, chat, observability, admin-api)

### Frontend Structure

Single HTML file with embedded CSS and JS:

```
index.html
├── <style> — Cupertino glassmorphism CSS
├── <body>
│   ├── sidebar — macOS-style navigation
│   ├── topbar — connection status + theme toggle
│   └── content — module panels (one visible at a time)
└── <script>
    ├── WebSocket client (auto-reconnect)
    ├── Store (global state)
    ├── Router (hash-based)
    ├── Module renderers (one per feature)
    └── Utility functions
```

## UI Design — Cupertino Glassmorphism

### Color Palette

**Light theme:**
- Background: linear-gradient `#667eea → #764ba2` (blue-purple)
- Sidebar: `rgba(255,255,255,0.15)` + `backdrop-filter: blur(20px)`
- Cards: `rgba(255,255,255,0.7)` + `backdrop-filter: blur(10px)`
- Text: `#1d1d1f` (primary), `#86868b` (secondary)
- Accent: `#007AFF` (macOS blue)
- Success: `#34C759`, Warning: `#FF9500`, Error: `#FF3B30`

**Dark theme:**
- Sidebar: `rgba(0,0,0,0.3)` + `backdrop-filter: blur(20px)`
- Cards: `rgba(30,30,30,0.8)`
- Text: `#f5f5f7` (primary), `#86868b` (secondary)

### Typography

- Font: `-apple-system, BlinkMacSystemFont, 'SF Pro Text', 'Segoe UI', sans-serif`
- Headings: 600 weight, 20-24px
- Body: 400 weight, 14px
- Monospace (logs): `'SF Mono', 'Fira Code', monospace`

### Layout

```
┌──────────────────────────────────────────────────────┐
│ topbar: 48px, glass, project name + ws status + theme│
├────────┬─────────────────────────────────────────────┤
│sidebar │ content area                                │
│220px   │ padding: 24px                               │
│glass   │ max-width: 1200px, centered                 │
│        │                                             │
│nav     │ ┌─ module header ─────────────────────┐     │
│items   │ │ title + status badge + actions       │     │
│        │ └──────────────────────────────────────┘     │
│        │ ┌─ main content ──────────────────────┐     │
│        │ │ cards / forms / message streams      │     │
│        │ │                                       │     │
│        │ └──────────────────────────────────────┘     │
│        │ ┌─ log panel ─────────────────────────┐     │
│        │ │ real-time message log (collapsible)   │     │
│        │ └──────────────────────────────────────┘     │
└────────┴─────────────────────────────────────────────┘
```

### Animations

- Panel transitions: `opacity 0.3s ease, transform 0.3s ease`
- Card hover: `transform: translateY(-2px)`, shadow deepen
- Message appear: `fadeIn 0.3s ease`
- Sidebar item active: `background-color 0.2s ease`

### Icons

Lucide Icons via CDN: `https://unpkg.com/lucide@latest`

Used icons: Activity, Wifi, WifiOff, MessageSquare, Radio, ShoppingCart, Settings, BarChart3, Send, Users, Home, Moon, Sun, ChevronRight, Circle

## Feature Modules

### 1. Presence Module (`#presence`)

**Backend:** sample-presence (port 8081)
**REST endpoints:**
- `GET /api/presence/connections/count`
- `GET /api/presence/users/{userId}/online`
- `GET /api/presence/rooms/{roomId}/count`
- `POST /api/presence/users/{userId}/push`
- `POST /api/presence/broadcast`

**UI components:**
- User status cards (Alice, Bob, Charlie) — click to connect/disconnect
- Connection stats bar (total connections, online users)
- Push notification form (select user + message)
- Broadcast form
- Real-time message received log

**WebSocket messages:**
- Connect: `ws://localhost:8081/ws?access_token={user}-token`
- Receive: `presence.notification`, `presence.broadcast`

### 2. Push Module (`#push`)

**Backend:** sample-basic-echo (port 8080)
**REST endpoints:**
- `POST /api/push/users/{userId}`
- `POST /api/push/broadcast`
- `POST /api/push/rooms/{roomId}`

**UI components:**
- User selector (connect as a user)
- Push form: target type (user/broadcast/room) + target ID + message content
- Message received panel with timestamp
- Echo response display

**WebSocket messages:**
- Connect: `ws://localhost:8080/ws?access_token={user}-token`
- Send: any type → receives `echo` response
- Receive: `sample.user-push`, `sample.broadcast`, `sample.room-push`

### 3. Danmaku Module (`#danmaku`)

**Backend:** sample-live-danmaku (port 8083)
**REST endpoints:**
- `POST /api/danmaku/rooms/{roomId}?content=xxx&userId=xxx`
- `POST /api/danmaku/rooms/{roomId}/mute/{userId}`
- `POST /api/danmaku/rooms/{roomId}/unmute/{userId}`
- `GET /api/danmaku/rooms/{roomId}/muted`
- `GET /api/danmaku/rooms/{roomId}/count`

**UI components:**
- Simulated live video area (gradient background with room ID overlay)
- Danmaku stream — messages float right to left with CSS animation
- Input bar at bottom with send button
- Side panel: online count, rate limit indicator, muted users list
- Mute/unmute controls

**WebSocket messages:**
- Connect: `ws://localhost:8083/ws?access_token={user}-token`
- Send: `{ type: "danmaku.send", payload: { roomId, content, contentType } }`
- Receive: `{ type: "danmaku.message", payload: { content, userId, roomId } }`

### 4. Chat Module (`#chat`)

**Backend:** sample-chat-room (port 8084)
**REST endpoints:**
- `GET /api/chat/conversations/{id}/history?limit=N`
- `GET /api/chat/conversations/{id}/unread/{userId}`

**UI components:**
- Left panel: conversation list (single chats + group chat)
- Right panel: iMessage-style message bubbles
  - Sent messages: blue, right-aligned
  - Received messages: gray, left-aligned
  - Read receipts (double check marks)
- Unread badge on conversation items
- Input bar with send button
- User switcher (Alice / Bob / Charlie)

**WebSocket messages:**
- Connect: `ws://localhost:8084/ws?access_token={user}-token`
- Send: `{ type: "chat.send", payload: { conversationId, conversationType, content, contentType } }`
- Send ACK: `{ type: "chat.ack", payload: { messageId } }`
- Send Read: `{ type: "chat.read", payload: { conversationId } }`
- Receive: `{ type: "chat.message", payload: { messageId, conversationId, content, senderId } }`

### 5. Order Tracking Module (`#orders`)

**Backend:** sample-order-tracking (port 8086)
**REST endpoints:**
- `POST /api/orders` — create order
- `POST /api/orders/{orderId}/status` — update status
- `GET /api/orders/{orderId}` — get order
- `GET /api/orders/user/{userId}` — user's orders
- `GET /api/orders/stats` — stats

**UI components:**
- Create order form (product, quantity)
- Order cards list with status badges
- Status change timeline
- Real-time status update notifications (toast)
- Stats bar (total orders, connections)

**WebSocket messages:**
- Connect: `ws://localhost:8086/ws?access_token={user}-token`
- Receive: `order.created`, `order.status.changed`

### 6. Admin Module (`#admin`)

**Backend:** sample-admin-console (port 8085)
**REST endpoints:**
- `GET /admin/api/stats`
- `GET /admin/api/connections`
- `DELETE /admin/api/connections/{sessionId}`
- `GET /admin/api/rooms`
- `GET /admin/api/rooms/{roomId}/members`
- `POST /admin/api/broadcast`

**UI components:**
- Stats cards (connections, users, rooms, success rate)
- Connections table with kick action
- Rooms table with member count
- Broadcast form

### 7. Metrics Module (`#metrics`)

**Backend:** sample-admin-console (port 8085)
**Endpoints:**
- `GET /admin/api/stats`
- `GET /actuator/prometheus`

**UI components:**
- Real-time line charts (connections, messages, success rate)
- Auto-refresh every 2 seconds
- Canvas-based charts (no library, simple line drawing)

## Backend: sample-showcase

### Module Structure

```
samples/showcase/
├── pom.xml
└── sample-showcase/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/io/github/zhi/pm/sample/showcase/
        │   │   ├── SampleShowcaseApplication.java
        │   │   └── ShowcaseController.java
        │   └── resources/
        │       ├── application.yml
        │       └── static/
        │           └── index.html
        └── test/
            └── java/.../SampleShowcaseApplicationTest.java
```

### Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>io.github.zhi</groupId>
        <artifactId>zhi-pm-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.zhi</groupId>
        <artifactId>zhi-pm-room</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.zhi</groupId>
        <artifactId>zhi-pm-danmaku</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.zhi</groupId>
        <artifactId>zhi-pm-chat</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.zhi</groupId>
        <artifactId>zhi-pm-observability</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.zhi</groupId>
        <artifactId>zhi-pm-admin-api</artifactId>
    </dependency>
</dependencies>
```

### Configuration

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
```

### ShowcaseController

Aggregates endpoints from all sample controllers:
- `POST /api/push/users/{userId}` — push to user
- `POST /api/push/broadcast` — broadcast
- `POST /api/push/rooms/{roomId}` — push to room
- `GET /api/presence/connections/count` — connection count
- `GET /api/presence/users/{userId}/online` — check online
- `POST /api/danmaku/rooms/{roomId}` — send danmaku
- `GET /api/chat/conversations/{id}/history` — chat history
- `POST /api/orders` — create order
- `POST /api/orders/{orderId}/status` — update order status
- `GET /admin/api/stats` — admin stats (from admin-api module)
- `GET /admin/api/connections` — connections list
- `GET /admin/api/rooms` — rooms list

## WebSocket Client

### Connection Manager

```javascript
class WsClient {
    constructor(url, userId) {
        this.url = url;
        this.userId = userId;
        this.ws = null;
        this.handlers = new Map();
        this.reconnectDelay = 1000;
        this.maxReconnectDelay = 30000;
    }

    connect() {
        this.ws = new WebSocket(this.url);
        this.ws.onopen = () => { /* update UI, reset reconnect */ };
        this.ws.onmessage = (e) => { /* parse JSON, dispatch to handlers */ };
        this.ws.onclose = () => { /* schedule reconnect */ };
    }

    send(type, payload) {
        this.ws.send(JSON.stringify({ type, payload, from: this.userId, timestamp: new Date().toISOString() }));
    }

    on(type, handler) {
        this.handlers.set(type, handler);
    }

    disconnect() {
        this.ws.close();
    }
}
```

### Auto-Reconnect

- Initial delay: 1s
- Exponential backoff: 1s → 2s → 4s → 8s → ... → 30s max
- Reset on successful connection
- Visual indicator: green dot = connected, yellow = reconnecting, red = disconnected

## File Deliverables

1. `samples/showcase/pom.xml` — parent POM
2. `samples/showcase/sample-showcase/pom.xml` — module POM
3. `samples/showcase/sample-showcase/src/main/java/.../SampleShowcaseApplication.java`
4. `samples/showcase/sample-showcase/src/main/java/.../ShowcaseController.java`
5. `samples/showcase/sample-showcase/src/main/resources/application.yml`
6. `samples/showcase/sample-showcase/src/main/resources/static/index.html`
7. `samples/showcase/sample-showcase/src/test/java/.../SampleShowcaseApplicationTest.java`
8. `samples/pom.xml` — updated to include showcase module
9. `pom.xml` — updated if needed
