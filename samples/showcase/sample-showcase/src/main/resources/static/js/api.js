// ============================================================
// API 接入文档模块 — 前后端示例
// ============================================================

const API_CATEGORIES = [
    {
        id: 'ws', name: 'WebSocket', icon: 'plug', color: '#007AFF',
        apis: [
            {
                method: 'WS', path: '/ws?userId={userId}',
                summary: '建立 WebSocket 连接',
                params: [{ name: 'userId', in: 'query', type: 'string', required: true, desc: '用户 ID' }],
                body: null,
                curl: `# WebSocket 连接 (wscat)
wscat -c "ws://localhost:8088/ws?userId=alice"

# 连接后发送心跳
{"type":"heartbeat","timestamp":1715000000000}

# 发送单播消息
{"type":"chat.message","to":"bob","payload":{"text":"Hi Bob!"}}

# 发送房间消息
{"type":"room.message","roomId":"general","payload":{"text":"Hello room!"}}`,
                js: `// 建立 WebSocket 连接
const ws = new WebSocket('ws://localhost:8088/ws?userId=alice');

ws.onopen = () => {
    console.log('Connected');

    // 发送心跳
    ws.send(JSON.stringify({
        type: 'heartbeat',
        timestamp: Date.now()
    }));

    // 发送单播消息
    ws.send(JSON.stringify({
        type: 'chat.message',
        to: 'bob',
        payload: { text: 'Hi Bob!' }
    }));

    // 加入房间并发送消息
    ws.send(JSON.stringify({
        type: 'room.join',
        roomId: 'general'
    }));
};

ws.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    console.log('Received:', msg.type, msg.payload);
};

ws.onclose = () => console.log('Disconnected');
ws.onerror = (err) => console.error('Error:', err);`,
                java: `// Spring Boot 自动配置，无需额外代码
// WebSocket 端点已通过 zhi-pm 框架注册

// 服务端推送消息到指定用户
@Autowired MessageSender sender;

WsMessage<?> msg = WsMessage.of("notification",
    Map.of("title", "Order updated", "body", "Your order is shipped"));
sender.sendToUser("alice", msg).subscribe();

// 推送到房间
sender.sendToRoom("general", msg).subscribe();

// 广播到所有连接
sender.broadcast(msg).subscribe();`
            },
            {
                method: 'MSG', path: 'WsMessage 协议格式',
                summary: '消息协议结构',
                params: [],
                body: null,
                curl: `# 消息协议 JSON 结构
{
  "id": "uuid-string",        // 消息唯一 ID（自动生成）
  "type": "message.type",     // 消息类型标识
  "traceId": "trace-xxx",     // 链路追踪 ID（可选）
  "tenantId": "default",      // 租户 ID（可选）
  "from": "alice",            // 发送者用户 ID
  "to": "bob",                // 接收者用户 ID（单播）
  "roomId": "general",        // 房间 ID（房间消息）
  "timestamp": 1715000000000, // 时间戳（毫秒）
  "payload": {}               // 业务载荷（任意 JSON）
}`,
                js: `// WsMessage TypeScript 接口定义
interface WsMessage<T> {
    id: string;           // 消息唯一 ID
    type: string;         // 消息类型
    traceId?: string;     // 链路追踪 ID
    tenantId?: string;    // 租户 ID
    from?: string;        // 发送者
    to?: string;          // 接收者（单播）
    roomId?: string;      // 房间 ID
    timestamp: number;    // 毫秒时间戳
    payload?: T;          // 业务载荷
}

// 接收消息时的类型判断
ws.onmessage = (event) => {
    const msg: WsMessage<any> = JSON.parse(event.data);
    switch (msg.type) {
        case 'chat.message':
            handleChat(msg.payload);
            break;
        case 'order.status.changed':
            handleOrderUpdate(msg.payload);
            break;
        case 'showcase.broadcast':
            handleBroadcast(msg.payload);
            break;
    }
};`,
                java: `// WsMessage 泛型类
public class WsMessage<T> {
    private String id;         // UUID 自动生成
    private String type;       // 消息类型
    private String traceId;    // 链路追踪
    private String tenantId;   // 租户
    private String from;       // 发送者
    private String to;         // 接收者
    private String roomId;     // 房间
    private long timestamp;    // 毫秒时间戳
    private T payload;         // 业务载荷

    // 快速创建消息
    public static <T> WsMessage<T> of(String type, T payload) { ... }
}

// 在 Handler 中接收消息
@Component
public class ChatMessageHandler implements WsMessageHandler<ChatPayload> {
    @Override
    public String type() { return "chat.message"; }

    @Override
    public Mono<Void> handle(WsSession session, WsMessage<ChatPayload> msg) {
        String targetUser = msg.getTo();
        return sender.sendToUser(targetUser, msg);
    }
}`
            }
        ]
    },
    {
        id: 'push', name: '消息推送', icon: 'send', color: '#34C759',
        apis: [
            {
                method: 'POST', path: '/api/push/users/{userId}',
                summary: '向指定用户推送消息',
                params: [{ name: 'userId', in: 'path', type: 'string', required: true, desc: '目标用户 ID' }],
                body: { type: 'custom.event', payload: { message: 'Hello!' } },
                curl: `curl -X POST http://localhost:8088/api/push/users/alice \\
  -H "Content-Type: application/json" \\
  -d '{"type":"custom.event","payload":{"message":"Hello!"}}'`,
                js: `// 向指定用户推送消息
const response = await fetch('/api/push/users/alice', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        type: 'custom.event',
        payload: { message: 'Hello!' }
    })
});
const result = await response.json();
// => { "sent": 1, "userId": "alice" }`,
                java: `@Autowired MessageSender sender;

// 推送自定义事件到指定用户
WsMessage<?> msg = WsMessage.of("custom.event",
    Map.of("message", "Hello!"));
sender.sendToUser("alice", msg).subscribe();

// 带返回值的推送
Mono<SendResult> result = sender.sendToUser("alice", msg);`
            },
            {
                method: 'POST', path: '/api/push/rooms/{roomId}',
                summary: '向指定房间推送消息',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '目标房间 ID' }],
                body: { type: 'room.event', payload: { message: 'Room broadcast' } },
                curl: `curl -X POST http://localhost:8088/api/push/rooms/general \\
  -H "Content-Type: application/json" \\
  -d '{"type":"room.event","payload":{"message":"Room broadcast"}}'`,
                js: `// 向房间内所有用户推送消息
const response = await fetch('/api/push/rooms/general', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        type: 'room.event',
        payload: { message: 'Room broadcast' }
    })
});
const result = await response.json();
// => { "sent": 3, "roomId": "general" }`,
                java: `// 推送消息到房间
WsMessage<?> msg = WsMessage.of("room.event",
    Map.of("message", "Room broadcast"));
sender.sendToRoom("general", msg).subscribe();`
            },
            {
                method: 'POST', path: '/api/push/broadcast',
                summary: '向所有连接广播消息',
                params: [],
                body: { type: 'broadcast', payload: { message: 'Hello everyone!' } },
                curl: `curl -X POST http://localhost:8088/api/push/broadcast \\
  -H "Content-Type: application/json" \\
  -d '{"type":"broadcast","payload":{"message":"Hello everyone!"}}'`,
                js: `// 广播消息到所有已连接的客户端
const response = await fetch('/api/push/broadcast', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        type: 'broadcast',
        payload: { message: 'Hello everyone!' }
    })
});
const result = await response.json();
// => { "sent": 5 }`,
                java: `// 广播到所有连接
WsMessage<?> msg = WsMessage.of("broadcast",
    Map.of("message", "Hello everyone!"));
sender.broadcast(msg).subscribe();`
            }
        ]
    },
    {
        id: 'presence', name: '在线状态', icon: 'activity', color: '#5856D6',
        apis: [
            {
                method: 'GET', path: '/api/presence/connections/count',
                summary: '获取当前连接数',
                params: [],
                curl: `curl http://localhost:8088/api/presence/connections/count`,
                js: `const res = await fetch('/api/presence/connections/count');
const { count } = await res.json();
console.log('Active connections:', count);
// => { "count": 5 }`,
                java: `@Autowired ConnectionRegistry registry;

Mono<Long> count = registry.countConnections();
count.subscribe(c -> log.info("Active connections: {}", c));`
            },
            {
                method: 'GET', path: '/api/presence/users/{userId}/online',
                summary: '检查用户是否在线',
                params: [{ name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }],
                curl: `curl http://localhost:8088/api/presence/users/alice/online`,
                js: `const res = await fetch('/api/presence/users/alice/online');
const { online } = await res.json();
console.log('Alice is online:', online);
// => { "online": true }`,
                java: `Mono<Boolean> online = registry.isUserOnline("online");
online.subscribe(isOnline ->
    log.info("Alice online: {}", isOnline));`
            },
            {
                method: 'GET', path: '/api/presence/rooms/{roomId}/count',
                summary: '获取房间在线人数',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' }],
                curl: `curl http://localhost:8088/api/presence/rooms/general/count`,
                js: `const res = await fetch('/api/presence/rooms/general/count');
const { count } = await res.json();
console.log('Room members:', count);
// => { "count": 3 }`,
                java: `Mono<Long> count = registry.roomMemberCount("general");
count.subscribe(c -> log.info("Room members: {}", c));`
            }
        ]
    },
    {
        id: 'danmaku', name: '弹幕', icon: 'radio', color: '#FF3B30',
        apis: [
            {
                method: 'POST', path: '/api/danmaku/rooms/{roomId}',
                summary: '发送弹幕消息',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' }],
                body: { userId: 'alice', content: 'Hello danmaku!' },
                curl: `curl -X POST http://localhost:8088/api/danmaku/rooms/live-001 \\
  -H "Content-Type: application/json" \\
  -d '{"userId":"alice","content":"Hello danmaku!"}'`,
                js: `// 通过 REST API 发送弹幕
const res = await fetch('/api/danmaku/rooms/live-001', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        userId: 'alice',
        content: 'Hello danmaku!'
    })
});
const result = await res.json();
// => { "success": true, "roomId": "live-001" }

// 也可以通过 WebSocket 发送弹幕
ws.send(JSON.stringify({
    type: 'danmaku.send',
    roomId: 'live-001',
    payload: { content: 'Hello!' }
}));`,
                java: `@Autowired DanmakuService danmakuService;

// 发送弹幕
DanmakuMessage msg = new DanmakuMessage();
msg.setUserId("alice");
msg.setContent("Hello danmaku!");
danmakuService.send("live-001", msg).subscribe();`
            },
            {
                method: 'POST', path: '/api/danmaku/rooms/{roomId}/mute/{userId}',
                summary: '禁言用户',
                params: [
                    { name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' },
                    { name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }
                ],
                curl: `curl -X POST http://localhost:8088/api/danmaku/rooms/live-001/mute/bob`,
                js: `// 禁言指定用户
const res = await fetch('/api/danmaku/rooms/live-001/mute/bob', {
    method: 'POST'
});
const result = await res.json();
// => { "success": true, "userId": "bob", "muted": true }`,
                java: `danmakuService.muteUser("live-001", "bob").subscribe();`
            },
            {
                method: 'POST', path: '/api/danmaku/rooms/{roomId}/unmute/{userId}',
                summary: '解除禁言',
                params: [
                    { name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' },
                    { name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }
                ],
                curl: `curl -X POST http://localhost:8088/api/danmaku/rooms/live-001/unmute/bob`,
                js: `// 解除用户禁言
const res = await fetch('/api/danmaku/rooms/live-001/unmute/bob', {
    method: 'POST'
});
const result = await res.json();
// => { "success": true, "userId": "bob", "muted": false }`,
                java: `danmakuService.unmuteUser("live-001", "bob").subscribe();`
            },
            {
                method: 'GET', path: '/api/danmaku/rooms/{roomId}/muted',
                summary: '获取已禁言用户列表',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' }],
                curl: `curl http://localhost:8088/api/danmaku/rooms/live-001/muted`,
                js: `// 获取禁言列表
const res = await fetch('/api/danmaku/rooms/live-001/muted');
const { mutedUsers } = await res.json();
console.log('Muted users:', mutedUsers);
// => { "mutedUsers": ["bob"] }`,
                java: `Set<String> muted = danmakuService.getMutedUsers("live-001");`
            }
        ]
    },
    {
        id: 'chat', name: '聊天', icon: 'message-square', color: '#C780FF',
        apis: [
            {
                method: 'GET', path: '/api/chat/conversations/{conversationId}/history',
                summary: '获取聊天历史记录',
                params: [
                    { name: 'conversationId', in: 'path', type: 'string', required: true, desc: '会话 ID' },
                    { name: 'limit', in: 'query', type: 'int', required: false, desc: '返回条数，默认 50' }
                ],
                curl: `curl "http://localhost:8088/api/chat/conversations/conv-alice-bob/history?limit=20"`,
                js: `// 获取聊天历史
const res = await fetch(
    '/api/chat/conversations/conv-alice-bob/history?limit=20'
);
const { messages } = await res.json();
messages.forEach(msg => {
    console.log(\`\${msg.from}: \${msg.content}\`);
});
// => { "messages": [{ "from":"alice","content":"Hi","timestamp":... }] }`,
                java: `@Autowired ChatService chatService;

// 获取历史消息
Flux<ChatMessage> history = chatService
    .getHistory("conv-alice-bob", 20);`
            },
            {
                method: 'GET', path: '/api/chat/conversations/{conversationId}/unread/{userId}',
                summary: '获取未读消息数',
                params: [
                    { name: 'conversationId', in: 'path', type: 'string', required: true, desc: '会话 ID' },
                    { name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }
                ],
                curl: `curl http://localhost:8088/api/chat/conversations/conv-alice-bob/unread/alice`,
                js: `// 获取未读消息数
const res = await fetch(
    '/api/chat/conversations/conv-alice-bob/unread/alice'
);
const { count } = await res.json();
console.log('Unread messages:', count);
// => { "count": 3 }`,
                java: `Mono<Long> unread = chatService
    .getUnreadCount("conv-alice-bob", "alice");`
            }
        ]
    },
    {
        id: 'orders', name: '订单', icon: 'shopping-cart', color: '#FF9500',
        apis: [
            {
                method: 'POST', path: '/api/orders',
                summary: '创建订单',
                params: [],
                body: { userId: 'alice', product: 'MacBook Pro', amount: 14999.00 },
                curl: `curl -X POST http://localhost:8088/api/orders \\
  -H "Content-Type: application/json" \\
  -d '{"userId":"alice","product":"MacBook Pro","amount":14999.00}'`,
                js: `// 创建订单
const res = await fetch('/api/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        userId: 'alice',
        product: 'MacBook Pro',
        amount: 14999.00
    })
});
const order = await res.json();
console.log('Order created:', order.orderId);
// => { "orderId":"A1B2C3D4","userId":"alice","status":"CREATED",... }

// WebSocket 会实时推送状态变更
ws.onmessage = (e) => {
    const msg = JSON.parse(e.data);
    if (msg.type === 'order.status.changed') {
        console.log('Order status:', msg.payload.status);
    }
};`,
                java: `// 创建订单并通过 WebSocket 推送状态
@PostMapping("/api/orders")
public Mono<Map<String, Object>> createOrder(
        @RequestBody CreateOrderRequest request) {
    String orderId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    OrderInfo order = new OrderInfo(orderId, request.userId(),
        request.product(), request.amount(), "CREATED", Instant.now());
    orders.put(orderId, order);

    // 推送订单创建事件给用户
    WsMessage<?> msg = WsMessage.of("order.created", order);
    sender.sendToUser(request.userId(), msg).subscribe();

    return Mono.just(order.toMap());
}`
            },
            {
                method: 'POST', path: '/api/orders/{orderId}/status',
                summary: '更新订单状态',
                params: [{ name: 'orderId', in: 'path', type: 'string', required: true, desc: '订单 ID' }],
                body: { status: 'PAID' },
                curl: `curl -X POST http://localhost:8088/api/orders/A1B2C3D4/status \\
  -H "Content-Type: application/json" \\
  -d '{"status":"PAID"}'`,
                js: `// 更新订单状态
const res = await fetch('/api/orders/A1B2C3D4/status', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: 'PAID' })
});
const order = await res.json();
// => { "orderId":"A1B2C3D4","status":"PAID","previousStatus":"CREATED" }

// 客户端通过 WebSocket 自动收到推送:
// { type: "order.status.changed",
//   payload: { orderId, status, previousStatus, updatedAt } }`,
                java: `// 更新订单状态并推送通知
OrderStatus[] flow = {
    OrderStatus.CREATED, OrderStatus.PAID,
    OrderStatus.SHIPPING, OrderStatus.DELIVERED,
    OrderStatus.COMPLETED
};

orders.computeIfPresent(orderId, (id, order) -> {
    order.setStatus(newStatus);
    order.setUpdatedAt(Instant.now());
    // 推送状态变更事件
    WsMessage<?> msg = WsMessage.of("order.status.changed",
        Map.of("orderId", id, "status", newStatus,
               "previousStatus", oldStatus));
    sender.sendToUser(order.getUserId(), msg).subscribe();
    return order;
});`
            },
            {
                method: 'GET', path: '/api/orders/{orderId}',
                summary: '获取订单详情',
                params: [{ name: 'orderId', in: 'path', type: 'string', required: true, desc: '订单 ID' }],
                curl: `curl http://localhost:8088/api/orders/A1B2C3D4`,
                js: `const res = await fetch('/api/orders/A1B2C3D4');
const order = await res.json();
console.log('Order:', order);
// => { "orderId":"A1B2C3D4","userId":"alice",
//      "product":"MacBook Pro","amount":14999.00,
//      "status":"PAID","createdAt":"...","updatedAt":"..." }`,
                java: `Mono<OrderInfo> order = Mono.justOrEmpty(orders.get(orderId));`
            },
            {
                method: 'GET', path: '/api/orders/user/{userId}',
                summary: '获取用户订单列表',
                params: [{ name: 'userId', in: 'path', type: 'string', required: true, desc: '用户 ID' }],
                curl: `curl http://localhost:8088/api/orders/user/alice`,
                js: `const res = await fetch('/api/orders/user/alice');
const { orders } = await res.json();
orders.forEach(o => {
    console.log(\`\${o.orderId}: \${o.product} - \${o.status}\`);
});
// => { "orders": [{ "orderId":"A1B2C3D4","status":"PAID",... }] }`,
                java: `List<OrderInfo> userOrders = orders.values().stream()
    .filter(o -> o.getUserId().equals(userId))
    .toList();`
            }
        ]
    },
    {
        id: 'admin', name: '管理', icon: 'shield', color: '#86868b',
        apis: [
            {
                method: 'GET', path: '/admin/api/stats',
                summary: '获取网关统计数据',
                params: [],
                curl: `curl http://localhost:8088/admin/api/stats`,
                js: `const res = await fetch('/admin/api/stats');
const stats = await res.json();
console.log('Connections:', stats.activeConnections);
console.log('Users:', stats.onlineUsers);
console.log('Rooms:', stats.activeRooms);
console.log('Success rate:', stats.pushSuccessRate);
// => { "activeConnections":5,"onlineUsers":3,
//      "activeRooms":2,"pushSuccessRate":0.98 }`,
                java: `@Autowired AdminService adminService;

Mono<Map<String, Object>> stats = adminService.getStats();`
            },
            {
                method: 'GET', path: '/admin/api/connections',
                summary: '列出所有活跃连接',
                params: [],
                curl: `curl http://localhost:8088/admin/api/connections`,
                js: `const res = await fetch('/admin/api/connections');
const connections = await res.json();
connections.forEach(c => {
    console.log(\`\${c.userId} - \${c.sessionId}\`);
});
// => [{ "userId":"alice","sessionId":"abc123",
//      "connectedAt":"...","lastHeartbeatAt":"..." }]`,
                java: `Flux<Map<String, Object>> conns = adminService.listConnections();`
            },
            {
                method: 'DELETE', path: '/admin/api/connections/{sessionId}',
                summary: '踢出指定连接',
                params: [{ name: 'sessionId', in: 'path', type: 'string', required: true, desc: '会话 ID' }],
                curl: `curl -X DELETE http://localhost:8088/admin/api/connections/abc123`,
                js: `const res = await fetch('/admin/api/connections/abc123', {
    method: 'DELETE'
});
const result = await res.json();
// => { "kicked": true, "sessionId": "abc123" }`,
                java: `Mono<Boolean> kicked = adminService.kickConnection("abc123");`
            },
            {
                method: 'GET', path: '/admin/api/rooms',
                summary: '列出所有活跃房间',
                params: [],
                curl: `curl http://localhost:8088/admin/api/rooms`,
                js: `const res = await fetch('/admin/api/rooms');
const rooms = await res.json();
rooms.forEach(r => {
    console.log(\`\${r.roomId}: \${r.memberCount} members\`);
});
// => [{ "roomId":"general","memberCount":3 }]`,
                java: `Flux<Map<String, Object>> rooms = adminService.listRooms();`
            },
            {
                method: 'GET', path: '/admin/api/rooms/{roomId}/members',
                summary: '获取房间成员列表',
                params: [{ name: 'roomId', in: 'path', type: 'string', required: true, desc: '房间 ID' }],
                curl: `curl http://localhost:8088/admin/api/rooms/general/members`,
                js: `const res = await fetch('/admin/api/rooms/general/members');
const members = await res.json();
members.forEach(m => {
    console.log(\`\${m.userId} joined at \${m.joinedAt}\`);
});
// => [{ "userId":"alice","sessionId":"abc123","joinedAt":"..." }]`,
                java: `Flux<Map<String, Object>> members =
    adminService.getRoomMembers("general");`
            },
            {
                method: 'POST', path: '/admin/api/broadcast',
                summary: '广播消息到所有客户端',
                params: [],
                body: { type: 'admin.broadcast', payload: { message: 'System notice' } },
                curl: `curl -X POST http://localhost:8088/admin/api/broadcast \\
  -H "Content-Type: application/json" \\
  -d '{"type":"admin.broadcast","payload":{"message":"System notice"}}'`,
                js: `const res = await fetch('/admin/api/broadcast', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        type: 'admin.broadcast',
        payload: { message: 'System notice' }
    })
});
const result = await res.json();
// => { "sent": 5 }`,
                java: `adminService.broadcastMessage("admin.broadcast",
    Map.of("message", "System notice")).subscribe();`
            }
        ]
    }
];

let apiActiveTab = 'ws';
let apiExpandedCards = {};
let apiExampleTabs = {}; // track which example tab (curl/js/java) is active per card

function render_api() {
    const panel = document.getElementById('panel-api');
    panel.innerHTML = `
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px">
            <div style="width:40px;height:40px;border-radius:10px;background:rgba(0,122,255,0.1);display:flex;align-items:center;justify-content:center">
                <i data-lucide="code-2" style="width:20px;height:20px;color:var(--accent)"></i>
            </div>
            <div>
                <div style="font-weight:700;font-size:18px">API 接入文档</div>
                <div style="font-size:12px;color:var(--text-secondary)">WebSocket 网关接口一览 — cURL / JavaScript / Java 三端示例，在线调试</div>
            </div>
        </div>
        <div id="api-tabs" style="display:flex;gap:6px;margin-bottom:16px;flex-wrap:wrap"></div>
        <div id="api-content"></div>
    `;
    lucide.createIcons();
    apiExpandedCards = {};
    apiExampleTabs = {};
    renderApiTabs();
    renderApiContent();
}

function renderApiTabs() {
    const container = document.getElementById('api-tabs');
    container.innerHTML = API_CATEGORIES.map(cat => `
        <button class="btn btn-sm ${apiActiveTab === cat.id ? 'btn-primary' : 'btn-ghost'}"
                onclick="apiSwitchTab('${cat.id}')" style="gap:6px">
            <i data-lucide="${cat.icon}" style="width:14px;height:14px"></i>
            ${cat.name}
            <span style="background:${apiActiveTab === cat.id ? 'rgba(255,255,255,0.2)' : 'var(--border)'};padding:1px 6px;border-radius:4px;font-size:10px">${cat.apis.length}</span>
        </button>
    `).join('');
    lucide.createIcons();
}

function apiSwitchTab(id) {
    apiActiveTab = id;
    apiExpandedCards = {};
    apiExampleTabs = {};
    renderApiTabs();
    renderApiContent();
}

function renderApiContent() {
    const cat = API_CATEGORIES.find(c => c.id === apiActiveTab);
    if (!cat) return;
    const container = document.getElementById('api-content');
    container.innerHTML = `
        <div style="background:var(--card-bg);border-radius:12px;border:0.5px solid var(--border);box-shadow:var(--card-shadow);overflow:hidden">
            <div style="padding:16px 20px;border-bottom:0.5px solid var(--border);display:flex;align-items:center;gap:10px">
                <div style="width:28px;height:28px;border-radius:7px;background:${cat.color}15;display:flex;align-items:center;justify-content:center">
                    <i data-lucide="${cat.icon}" style="width:16px;height:16px;color:${cat.color}"></i>
                </div>
                <div style="font-weight:600;font-size:15px">${cat.name}</div>
                <span style="font-size:12px;color:var(--text-secondary)">${cat.apis.length} 个接口</span>
            </div>
            <div style="display:flex;flex-direction:column">
                ${cat.apis.map((api, i) => renderApiCard(api, i, cat)).join('')}
            </div>
        </div>
    `;
    lucide.createIcons();
}

function renderApiCard(api, index, cat) {
    const key = cat.id + '-' + index;
    const expanded = apiExpandedCards[key];
    const methodColors = { GET: '#34C759', POST: '#007AFF', PUT: '#FF9500', DELETE: '#FF3B30', PATCH: '#C780FF', WS: '#5856D6', MSG: '#FF9500' };
    const methodColor = methodColors[api.method] || '#86868b';

    return `
        <div style="border-bottom:0.5px solid var(--border);${index === cat.apis.length - 1 ? 'border-bottom:none' : ''}">
            <div style="padding:14px 20px;display:flex;align-items:center;gap:12px;cursor:pointer;transition:background 0.1s"
                 onclick="apiToggleCard('${key}')" onmouseenter="this.style.background='var(--sidebar-hover)'" onmouseleave="this.style.background='transparent'">
                <span style="background:${methodColor}15;color:${methodColor};font-size:11px;font-weight:700;padding:3px 8px;border-radius:5px;min-width:52px;text-align:center;font-family:'SF Mono','Fira Code',monospace">${api.method}</span>
                <span style="font-family:'SF Mono','Fira Code',monospace;font-size:13px;color:var(--text-primary);flex:1">${api.path}</span>
                <span style="font-size:12px;color:var(--text-secondary)">${api.summary}</span>
                <i data-lucide="${expanded ? 'chevron-up' : 'chevron-down'}" style="width:16px;height:16px;color:var(--text-secondary);flex-shrink:0"></i>
            </div>
            ${expanded ? renderApiDetail(api, key) : ''}
        </div>
    `;
}

function renderApiDetail(api, key) {
    let html = '<div style="padding:0 20px 16px;animation:fadeIn 0.2s ease">';

    // Parameters
    if (api.params && api.params.length > 0) {
        html += `
            <div style="margin-bottom:14px">
                <div style="font-size:12px;font-weight:600;color:var(--text-secondary);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.4px">Parameters</div>
                <div style="background:var(--input-bg);border-radius:8px;border:0.5px solid var(--border);overflow:hidden">
                    <table style="width:100%;border-collapse:collapse;font-size:12px">
                        <thead>
                            <tr style="border-bottom:0.5px solid var(--border)">
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">Name</th>
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">In</th>
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">Type</th>
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">Required</th>
                                <th style="padding:8px 12px;text-align:left;font-weight:600;color:var(--text-secondary)">Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${api.params.map(p => `
                                <tr style="border-bottom:0.5px solid var(--border)">
                                    <td style="padding:8px 12px;font-family:'SF Mono','Fira Code',monospace;color:var(--accent)">${p.name}</td>
                                    <td style="padding:8px 12px;color:var(--text-secondary)">${p.in}</td>
                                    <td style="padding:8px 12px;font-family:'SF Mono','Fira Code',monospace">${p.type}</td>
                                    <td style="padding:8px 12px">${p.required ? '<span style="color:var(--error);font-weight:600">Yes</span>' : '<span style="color:var(--text-secondary)">No</span>'}</td>
                                    <td style="padding:8px 12px;color:var(--text-secondary)">${p.desc}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }

    // Request body
    if (api.body) {
        html += `
            <div style="margin-bottom:14px">
                <div style="font-size:12px;font-weight:600;color:var(--text-secondary);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.4px">Request Body</div>
                <pre style="background:var(--input-bg);border-radius:8px;border:0.5px solid var(--border);padding:12px 14px;font-family:'SF Mono','Fira Code','Menlo',monospace;font-size:12px;line-height:1.5;overflow-x:auto;margin:0;color:var(--text-primary)">${JSON.stringify(api.body, null, 2)}</pre>
            </div>
        `;
    }

    // Example tabs: curl / JavaScript / Java
    const activeExampleTab = apiExampleTabs[key] || 'curl';
    const tabs = [
        { id: 'curl', label: 'cURL', icon: 'terminal', color: '#86868b' },
        { id: 'js', label: 'JavaScript', icon: 'braces', color: '#FF9500' },
        { id: 'java', label: 'Java', icon: 'coffee', color: '#34C759' }
    ];
    const examples = { curl: api.curl, js: api.js, java: api.java };

    html += `
        <div style="margin-bottom:14px">
            <div style="display:flex;align-items:center;gap:4px;margin-bottom:8px">
                <div style="font-size:12px;font-weight:600;color:var(--text-secondary);text-transform:uppercase;letter-spacing:0.4px;margin-right:8px">Examples</div>
                ${tabs.map(t => `
                    <button class="btn btn-sm ${activeExampleTab === t.id ? 'btn-primary' : 'btn-ghost'}"
                            onclick="event.stopPropagation();apiSwitchExampleTab('${key}','${t.id}')"
                            style="padding:3px 10px;font-size:11px;gap:4px;border-radius:6px">
                        <i data-lucide="${t.icon}" style="width:12px;height:12px"></i> ${t.label}
                    </button>
                `).join('')}
            </div>
            <div style="position:relative">
                <pre style="background:#1d1d1f;border-radius:8px;padding:14px;font-family:'SF Mono','Fira Code','Menlo',monospace;font-size:12px;line-height:1.6;overflow-x:auto;margin:0;color:#f5f5f7;white-space:pre-wrap;word-break:break-all">${escapeHtml(examples[activeExampleTab] || '# No example available')}</pre>
                <button class="btn btn-sm" onclick="event.stopPropagation();apiCopyExample(this)" data-code="${escapeAttr(examples[activeExampleTab] || '')}"
                        style="position:absolute;top:8px;right:8px;background:rgba(255,255,255,0.1);color:#f5f5f7;border:0.5px solid rgba(255,255,255,0.2);font-size:11px;padding:3px 8px;border-radius:5px">
                    <i data-lucide="copy" style="width:12px;height:12px"></i> Copy
                </button>
            </div>
        </div>
    `;

    // Try it
    const tryId = 'try-' + key;
    html += `
        <div style="display:flex;gap:8px;align-items:center">
            <button class="btn btn-primary btn-sm" onclick="event.stopPropagation();apiTryIt('${key}')" id="btn-${tryId}">
                <i data-lucide="play" style="width:12px;height:12px"></i> Try it
            </button>
            <span style="font-size:11px;color:var(--text-secondary)">直接调用接口查看返回结果</span>
        </div>
        <div id="${tryId}" style="margin-top:10px;display:none"></div>
    `;

    html += '</div>';
    return html;
}

function apiSwitchExampleTab(key, tabId) {
    apiExampleTabs[key] = tabId;
    renderApiContent();
}

function apiToggleCard(key) {
    apiExpandedCards[key] = !apiExpandedCards[key];
    renderApiContent();
}

function apiCopyExample(btn) {
    const code = btn.getAttribute('data-code');
    navigator.clipboard.writeText(code).then(() => {
        btn.innerHTML = '<i data-lucide="check" style="width:12px;height:12px"></i> Copied';
        lucide.createIcons();
        setTimeout(() => {
            btn.innerHTML = '<i data-lucide="copy" style="width:12px;height:12px"></i> Copy';
            lucide.createIcons();
        }, 2000);
    });
}

async function apiTryIt(key) {
    const [catId, idxStr] = key.split('-');
    const cat = API_CATEGORIES.find(c => c.id === catId);
    const api = cat.apis[parseInt(idxStr)];
    const resultDiv = document.getElementById('try-' + key);

    if (!resultDiv) return;
    resultDiv.style.display = 'block';
    resultDiv.innerHTML = `
        <div style="background:var(--input-bg);border-radius:8px;border:0.5px solid var(--border);padding:12px;font-size:12px;color:var(--text-secondary)">
            <i data-lucide="loader-2" style="width:14px;height:14px;animation:spin 1s linear infinite;vertical-align:middle"></i> 请求中...
        </div>
    `;
    lucide.createIcons();

    // Build URL with path params replaced
    let url = api.path;
    if (api.params) {
        api.params.filter(p => p.in === 'path').forEach(p => {
            const val = prompt(`Enter ${p.name} (${p.desc}):`, p.name === 'userId' ? 'alice' : p.name === 'roomId' ? 'general' : p.name === 'orderId' ? 'A1B2C3D4' : p.name === 'conversationId' ? 'conv-alice-bob' : p.name === 'sessionId' ? 'abc123' : '');
            if (val) url = url.replace(`{${p.name}}`, val);
        });
        const queryParams = api.params.filter(p => p.in === 'query');
        if (queryParams.length > 0) {
            const parts = [];
            queryParams.forEach(p => {
                const val = prompt(`Enter ${p.name} (${p.desc}, optional - leave empty to skip):`, '');
                if (val) parts.push(`${p.name}=${encodeURIComponent(val)}`);
            });
            if (parts.length > 0) url += '?' + parts.join('&');
        }
    }

    const options = { method: api.method, headers: { 'Content-Type': 'application/json' } };
    if (api.body && (api.method === 'POST' || api.method === 'PUT' || api.method === 'PATCH')) {
        options.body = JSON.stringify(api.body);
    }

    try {
        const resp = await fetch(url, options);
        const status = resp.status;
        let body;
        try { body = await resp.json(); } catch { body = await resp.text(); }
        const statusColor = status >= 200 && status < 300 ? 'var(--success)' : 'var(--error)';
        resultDiv.innerHTML = `
            <div style="background:var(--input-bg);border-radius:8px;border:0.5px solid var(--border);overflow:hidden">
                <div style="padding:8px 12px;border-bottom:0.5px solid var(--border);display:flex;align-items:center;gap:8px">
                    <span style="background:${statusColor}15;color:${statusColor};font-size:11px;font-weight:700;padding:2px 8px;border-radius:4px">${status}</span>
                    <span style="font-size:11px;color:var(--text-secondary);font-family:'SF Mono','Fira Code',monospace">${api.method} ${url}</span>
                </div>
                <pre style="padding:12px;font-family:'SF Mono','Fira Code','Menlo',monospace;font-size:12px;line-height:1.5;overflow-x:auto;margin:0;color:var(--text-primary)">${typeof body === 'string' ? escapeHtml(body) : JSON.stringify(body, null, 2)}</pre>
            </div>
        `;
    } catch (err) {
        resultDiv.innerHTML = `
            <div style="background:rgba(255,59,48,0.06);border-radius:8px;border:0.5px solid rgba(255,59,48,0.2);padding:12px;font-size:12px;color:var(--error)">
                Error: ${escapeHtml(err.message)}
            </div>
        `;
    }
}

function escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function escapeAttr(str) {
    return String(str).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/\n/g, '&#10;');
}

const style = document.createElement('style');
style.textContent = '@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}';
document.head.appendChild(style);
