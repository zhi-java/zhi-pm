package io.github.zhi.pm.core.session;

import io.github.zhi.pm.core.message.WsMessage;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public final class DefaultSessionConnection implements SessionConnection {
    private final String sessionId;
    private final String userId;
    private final Map<String, String> attributes;
    private final Instant connectedAt;
    private final AtomicReference<Instant> lastHeartbeatAt;
    private final Sinks.Many<WsMessage<?>> outbound;
    private final Function<String, Mono<Void>> closeAction;

    public DefaultSessionConnection(String sessionId, String userId, Map<String, String> attributes, int outboundBufferSize, Function<String, Mono<Void>> closeAction) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(attributes));
        this.connectedAt = Instant.now();
        this.lastHeartbeatAt = new AtomicReference<>(connectedAt);
        this.outbound = Sinks.many().unicast().onBackpressureBuffer(new ArrayBlockingQueue<>(Math.max(1, outboundBufferSize)));
        this.closeAction = closeAction == null ? reason -> Mono.empty() : closeAction;
    }

    @Override public String sessionId() { return sessionId; }
    @Override public String userId() { return userId; }
    @Override public Map<String, String> attributes() { return attributes; }
    @Override public Instant connectedAt() { return connectedAt; }
    @Override public Instant lastHeartbeatAt() { return lastHeartbeatAt.get(); }
    @Override public void markHeartbeat(Instant heartbeatAt) { lastHeartbeatAt.set(Objects.requireNonNull(heartbeatAt, "heartbeatAt")); }

    @Override
    public SendOutcome trySend(WsMessage<?> message) {
        Sinks.EmitResult result = outbound.tryEmitNext(Objects.requireNonNull(message, "message"));
        if (result.isSuccess()) {
            return SendOutcome.SENT;
        }
        if (result == Sinks.EmitResult.FAIL_OVERFLOW) {
            return SendOutcome.OVERFLOW;
        }
        if (result == Sinks.EmitResult.FAIL_TERMINATED) {
            return SendOutcome.TERMINATED;
        }
        if (result == Sinks.EmitResult.FAIL_CANCELLED) {
            return SendOutcome.CANCELLED;
        }
        if (result == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
            return SendOutcome.NON_SERIALIZED;
        }
        return SendOutcome.FAILED;
    }

    @Override public Flux<WsMessage<?>> outboundMessages() { return outbound.asFlux(); }
    @Override public Mono<Void> close(String reason) { return closeAction.apply(reason).doFinally(signal -> completeOutbound()); }
    @Override public void completeOutbound() { outbound.tryEmitComplete(); }
}
