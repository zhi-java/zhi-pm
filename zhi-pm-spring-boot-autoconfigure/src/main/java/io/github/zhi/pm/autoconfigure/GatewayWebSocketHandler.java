package io.github.zhi.pm.autoconfigure;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.core.auth.AuthenticationRequest;
import io.github.zhi.pm.core.auth.AuthenticationResult;
import io.github.zhi.pm.core.auth.WebSocketAuthenticator;
import io.github.zhi.pm.core.danmaku.DanmakuService;
import io.github.zhi.pm.core.heartbeat.HeartbeatService;
import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.core.session.DefaultSessionConnection;
import io.github.zhi.pm.core.session.SendOutcome;
import io.github.zhi.pm.core.session.SessionConnection;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class GatewayWebSocketHandler implements WebSocketHandler {
    private final ConnectionRegistry registry;
    private final MessageSender sender;
    private final WebSocketAuthenticator authenticator;
    private final HeartbeatService heartbeatService;
    private final ObjectMapper objectMapper;
    private final RealtimeWebSocketProperties properties;
    private final JavaType messageType;
    @Nullable
    private final DanmakuService danmakuService;

    public GatewayWebSocketHandler(ConnectionRegistry registry, MessageSender sender, WebSocketAuthenticator authenticator, HeartbeatService heartbeatService, ObjectMapper objectMapper, RealtimeWebSocketProperties properties) {
        this(registry, sender, authenticator, heartbeatService, objectMapper, properties, null);
    }

    public GatewayWebSocketHandler(ConnectionRegistry registry, MessageSender sender, WebSocketAuthenticator authenticator, HeartbeatService heartbeatService, ObjectMapper objectMapper, RealtimeWebSocketProperties properties, @Nullable DanmakuService danmakuService) {
        this.registry = registry;
        this.sender = sender;
        this.authenticator = authenticator;
        this.heartbeatService = heartbeatService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.messageType = objectMapper.getTypeFactory().constructParametricType(WsMessage.class, JsonNode.class);
        this.danmakuService = danmakuService;
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
                    String type = message.getType();
                    if ("room.join".equals(type)) {
                        return handleRoomJoin(connection, message);
                    }
                    if ("room.leave".equals(type)) {
                        return handleRoomLeave(connection, message);
                    }
                    if ("room.message".equals(type)) {
                        return handleRoomMessage(connection, message);
                    }
                    if (danmakuService != null && danmakuService.isDanmaku(message)) {
                        return danmakuService.processDanmaku(connection, message);
                    }
                    return sendOrCloseOnFailure(connection, new WsMessage<>(message.getId(), "echo", message.getTraceId(), Instant.now(), message.getPayload()));
                })
                .onErrorResume(IOException.class, ex -> sendOrCloseOnFailure(connection, WsMessage.of("error.bad-message", Collections.singletonMap("message", "Invalid WebSocket message"))));
    }

    private Mono<Void> handleRoomJoin(SessionConnection connection, WsMessage<JsonNode> message) {
        String roomId = extractRoomId(message);
        if (roomId == null || roomId.isBlank()) {
            return sendOrCloseOnFailure(connection, WsMessage.of("error.room-join", Collections.singletonMap("message", "roomId is required")));
        }
        return registry.joinRoom(roomId, connection.sessionId())
                .then(sendOrCloseOnFailure(connection, new WsMessage<>(message.getId(), "room.joined", message.getTraceId(), Instant.now(), Map.of("roomId", roomId, "userId", connection.userId()))));
    }

    private Mono<Void> handleRoomLeave(SessionConnection connection, WsMessage<JsonNode> message) {
        String roomId = extractRoomId(message);
        if (roomId == null || roomId.isBlank()) {
            return registry.leaveAllRooms(connection.sessionId())
                    .then(sendOrCloseOnFailure(connection, new WsMessage<>(message.getId(), "room.left-all", message.getTraceId(), Instant.now(), Collections.emptyMap())));
        }
        return registry.leaveRoom(roomId, connection.sessionId())
                .then(sendOrCloseOnFailure(connection, new WsMessage<>(message.getId(), "room.left", message.getTraceId(), Instant.now(), Map.of("roomId", roomId))));
    }

    private Mono<Void> handleRoomMessage(SessionConnection connection, WsMessage<JsonNode> message) {
        String roomId = extractRoomId(message);
        if (roomId == null || roomId.isBlank()) {
            return sendOrCloseOnFailure(connection, WsMessage.of("error.room-message", Collections.singletonMap("message", "roomId is required")));
        }
        WsMessage<?> roomMsg = new WsMessage<>(message.getId(), "room.message", message.getTraceId(), null, connection.userId(), null, roomId, Instant.now(), message.getPayload());
        return sender.sendToRoom(roomId, roomMsg).then();
    }

    private String extractRoomId(WsMessage<JsonNode> message) {
        if (message.getRoomId() != null && !message.getRoomId().isBlank()) {
            return message.getRoomId();
        }
        JsonNode payload = message.getPayload();
        if (payload != null && payload.has("roomId")) {
            return payload.get("roomId").asText();
        }
        return null;
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
