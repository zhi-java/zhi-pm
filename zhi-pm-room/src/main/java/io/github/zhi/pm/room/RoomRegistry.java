package io.github.zhi.pm.room;

import io.github.zhi.pm.core.session.SessionConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Registry for managing room membership and room connections.
 */
public interface RoomRegistry {

    /**
     * Join a room.
     */
    Mono<Void> joinRoom(String roomId, SessionConnection connection);

    /**
     * Leave a room.
     */
    Mono<Void> leaveRoom(String roomId, String sessionId);

    /**
     * Leave all rooms for a session.
     */
    Mono<Void> leaveAllRooms(String sessionId);

    /**
     * Get all connections in a room.
     */
    Flux<SessionConnection> getRoomConnections(String roomId);

    /**
     * Count connections in a room.
     */
    Mono<Long> countRoomConnections(String roomId);

    /**
     * Check if a session is in a room.
     */
    Mono<Boolean> isInRoom(String roomId, String sessionId);
}
