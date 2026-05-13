package io.github.zhi.pm.core.registry;

import io.github.zhi.pm.core.session.SessionConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConnectionRegistry {
    Mono<Void> register(SessionConnection connection);
    Mono<Void> unregister(String sessionId);
    Mono<SessionConnection> getConnection(String sessionId);
    Flux<SessionConnection> getUserConnections(String userId);
    Mono<Boolean> isOnline(String userId);
    Mono<Long> countConnections();
    Flux<SessionConnection> connections();
    void unregisterNow(String sessionId);

    // Room support
    Mono<Void> joinRoom(String roomId, String sessionId);
    Mono<Void> leaveRoom(String roomId, String sessionId);
    Mono<Void> leaveAllRooms(String sessionId);
    Flux<SessionConnection> getRoomConnections(String roomId);
    Mono<Long> countRoomConnections(String roomId);
    Flux<String> getSessionRooms(String sessionId);
}
