package io.github.zhi.pm.core.registry;

import io.github.zhi.pm.core.session.SessionConnection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class InMemoryConnectionRegistry implements ConnectionRegistry {
    private final ConcurrentMap<String, SessionConnection> bySessionId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> sessionsByUserId = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> register(SessionConnection connection) {
        return Mono.fromRunnable(() -> registerNow(connection));
    }

    public void registerNow(SessionConnection connection) {
        Objects.requireNonNull(connection, "connection");
        SessionConnection previous = bySessionId.put(connection.sessionId(), connection);
        if (previous != null) {
            removeUserIndex(previous.userId(), previous.sessionId());
        }
        sessionsByUserId.computeIfAbsent(connection.userId(), ignored -> ConcurrentHashMap.newKeySet()).add(connection.sessionId());
    }

    @Override
    public Mono<Void> unregister(String sessionId) {
        return Mono.fromRunnable(() -> unregisterNow(sessionId));
    }

    @Override
    public void unregisterNow(String sessionId) {
        SessionConnection removed = bySessionId.remove(sessionId);
        if (removed != null) {
            removeUserIndex(removed.userId(), removed.sessionId());
            removed.completeOutbound();
        }
    }

    @Override public Mono<SessionConnection> getConnection(String sessionId) { return Mono.justOrEmpty(bySessionId.get(sessionId)); }

    @Override
    public Flux<SessionConnection> getUserConnections(String userId) {
        return Flux.fromIterable(sessionsByUserId.getOrDefault(userId, Collections.emptySet())).flatMap(this::getConnection);
    }

    @Override public Mono<Boolean> isOnline(String userId) { return Mono.fromSupplier(() -> sessionsByUserId.containsKey(userId)); }
    @Override public Mono<Long> countConnections() { return Mono.fromSupplier(() -> (long) bySessionId.size()); }
    @Override public Flux<SessionConnection> connections() { return Flux.defer(() -> Flux.fromIterable(bySessionId.values())); }

    private void removeUserIndex(String userId, String sessionId) {
        sessionsByUserId.computeIfPresent(userId, (key, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
    }
}
