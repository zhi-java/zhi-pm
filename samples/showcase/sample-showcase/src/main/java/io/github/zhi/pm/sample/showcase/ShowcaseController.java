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

    // ---- Push endpoints ----

    @PostMapping("/push/users/{userId}")
    public Mono<Map<String, Object>> pushToUser(@PathVariable("userId") String userId,
                                                @RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = WsMessage.of("showcase.user-push", payload == null ? Collections.emptyMap() : payload);
        return sender.sendToUser(userId, message).map(sent -> Map.of("userId", userId, "sent", sent));
    }

    @PostMapping("/push/broadcast")
    public Mono<Map<String, Object>> broadcast(@RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = WsMessage.of("showcase.broadcast", payload == null ? Collections.emptyMap() : payload);
        return sender.broadcast(message).map(count -> Map.of("sent", count));
    }

    @PostMapping("/push/rooms/{roomId}")
    public Mono<Map<String, Object>> pushToRoom(@PathVariable("roomId") String roomId,
                                                @RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = WsMessage.of("showcase.room-push", payload == null ? Collections.emptyMap() : payload);
        return sender.sendToRoom(roomId, message).map(count -> Map.of("roomId", roomId, "sent", count));
    }

    // ---- Presence endpoints ----

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

    // ---- Danmaku endpoints ----

    @PostMapping("/danmaku/rooms/{roomId}")
    public Mono<Map<String, Object>> sendDanmaku(@PathVariable("roomId") String roomId,
                                                 @RequestParam("content") String content,
                                                 @RequestParam(value = "userId", defaultValue = "system") String userId) {
        WsMessage<Map<String, Object>> message = new WsMessage<>(null, "danmaku.message", null, null,
                userId, null, roomId, Instant.now(), Map.of("content", content));
        return sender.sendToRoom(roomId, message).map(count -> Map.of("roomId", roomId, "sent", count));
    }

    @PostMapping("/danmaku/rooms/{roomId}/mute/{userId}")
    public Map<String, Object> muteUser(@PathVariable("roomId") String roomId,
                                        @PathVariable("userId") String userId) {
        danmakuService.getMuteService().mute(roomId, userId);
        return Map.of("roomId", roomId, "userId", userId, "muted", true);
    }

    @PostMapping("/danmaku/rooms/{roomId}/unmute/{userId}")
    public Map<String, Object> unmuteUser(@PathVariable("roomId") String roomId,
                                          @PathVariable("userId") String userId) {
        danmakuService.getMuteService().unmute(roomId, userId);
        return Map.of("roomId", roomId, "userId", userId, "muted", false);
    }

    @GetMapping("/danmaku/rooms/{roomId}/muted")
    public Map<String, Object> getMutedUsers(@PathVariable("roomId") String roomId) {
        Set<String> muted = danmakuService.getMuteService().getMutedUsers(roomId);
        return Map.of("roomId", roomId, "mutedUsers", muted);
    }

    // ---- Chat endpoints ----

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

    // ---- Order endpoints ----

    @PostMapping("/orders")
    public Mono<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OrderInfo order = new OrderInfo(orderId, request.userId(), request.product(), request.quantity(),
                "CREATED", Instant.now());
        orders.put(orderId, order);

        WsMessage<Map<String, Object>> message = buildOrderMessage("order.created", order);
        return sender.sendToUser(request.userId(), message)
                .map(sent -> Map.of("orderId", orderId, "status", "CREATED", "sent", sent));
    }

    @PostMapping("/orders/{orderId}/status")
    public Mono<Map<String, Object>> updateStatus(@PathVariable("orderId") String orderId,
                                                  @RequestBody UpdateStatusRequest request) {
        OrderInfo order = orders.get(orderId);
        if (order == null) {
            return Mono.just(Map.of("error", "Order not found", "orderId", orderId));
        }

        OrderInfo updated = new OrderInfo(order.orderId(), order.userId(), order.product(),
                order.quantity(), request.status(), Instant.now());
        orders.put(orderId, updated);

        WsMessage<Map<String, Object>> message = buildOrderMessage("order.status.changed", updated);
        return sender.sendToUser(updated.userId(), message)
                .map(sent -> Map.of("orderId", orderId, "status", request.status(), "sent", sent));
    }

    @GetMapping("/orders/{orderId}")
    public Mono<Map<String, Object>> getOrder(@PathVariable("orderId") String orderId) {
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

    @GetMapping("/orders/user/{userId}")
    public Mono<Map<String, Object>> getUserOrders(@PathVariable("userId") String userId) {
        List<OrderInfo> userOrders = orders.values().stream()
                .filter(o -> o.userId().equals(userId))
                .toList();
        return Mono.just(Map.of("userId", userId, "orders", userOrders, "count", userOrders.size()));
    }

    // ---- Helper methods ----

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

    // ---- Inner records ----

    public record CreateOrderRequest(String userId, String product, int quantity) {}

    public record UpdateStatusRequest(String status) {}

    public record OrderInfo(String orderId, String userId, String product, int quantity,
                            String status, Instant updatedAt) {}
}
