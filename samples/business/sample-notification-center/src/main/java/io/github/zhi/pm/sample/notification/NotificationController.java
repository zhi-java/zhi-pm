package io.github.zhi.pm.sample.notification;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final ConnectionRegistry registry;
    private final MessageSender sender;

    public NotificationController(ConnectionRegistry registry, MessageSender sender) {
        this.registry = registry;
        this.sender = sender;
    }

    @PostMapping("/users/{userId}")
    public Mono<Map<String, Object>> sendToUser(@PathVariable("userId") String userId, @RequestBody NotificationRequest request) {
        WsMessage<?> message = buildNotification("notification.user", request.title(), request.content(), request.level());
        return sender.sendToUser(userId, message).map(sent -> Map.of("userId", userId, "sent", sent));
    }

    @PostMapping("/users/batch")
    public Mono<Map<String, Object>> sendToUsers(@RequestBody BatchNotificationRequest request) {
        WsMessage<?> message = buildNotification("notification.batch", request.title(), request.content(), request.level());
        return sender.sendToUsers(request.userIds(), message).map(count -> Map.of("sent", count, "total", request.userIds().size()));
    }

    @PostMapping("/rooms/{roomId}")
    public Mono<Map<String, Object>> sendToRoom(@PathVariable("roomId") String roomId, @RequestBody NotificationRequest request) {
        WsMessage<?> message = buildNotification("notification.room", request.title(), request.content(), request.level());
        return sender.sendToRoom(roomId, message).map(count -> Map.of("roomId", roomId, "sent", count));
    }

    @PostMapping("/broadcast")
    public Mono<Map<String, Object>> broadcast(@RequestBody NotificationRequest request) {
        WsMessage<?> message = buildNotification("notification.broadcast", request.title(), request.content(), request.level());
        return sender.broadcast(message).map(count -> Map.of("sent", count));
    }

    @GetMapping("/users/{userId}/online")
    public Mono<Map<String, Object>> isOnline(@PathVariable("userId") String userId) {
        return registry.isOnline(userId).map(online -> Map.of("userId", userId, "online", online));
    }

    @GetMapping("/stats")
    public Mono<Map<String, Object>> stats() {
        return registry.countConnections().map(count -> Map.of("totalConnections", count));
    }

    private WsMessage<Map<String, Object>> buildNotification(String type, String title, String content, String level) {
        Map<String, Object> payload = Map.of(
                "title", title == null ? "" : title,
                "content", content == null ? "" : content,
                "level", level == null ? "info" : level
        );
        return new WsMessage<>(null, type, null, Instant.now(), payload);
    }

    public record NotificationRequest(String title, String content, String level) {}

    public record BatchNotificationRequest(List<String> userIds, String title, String content, String level) {}
}
