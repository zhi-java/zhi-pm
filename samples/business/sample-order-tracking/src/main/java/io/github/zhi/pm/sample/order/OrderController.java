package io.github.zhi.pm.sample.order;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final ConnectionRegistry registry;
    private final MessageSender sender;
    private final Map<String, OrderInfo> orders = new ConcurrentHashMap<>();

    public OrderController(ConnectionRegistry registry, MessageSender sender) {
        this.registry = registry;
        this.sender = sender;
    }

    @PostMapping
    public Mono<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OrderInfo order = new OrderInfo(orderId, request.userId(), request.product(), request.quantity(), "CREATED", Instant.now());
        orders.put(orderId, order);

        WsMessage<Map<String, Object>> message = buildOrderMessage("order.created", order);
        return sender.sendToUser(request.userId(), message)
                .map(sent -> Map.of("orderId", orderId, "status", "CREATED", "sent", sent));
    }

    @PostMapping("/{orderId}/status")
    public Mono<Map<String, Object>> updateStatus(@PathVariable("orderId") String orderId,
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

    @GetMapping("/{orderId}")
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

    @GetMapping("/user/{userId}")
    public Mono<Map<String, Object>> getUserOrders(@PathVariable("userId") String userId) {
        var userOrders = orders.values().stream()
                .filter(o -> o.userId().equals(userId))
                .toList();
        return Mono.just(Map.of("userId", userId, "orders", userOrders, "count", userOrders.size()));
    }

    @GetMapping("/stats")
    public Mono<Map<String, Object>> stats() {
        return registry.countConnections().map(count -> Map.of("totalConnections", count, "totalOrders", orders.size()));
    }

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

    public record CreateOrderRequest(String userId, String product, int quantity) {}
    public record UpdateStatusRequest(String status) {}
    public record OrderInfo(String orderId, String userId, String product, int quantity, String status, Instant updatedAt) {}
}
