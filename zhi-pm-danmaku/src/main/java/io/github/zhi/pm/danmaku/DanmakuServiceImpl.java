package io.github.zhi.pm.danmaku;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.zhi.pm.core.danmaku.DanmakuService;
import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.core.session.SendOutcome;
import io.github.zhi.pm.core.session.SessionConnection;
import io.github.zhi.pm.danmaku.filter.ContentFilter;
import io.github.zhi.pm.danmaku.limiter.TokenBucketRateLimiter;
import io.github.zhi.pm.danmaku.mute.InMemoryMuteService;
import java.time.Instant;
import java.util.Map;
import reactor.core.publisher.Mono;

public class DanmakuServiceImpl implements DanmakuService {
    private final MessageSender sender;
    private final ConnectionRegistry registry;
    private final ContentFilter contentFilter;
    private final TokenBucketRateLimiter userLimiter;
    private final TokenBucketRateLimiter roomLimiter;
    private final InMemoryMuteService muteService;
    private final int maxContentLength;

    public DanmakuServiceImpl(MessageSender sender, ConnectionRegistry registry,
                              ContentFilter contentFilter, InMemoryMuteService muteService,
                              int maxContentLength, int maxUserPerSecond, int maxRoomPerSecond) {
        this.sender = sender;
        this.registry = registry;
        this.contentFilter = contentFilter;
        this.muteService = muteService;
        this.maxContentLength = maxContentLength;
        this.userLimiter = new TokenBucketRateLimiter(maxUserPerSecond);
        this.roomLimiter = new TokenBucketRateLimiter(maxRoomPerSecond);
    }

    @Override
    public boolean isDanmaku(WsMessage<?> message) {
        return message != null && "danmaku.send".equals(message.getType());
    }

    @Override
    public Mono<Void> processDanmaku(SessionConnection connection, WsMessage<?> message) {
        String roomId = extractRoomId(message);
        if (roomId == null || roomId.isBlank()) {
            return sendError(connection, message, "roomId is required");
        }
        String content = extractContent(message);
        if (content == null || content.isBlank()) {
            return sendError(connection, message, "content is required");
        }
        if (content.length() > maxContentLength) {
            return sendError(connection, message, "content too long");
        }
        if (contentFilter.isBlocked(content)) {
            return sendError(connection, message, "content blocked");
        }
        if (muteService.isMuted(roomId, connection.userId())) {
            return sendError(connection, message, "you are muted in this room");
        }
        String userKey = connection.userId() + ":" + roomId;
        if (!userLimiter.tryAcquire(userKey)) {
            return sendError(connection, message, "rate limit exceeded");
        }
        if (!roomLimiter.tryAcquire(roomId)) {
            return sendError(connection, message, "room rate limit exceeded");
        }
        WsMessage<?> danmakuMsg = new WsMessage<>(message.getId(), "danmaku.message",
                message.getTraceId(), null, connection.userId(), null, roomId,
                Instant.now(), Map.of("content", content));
        return sender.sendToRoom(roomId, danmakuMsg).then();
    }

    public InMemoryMuteService getMuteService() {
        return muteService;
    }

    private String extractRoomId(WsMessage<?> message) {
        if (message.getRoomId() != null && !message.getRoomId().isBlank()) {
            return message.getRoomId();
        }
        Object payload = message.getPayload();
        if (payload instanceof Map<?, ?> map) {
            Object rid = map.get("roomId");
            return rid instanceof String s ? s : null;
        }
        if (payload instanceof JsonNode node && node.has("roomId")) {
            return node.get("roomId").asText(null);
        }
        return null;
    }

    private String extractContent(WsMessage<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof Map<?, ?> map) {
            Object c = map.get("content");
            return c instanceof String s ? s : null;
        }
        if (payload instanceof JsonNode node && node.has("content")) {
            return node.get("content").asText(null);
        }
        return null;
    }

    private Mono<Void> sendError(SessionConnection connection, WsMessage<?> message, String error) {
        SendOutcome outcome = connection.trySend(WsMessage.of("danmaku.error", Map.of("message", error)));
        if (outcome == SendOutcome.OVERFLOW) {
            return connection.close("outbound buffer overflow");
        }
        return Mono.empty();
    }
}
