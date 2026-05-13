package io.github.zhi.pm.core.send;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.session.SendOutcome;
import io.github.zhi.pm.core.session.SessionConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class LocalMessageSender implements MessageSender {
    private final ConnectionRegistry registry;

    public LocalMessageSender(ConnectionRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public Mono<Boolean> sendToUser(String userId, WsMessage<?> message) {
        return registry.getUserConnections(userId)
                .flatMap(connection -> send(connection, message))
                .any(Boolean.TRUE::equals);
    }

    @Override
    public Mono<Integer> sendToUsers(Collection<String> userIds, WsMessage<?> message) {
        return Flux.fromIterable(new HashSet<>(userIds))
                .flatMap(userId -> sendToUser(userId, message))
                .filter(Boolean.TRUE::equals)
                .count()
                .map(Long::intValue);
    }

    @Override
    public Mono<Integer> sendToRoom(String roomId, WsMessage<?> message) {
        return registry.getRoomConnections(roomId)
                .flatMap(connection -> send(connection, message))
                .filter(Boolean.TRUE::equals)
                .count()
                .map(Long::intValue);
    }

    @Override
    public Mono<Integer> broadcast(WsMessage<?> message) {
        return registry.connections()
                .flatMap(connection -> send(connection, message))
                .filter(Boolean.TRUE::equals)
                .count()
                .map(Long::intValue);
    }

    private Mono<Boolean> send(SessionConnection connection, WsMessage<?> message) {
        SendOutcome outcome = connection.trySend(message);
        if (outcome.sent()) {
            return Mono.just(true);
        }
        if (outcome == SendOutcome.OVERFLOW) {
            return connection.close("outbound buffer overflow").thenReturn(false);
        }
        if (outcome == SendOutcome.TERMINATED || outcome == SendOutcome.CANCELLED) {
            return registry.unregister(connection.sessionId()).thenReturn(false);
        }
        return Mono.just(false);
    }
}
