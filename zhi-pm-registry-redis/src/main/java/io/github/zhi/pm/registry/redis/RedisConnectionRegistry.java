package io.github.zhi.pm.registry.redis;

import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.session.SessionConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class RedisConnectionRegistry implements ConnectionRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(RedisConnectionRegistry.class);
    private final ConcurrentMap<String, SessionConnection> localConnections = new ConcurrentHashMap<>();
    private final ReactiveRedisTemplate<String, String> redis;
    private final String prefix;
    private final Duration sessionTtl;

    public RedisConnectionRegistry(ReactiveRedisTemplate<String, String> redis, String prefix, Duration sessionTtl) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.prefix = prefix.endsWith(":") ? prefix : prefix + ":";
        this.sessionTtl = Objects.requireNonNull(sessionTtl, "sessionTtl");
    }

    // --- Key helpers ---

    private String connKey(String sessionId) { return prefix + "conn:" + sessionId; }
    private String userSessionsKey(String userId) { return prefix + "user:" + userId + ":sessions"; }
    private String roomMembersKey(String roomId) { return prefix + "room:" + roomId + ":members"; }
    private String sessionRoomsKey(String sessionId) { return prefix + "session:" + sessionId + ":rooms"; }
    private String globalSessionsKey() { return prefix + "sessions"; }

    // --- Registration ---

    @Override
    public Mono<Void> register(SessionConnection connection) {
        String sid = connection.sessionId();
        String uid = connection.userId();
        localConnections.put(sid, connection);

        String ck = connKey(sid);
        Map<String, String> fields = Map.of(
                "userId", uid,
                "connectedAt", connection.connectedAt().toString()
        );

        return redis.opsForHash().putAll(ck, fields)
                .then(redis.expire(ck, sessionTtl))
                .then(redis.opsForSet().add(userSessionsKey(uid), sid))
                .then(redis.opsForSet().add(globalSessionsKey(), sid))
                .then();
    }

    @Override
    public Mono<Void> unregister(String sessionId) {
        return Mono.fromRunnable(() -> unregisterNow(sessionId));
    }

    @Override
    public void unregisterNow(String sessionId) {
        SessionConnection removed = localConnections.remove(sessionId);
        if (removed == null) return;

        String uid = removed.userId();
        String ck = connKey(sessionId);
        String usKey = userSessionsKey(uid);
        String gsKey = globalSessionsKey();

        // Fetch rooms this session was in, then clean up everything.
        // Subscribe with error logging so failures are observable rather than silently swallowed.
        redis.opsForSet().members(sessionRoomsKey(sessionId))
                .flatMap(roomId -> redis.opsForSet().remove(roomMembersKey(roomId), sessionId).then())
                .then(redis.delete(sessionRoomsKey(sessionId)))
                .then(redis.opsForSet().remove(usKey, sessionId))
                .then(redis.opsForSet().remove(gsKey, sessionId))
                .then(redis.delete(ck))
                .doFinally(signal -> removed.completeOutbound())
                .subscribe(
                        v -> { /* onNext: unused for void chain */ },
                        error -> LOG.warn("Failed to clean up Redis state for session {}", sessionId, error)
                );
    }

    // --- Connection queries ---

    @Override
    public Mono<SessionConnection> getConnection(String sessionId) {
        return Mono.justOrEmpty(localConnections.get(sessionId));
    }

    @Override
    public Flux<SessionConnection> getUserConnections(String userId) {
        return redis.opsForSet().members(userSessionsKey(userId))
                .flatMap(sid -> Mono.justOrEmpty(localConnections.get(sid)));
    }

    @Override
    public Mono<Boolean> isOnline(String userId) {
        return redis.opsForSet().size(userSessionsKey(userId)).map(count -> count > 0);
    }

    @Override
    public Mono<Long> countConnections() {
        return redis.opsForSet().size(globalSessionsKey());
    }

    @Override
    public Flux<SessionConnection> connections() {
        return Flux.defer(() -> Flux.fromIterable(localConnections.values()));
    }

    // --- Room support ---

    @Override
    public Mono<Void> joinRoom(String roomId, String sessionId) {
        if (!localConnections.containsKey(sessionId)) return Mono.empty();
        return redis.opsForSet().add(roomMembersKey(roomId), sessionId)
                .then(redis.opsForSet().add(sessionRoomsKey(sessionId), roomId))
                .then();
    }

    @Override
    public Mono<Void> leaveRoom(String roomId, String sessionId) {
        return redis.opsForSet().remove(roomMembersKey(roomId), sessionId)
                .then(redis.opsForSet().remove(sessionRoomsKey(sessionId), roomId))
                .then();
    }

    @Override
    public Mono<Void> leaveAllRooms(String sessionId) {
        return redis.opsForSet().members(sessionRoomsKey(sessionId))
                .flatMap(roomId -> leaveRoom(roomId, sessionId))
                .then(redis.delete(sessionRoomsKey(sessionId)).then());
    }

    @Override
    public Flux<SessionConnection> getRoomConnections(String roomId) {
        return redis.opsForSet().members(roomMembersKey(roomId))
                .flatMap(sid -> Mono.justOrEmpty(localConnections.get(sid)));
    }

    @Override
    public Mono<Long> countRoomConnections(String roomId) {
        return redis.opsForSet().size(roomMembersKey(roomId));
    }

    @Override
    public Flux<String> getSessionRooms(String sessionId) {
        return redis.opsForSet().members(sessionRoomsKey(sessionId));
    }

    @Override
    public Flux<String> getRoomIds() {
        return redis.keys(prefix + "room:*:members")
                .map(key -> {
                    String s = key.substring(prefix.length() + 5);
                    return s.substring(0, s.length() - 8);
                });
    }

    // --- Session lease renewal (called on heartbeat) ---

    public Mono<Void> renewSessionLease(String sessionId) {
        SessionConnection conn = localConnections.get(sessionId);
        if (conn == null) return Mono.empty();
        return redis.expire(connKey(sessionId), sessionTtl)
                .then(redis.expire(userSessionsKey(conn.userId()), sessionTtl))
                .then();
    }
}
