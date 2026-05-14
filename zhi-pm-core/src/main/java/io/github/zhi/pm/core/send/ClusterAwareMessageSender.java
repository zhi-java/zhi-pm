package io.github.zhi.pm.core.send;

import io.github.zhi.pm.core.broker.BrokerMessage;
import io.github.zhi.pm.core.broker.MessageBroker;
import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.session.SendOutcome;
import io.github.zhi.pm.core.session.SessionConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class ClusterAwareMessageSender implements MessageSender {
    private final ConnectionRegistry registry;
    private final MessageBroker broker;
    private final String instanceId;

    public ClusterAwareMessageSender(ConnectionRegistry registry, MessageBroker broker, String instanceId) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.broker = Objects.requireNonNull(broker, "broker");
        this.instanceId = instanceId;
    }

    @Override
    public Mono<Boolean> sendToUser(String userId, WsMessage<?> message) {
        Mono<Boolean> localDelivery = registry.getUserConnections(userId)
                .flatMap(c -> send(c, message))
                .any(Boolean.TRUE::equals);

        Mono<Boolean> brokerDelivery = broker.publish(BrokerMessage.forUser(userId, message, instanceId))
                .thenReturn(true);

        // Always publish to broker for cross-instance delivery; local delivery
        // only covers connections on this instance. Other instances may also hold
        // connections for the same user.
        return localDelivery.zipWith(brokerDelivery, (local, broker) -> local || broker)
                .switchIfEmpty(brokerDelivery);
    }

    @Override
    public Mono<Integer> sendToUsers(Collection<String> userIds, WsMessage<?> message) {
        return Flux.fromIterable(new HashSet<>(userIds))
                .flatMap(userId -> sendToUser(userId, message).filter(Boolean.TRUE::equals).thenReturn(1))
                .count()
                .map(Long::intValue);
    }

    @Override
    public Mono<Integer> sendToRoom(String roomId, WsMessage<?> message) {
        Mono<Integer> localDelivery = registry.getRoomConnections(roomId)
                .flatMap(c -> send(c, message))
                .filter(Boolean.TRUE::equals)
                .count()
                .map(Long::intValue);

        Mono<Void> brokerPublish = broker.publish(BrokerMessage.forRoom(roomId, message, instanceId)).then();

        return localDelivery.zipWith(brokerPublish.then(Mono.just(0)), Integer::sum);
    }

    @Override
    public Mono<Integer> broadcast(WsMessage<?> message) {
        Mono<Integer> localDelivery = registry.connections()
                .flatMap(c -> send(c, message))
                .filter(Boolean.TRUE::equals)
                .count()
                .map(Long::intValue);

        Mono<Void> brokerPublish = broker.publish(BrokerMessage.forBroadcast(message, instanceId)).then();

        return localDelivery.zipWith(brokerPublish.then(Mono.just(0)), Integer::sum);
    }

    private Mono<Boolean> send(SessionConnection connection, WsMessage<?> message) {
        SendOutcome outcome = connection.trySend(message);
        if (outcome.sent()) return Mono.just(true);
        if (outcome == SendOutcome.OVERFLOW) {
            return connection.close("outbound buffer overflow").thenReturn(false);
        }
        if (outcome == SendOutcome.TERMINATED || outcome == SendOutcome.CANCELLED) {
            return registry.unregister(connection.sessionId()).thenReturn(false);
        }
        return Mono.just(false);
    }
}
