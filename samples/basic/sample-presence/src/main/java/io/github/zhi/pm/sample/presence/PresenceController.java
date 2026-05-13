package io.github.zhi.pm.sample.presence;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {
    private final ConnectionRegistry registry;
    private final MessageSender sender;

    public PresenceController(ConnectionRegistry registry, MessageSender sender) {
        this.registry = registry;
        this.sender = sender;
    }

    @GetMapping("/users/{userId}/online")
    public Mono<Map<String, Object>> isOnline(@PathVariable("userId") String userId) {
        return registry.isOnline(userId).map(online -> Map.of("userId", userId, "online", online));
    }

    @GetMapping("/connections/count")
    public Mono<Map<String, Object>> connectionCount() {
        return registry.countConnections().map(count -> Map.of("count", count));
    }

    @GetMapping("/rooms/{roomId}/count")
    public Mono<Map<String, Object>> roomCount(@PathVariable("roomId") String roomId) {
        return registry.countRoomConnections(roomId).map(count -> Map.of("roomId", roomId, "count", count));
    }

    @PostMapping("/users/{userId}/push")
    public Mono<Map<String, Object>> pushToUser(@PathVariable("userId") String userId, @RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = new WsMessage<>(null, "presence.notification", null, Instant.now(), payload == null ? Collections.emptyMap() : payload);
        return sender.sendToUser(userId, message).map(sent -> Map.of("userId", userId, "sent", sent));
    }

    @PostMapping("/rooms/{roomId}/push")
    public Mono<Map<String, Object>> pushToRoom(@PathVariable("roomId") String roomId, @RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = new WsMessage<>(null, "presence.room-notification", null, Instant.now(), payload == null ? Collections.emptyMap() : payload);
        return sender.sendToRoom(roomId, message).map(count -> Map.of("roomId", roomId, "sent", count));
    }

    @PostMapping("/broadcast")
    public Mono<Map<String, Object>> broadcast(@RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = new WsMessage<>(null, "presence.broadcast", null, Instant.now(), payload == null ? Collections.emptyMap() : payload);
        return sender.broadcast(message).map(count -> Map.of("sent", count));
    }
}
