package io.github.zhi.pm.autoconfigure;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.core.auth.AuthenticationRequest;
import io.github.zhi.pm.core.auth.AuthenticationResult;
import io.github.zhi.pm.core.auth.WebSocketAuthenticator;
import io.github.zhi.pm.core.heartbeat.HeartbeatService;
import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.session.DefaultSessionConnection;
import io.github.zhi.pm.core.session.SendOutcome;
import io.github.zhi.pm.core.session.SessionConnection;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class GatewayWebSocketHandler implements WebSocketHandler {
    private final ConnectionRegistry registry;
    private final WebSocketAuthenticator authenticator;
    private final HeartbeatService heartbeatService;
    private final ObjectMapper objectMapper;
    private final RealtimeWebSocketProperties properties;
    private final JavaType messageType;

    public GatewayWebSocketHandler(ConnectionRegistry registry, WebSocketAuthenticator authenticator, HeartbeatService heartbeatService, ObjectMapper objectMapper, RealtimeWebSocketProperties properties) {
        this.registry = registry;
        this.authenticator = authenticator;
        this.heartbeatService = heartbeatService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.messageType = objectMapper.getTypeFactory().constructParametricType(WsMessage.class, JsonNode.class);
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return authenticate(session).flatMap(result -> {
            if (!result.authenticated()) {
                return session.close(CloseStatus.POLICY_VIOLATION);
            }
            SessionConnection connection = new DefaultSessionConnection(
                    session.getId(), result.userId(), result.attributes(), properties.getOutboundBufferSize(),
                    reason -> session.close(CloseStatus.GOING_AWAY.withReason(reason == null ? "closed" : reason)));
            Flux<WebSocketMessage> outbound = connection.outboundMessages().map(this::serialize).map(session::textMessage);
            Mono<Void> receive = session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .flatMap(text -> handleInbound(connection, text))
                    .doFinally(signal -> connection.completeOutbound())
                    .then();
            Mono<Void> send = session.send(outbound);
            Mono<Void> timeout = heartbeatTimeout(connection);
            return registry.register(connection)
                    .then(Mono.firstWithSignal(Mono.when(receive, send), timeout))
                    .doFinally(signal -> registry.unregisterNow(connection.sessionId()));
        });
    }

    private Mono<AuthenticationResult> authenticate(WebSocketSession session) {
        if (!properties.getAuth().isEnabled()) {
            return Mono.just(AuthenticationResult.authenticated("anonymous-" + session.getId()));
        }
        return authenticator.authenticate(new AuthenticationRequest(resolveToken(session), session.getHandshakeInfo().getHeaders(), remoteAddress(session)));
    }

    private String resolveToken(WebSocketSession session) {
        String queryToken = session.getHandshakeInfo().getUri().getQuery() == null ? null : UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri()).build().getQueryParams().getFirst(properties.getAuth().getTokenParamName());
        if (queryToken != null && !queryToken.trim().isEmpty()) {
            return queryToken;
        }
        String headerValue = session.getHandshakeInfo().getHeaders().getFirst(properties.getAuth().getHeaderName());
        if (headerValue != null && headerValue.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return headerValue.substring(7);
        }
        return headerValue;
    }

    private String remoteAddress(WebSocketSession session) {
        return Optional.ofNullable(session.getHandshakeInfo().getRemoteAddress()).map(Object::toString).orElse(null);
    }

    private Mono<Void> handleInbound(SessionConnection connection, String text) {
        return Mono.fromCallable(() -> parse(text))
                .flatMap(message -> {
                    if (properties.getHeartbeat().isEnabled() && heartbeatService.isPing(message)) {
                        heartbeatService.recordHeartbeat(connection);
                        return sendOrCloseOnFailure(connection, heartbeatService.pongFor(message));
                    }
                    return sendOrCloseOnFailure(connection, new WsMessage<>(message.getId(), "echo", message.getTraceId(), Instant.now(), message.getPayload()));
                })
                .onErrorResume(IOException.class, ex -> sendOrCloseOnFailure(connection, WsMessage.of("error.bad-message", Collections.singletonMap("message", "Invalid WebSocket message"))));
    }

    private Mono<Void> sendOrCloseOnFailure(SessionConnection connection, WsMessage<?> message) {
        SendOutcome outcome = connection.trySend(message);
        if (outcome == SendOutcome.OVERFLOW) {
            return connection.close("outbound buffer overflow");
        }
        return Mono.empty();
    }

    private WsMessage<JsonNode> parse(String text) throws IOException {
        return objectMapper.readValue(text, messageType);
    }

    private String serialize(WsMessage<?> message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize WebSocket message", ex);
        }
    }

    private Mono<Void> heartbeatTimeout(SessionConnection connection) {
        if (!properties.getHeartbeat().isEnabled()) {
            return Mono.never();
        }
        return Flux.interval(properties.getHeartbeat().getCheckInterval())
                .filter(tick -> heartbeatService.isTimedOut(connection, Instant.now()))
                .next()
                .flatMap(tick -> connection.close("heartbeat timeout"));
    }
}
