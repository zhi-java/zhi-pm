package io.github.zhi.pm.core.session;

import io.github.zhi.pm.core.message.WsMessage;
import java.time.Instant;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SessionConnection {
    String sessionId();
    String userId();
    Map<String, String> attributes();
    Instant connectedAt();
    Instant lastHeartbeatAt();
    void markHeartbeat(Instant heartbeatAt);
    SendOutcome trySend(WsMessage<?> message);
    Flux<WsMessage<?>> outboundMessages();
    Mono<Void> close(String reason);
    void completeOutbound();
}
