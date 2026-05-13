package io.github.zhi.pm.admin;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.core.session.SessionConnection;
import io.github.zhi.pm.observability.GatewayMetrics;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service providing admin operations for the gateway.
 */
public class AdminService {
    private final ConnectionRegistry registry;
    private final MessageSender sender;
    private final GatewayMetrics metrics;

    public AdminService(ConnectionRegistry registry, MessageSender sender, GatewayMetrics metrics) {
        this.registry = registry;
        this.sender = sender;
        this.metrics = metrics;
    }

    /**
     * List all active connections.
     */
    public Flux<Map<String, Object>> listConnections() {
        return registry.connections().map(this::toConnectionInfo);
    }

    /**
     * Get details of a specific connection.
     */
    public Mono<Map<String, Object>> getConnection(String sessionId) {
        return registry.getConnection(sessionId).map(this::toConnectionInfo);
    }

    /**
     * Kick (close) a specific connection.
     */
    public Mono<Boolean> kickConnection(String sessionId) {
        return registry.getConnection(sessionId)
                .flatMap(conn -> conn.close("kicked by admin").thenReturn(true))
                .defaultIfEmpty(false);
    }

    /**
     * List all active rooms with member counts.
     */
    public Flux<Map<String, Object>> listRooms() {
        return registry.getRoomIds()
                .flatMap(roomId -> registry.countRoomConnections(roomId)
                        .map(count -> {
                            Map<String, Object> room = new LinkedHashMap<>();
                            room.put("roomId", roomId);
                            room.put("memberCount", count);
                            return room;
                        }));
    }

    /**
     * Get members of a specific room.
     */
    public Flux<Map<String, Object>> getRoomMembers(String roomId) {
        return registry.getRoomConnections(roomId).map(this::toConnectionInfo);
    }

    /**
     * Broadcast a message to all connected clients.
     */
    public Mono<Map<String, Object>> broadcastMessage(String type, Object payload) {
        WsMessage<?> message = new WsMessage<>(null, type, null, Instant.now(),
                payload == null ? Collections.emptyMap() : payload);
        return sender.broadcast(message).map(count -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sent", count);
            result.put("type", type);
            return result;
        });
    }

    /**
     * Get overall gateway statistics.
     */
    public Mono<Map<String, Object>> getStats() {
        return Mono.fromSupplier(() -> {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("activeConnections", metrics.getActiveConnections());
            stats.put("onlineUsers", metrics.getOnlineUsers());
            stats.put("activeRooms", metrics.getActiveRooms());
            stats.put("pushSuccessRate", metrics.getPushSuccessRate());
            return stats;
        });
    }

    private Map<String, Object> toConnectionInfo(SessionConnection conn) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("sessionId", conn.sessionId());
        info.put("userId", conn.userId());
        info.put("connectedAt", conn.connectedAt().toString());
        info.put("lastHeartbeatAt", conn.lastHeartbeatAt().toString());
        info.put("attributes", conn.attributes());
        return info;
    }
}
