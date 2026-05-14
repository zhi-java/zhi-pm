package io.github.zhi.pm.sample.showcase;

import io.github.zhi.pm.core.chat.ChatService;
import io.github.zhi.pm.core.danmaku.DanmakuService;
import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.danmaku.DanmakuServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * 演示控制器，聚合所有功能模块的 REST 端点。
 *
 * <p>涵盖推送、在线状态、弹幕、聊天和订单追踪等场景，
 * 便于在不连接 WebSocket 客户端的情况下快速验证后端能力。</p>
 */
@RestController
@RequestMapping("/api")
@Tag(name = "演示接口", description = "Showcase 聚合所有功能模块的 REST 端点")
public class ShowcaseController {

    private final ConnectionRegistry registry;
    private final MessageSender sender;
    private final DanmakuServiceImpl danmakuService;
    private final ChatService chatService;
    /** 内存订单存储，仅用于演示，重启后数据丢失 */
    private final Map<String, OrderInfo> orders = new ConcurrentHashMap<>();

    /**
     * 构造演示控制器。
     *
     * @param registry       连接注册表
     * @param sender         消息发送器
     * @param danmakuService 弹幕服务
     * @param chatService    聊天服务
     */
    public ShowcaseController(ConnectionRegistry registry, MessageSender sender,
                              DanmakuService danmakuService, ChatService chatService) {
        this.registry = registry;
        this.sender = sender;
        this.danmakuService = (DanmakuServiceImpl) danmakuService;
        this.chatService = chatService;
    }

    // ---- 推送端点 ----

    /**
     * 向指定用户推送消息。
     *
     * @param userId  目标用户 ID
     * @param payload 消息负载，可选
     * @return 推送结果
     */
    @Operation(summary = "向指定用户推送消息")
    @PostMapping("/push/users/{userId}")
    public Mono<Map<String, Object>> pushToUser(
            @Parameter(description = "目标用户 ID") @PathVariable("userId") String userId,
            @RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = WsMessage.of("showcase.user-push", payload == null ? Collections.emptyMap() : payload);
        return sender.sendToUser(userId, message).map(sent -> Map.of("userId", userId, "sent", sent));
    }

    /**
     * 向所有连接广播消息。
     *
     * @param payload 消息负载，可选
     * @return 广播结果，包含实际送达数量
     */
    @Operation(summary = "向所有连接广播消息")
    @PostMapping("/push/broadcast")
    public Mono<Map<String, Object>> broadcast(@RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = WsMessage.of("showcase.broadcast", payload == null ? Collections.emptyMap() : payload);
        return sender.broadcast(message).map(count -> Map.of("sent", count));
    }

    /**
     * 向指定房间推送消息。
     *
     * @param roomId  目标房间 ID
     * @param payload 消息负载，可选
     * @return 推送结果
     */
    @Operation(summary = "向指定房间推送消息")
    @PostMapping("/push/rooms/{roomId}")
    public Mono<Map<String, Object>> pushToRoom(
            @Parameter(description = "目标房间 ID") @PathVariable("roomId") String roomId,
            @RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = WsMessage.of("showcase.room-push", payload == null ? Collections.emptyMap() : payload);
        return sender.sendToRoom(roomId, message).map(count -> Map.of("roomId", roomId, "sent", count));
    }

    // ---- 在线状态端点 ----

    /**
     * 获取当前连接数。
     *
     * @return 当前活跃连接总数
     */
    @Operation(summary = "获取当前连接数")
    @GetMapping("/presence/connections/count")
    public Mono<Map<String, Object>> connectionCount() {
        return registry.countConnections().map(count -> Map.of("count", count));
    }

    /**
     * 检查用户是否在线。
     *
     * @param userId 用户 ID
     * @return 在线状态
     */
    @Operation(summary = "检查用户是否在线")
    @GetMapping("/presence/users/{userId}/online")
    public Mono<Map<String, Object>> isOnline(
            @Parameter(description = "用户 ID") @PathVariable("userId") String userId) {
        return registry.isOnline(userId).map(online -> Map.of("userId", userId, "online", online));
    }

    /**
     * 获取房间在线人数。
     *
     * @param roomId 房间 ID
     * @return 房间内当前连接数
     */
    @Operation(summary = "获取房间在线人数")
    @GetMapping("/presence/rooms/{roomId}/count")
    public Mono<Map<String, Object>> roomCount(
            @Parameter(description = "房间 ID") @PathVariable("roomId") String roomId) {
        return registry.countRoomConnections(roomId).map(count -> Map.of("roomId", roomId, "count", count));
    }

    // ---- 弹幕端点 ----

    /**
     * 发送弹幕消息。
     *
     * @param roomId 房间 ID
     * @param content 弹幕内容
     * @param userId 发送者用户 ID，默认 system
     * @return 发送结果
     */
    @Operation(summary = "发送弹幕消息")
    @PostMapping("/danmaku/rooms/{roomId}")
    public Mono<Map<String, Object>> sendDanmaku(
            @Parameter(description = "房间 ID") @PathVariable("roomId") String roomId,
            @Parameter(description = "弹幕内容") @RequestParam("content") String content,
            @Parameter(description = "发送者用户 ID") @RequestParam(value = "userId", defaultValue = "system") String userId) {
        WsMessage<Map<String, Object>> message = new WsMessage<>(null, "danmaku.message", null, null,
                userId, null, roomId, Instant.now(), Map.of("content", content));
        return sender.sendToRoom(roomId, message).map(count -> Map.of("roomId", roomId, "sent", count));
    }

    /**
     * 禁言用户。
     *
     * @param roomId 房间 ID
     * @param userId 要禁言的用户 ID
     * @return 禁言结果
     */
    @Operation(summary = "禁言用户")
    @PostMapping("/danmaku/rooms/{roomId}/mute/{userId}")
    public Map<String, Object> muteUser(
            @Parameter(description = "房间 ID") @PathVariable("roomId") String roomId,
            @Parameter(description = "要禁言的用户 ID") @PathVariable("userId") String userId) {
        danmakuService.getMuteService().mute(roomId, userId);
        return Map.of("roomId", roomId, "userId", userId, "muted", true);
    }

    /**
     * 解除禁言。
     *
     * @param roomId 房间 ID
     * @param userId 要解除禁言的用户 ID
     * @return 解除结果
     */
    @Operation(summary = "解除禁言")
    @PostMapping("/danmaku/rooms/{roomId}/unmute/{userId}")
    public Map<String, Object> unmuteUser(
            @Parameter(description = "房间 ID") @PathVariable("roomId") String roomId,
            @Parameter(description = "要解除禁言的用户 ID") @PathVariable("userId") String userId) {
        danmakuService.getMuteService().unmute(roomId, userId);
        return Map.of("roomId", roomId, "userId", userId, "muted", false);
    }

    /**
     * 获取已禁言用户列表。
     *
     * @param roomId 房间 ID
     * @return 禁言用户集合
     */
    @Operation(summary = "获取已禁言用户列表")
    @GetMapping("/danmaku/rooms/{roomId}/muted")
    public Map<String, Object> getMutedUsers(
            @Parameter(description = "房间 ID") @PathVariable("roomId") String roomId) {
        Set<String> muted = danmakuService.getMuteService().getMutedUsers(roomId);
        return Map.of("roomId", roomId, "mutedUsers", muted);
    }

    // ---- 聊天端点 ----

    /**
     * 获取聊天历史记录。
     *
     * @param conversationId 会话 ID
     * @param limit          返回条数上限，默认 50
     * @return 历史消息列表
     */
    @Operation(summary = "获取聊天历史记录")
    @GetMapping("/chat/conversations/{conversationId}/history")
    public Mono<Map<String, Object>> getHistory(
            @Parameter(description = "会话 ID") @PathVariable("conversationId") String conversationId,
            @Parameter(description = "返回条数上限") @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return chatService.getHistory(conversationId, limit)
                .map(messages -> Map.of("conversationId", conversationId, "messages", messages, "count", messages.size()));
    }

    /**
     * 获取未读消息数。
     *
     * @param conversationId 会话 ID
     * @param userId         用户 ID
     * @return 未读消息数量
     */
    @Operation(summary = "获取未读消息数")
    @GetMapping("/chat/conversations/{conversationId}/unread/{userId}")
    public Mono<Map<String, Object>> getUnreadCount(
            @Parameter(description = "会话 ID") @PathVariable("conversationId") String conversationId,
            @Parameter(description = "用户 ID") @PathVariable("userId") String userId) {
        return chatService.getUnreadCount(conversationId, userId)
                .map(count -> Map.of("conversationId", conversationId, "userId", userId, "unreadCount", count));
    }

    // ---- 订单端点 ----

    /**
     * 创建订单。
     *
     * @param request 创建订单请求体
     * @return 创建结果，包含订单 ID 和推送状态
     */
    @Operation(summary = "创建订单")
    @PostMapping("/orders")
    public Mono<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        // 生成订单 ID，取 UUID 前 8 位并转大写
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OrderInfo order = new OrderInfo(orderId, request.userId(), request.product(), request.quantity(),
                "CREATED", Instant.now());
        orders.put(orderId, order);

        // 构建订单创建通知并推送给用户
        WsMessage<Map<String, Object>> message = buildOrderMessage("order.created", order);
        return sender.sendToUser(request.userId(), message)
                .map(sent -> Map.of("orderId", orderId, "status", "CREATED", "sent", sent));
    }

    /**
     * 更新订单状态。
     *
     * @param orderId 订单 ID
     * @param request 状态更新请求体
     * @return 更新结果
     */
    @Operation(summary = "更新订单状态")
    @PostMapping("/orders/{orderId}/status")
    public Mono<Map<String, Object>> updateStatus(
            @Parameter(description = "订单 ID") @PathVariable("orderId") String orderId,
            @RequestBody UpdateStatusRequest request) {
        OrderInfo order = orders.get(orderId);
        if (order == null) {
            return Mono.just(Map.of("error", "Order not found", "orderId", orderId));
        }

        // 用新状态覆盖旧订单记录
        OrderInfo updated = new OrderInfo(order.orderId(), order.userId(), order.product(),
                order.quantity(), request.status(), Instant.now());
        orders.put(orderId, updated);

        // 构建状态变更通知并推送给用户
        WsMessage<Map<String, Object>> message = buildOrderMessage("order.status.changed", updated);
        return sender.sendToUser(updated.userId(), message)
                .map(sent -> Map.of("orderId", orderId, "status", request.status(), "sent", sent));
    }

    /**
     * 获取订单详情。
     *
     * @param orderId 订单 ID
     * @return 订单信息
     */
    @Operation(summary = "获取订单详情")
    @GetMapping("/orders/{orderId}")
    public Mono<Map<String, Object>> getOrder(
            @Parameter(description = "订单 ID") @PathVariable("orderId") String orderId) {
        OrderInfo order = orders.get(orderId);
        if (order == null) {
            return Mono.just(Map.of("error", "Order not found", "orderId", orderId));
        }
        return Mono.just(Map.of(
                "orderId", order.orderId(),
                "userId", order.userId(),
                "product", order.product(),
                "quantity", order.quantity(),
                "status", order.status(),
                "updatedAt", order.updatedAt().toString()
        ));
    }

    /**
     * 获取用户订单列表。
     *
     * @param userId 用户 ID
     * @return 该用户的所有订单
     */
    @Operation(summary = "获取用户订单列表")
    @GetMapping("/orders/user/{userId}")
    public Mono<Map<String, Object>> getUserOrders(
            @Parameter(description = "用户 ID") @PathVariable("userId") String userId) {
        List<OrderInfo> userOrders = orders.values().stream()
                .filter(o -> o.userId().equals(userId))
                .toList();
        return Mono.just(Map.of("userId", userId, "orders", userOrders, "count", userOrders.size()));
    }

    // ---- 辅助方法 ----

    /**
     * 构建订单相关 WebSocket 消息。
     *
     * @param type  消息类型
     * @param order 订单信息
     * @return 格式化后的 WebSocket 消息
     */
    private WsMessage<Map<String, Object>> buildOrderMessage(String type, OrderInfo order) {
        Map<String, Object> payload = Map.of(
                "orderId", order.orderId(),
                "userId", order.userId(),
                "product", order.product(),
                "quantity", order.quantity(),
                "status", order.status(),
                "updatedAt", order.updatedAt().toString()
        );
        return new WsMessage<>(null, type, null, Instant.now(), payload);
    }

    // ---- 内部记录 ----

    /**
     * 创建订单请求体。
     */
    public record CreateOrderRequest(String userId, String product, int quantity) {}

    /**
     * 更新订单状态请求体。
     */
    public record UpdateStatusRequest(String status) {}

    /**
     * 订单信息。
     */
    public record OrderInfo(String orderId, String userId, String product, int quantity,
                            String status, Instant updatedAt) {}
}
