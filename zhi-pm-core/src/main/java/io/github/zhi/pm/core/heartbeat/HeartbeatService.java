package io.github.zhi.pm.core.heartbeat;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.session.SessionConnection;
import java.time.Duration;
import java.time.Instant;

public final class HeartbeatService {
    public static final String PING_TYPE = "heartbeat.ping";
    public static final String PONG_TYPE = "heartbeat.pong";
    private final Duration timeout;

    public HeartbeatService(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean isPing(WsMessage<?> message) {
        return message != null && PING_TYPE.equals(message.getType());
    }

    public WsMessage<?> pongFor(WsMessage<?> ping) {
        return new WsMessage<>(ping.getId(), PONG_TYPE, ping.getTraceId(), Instant.now(), ping.getPayload());
    }

    public void recordHeartbeat(SessionConnection connection) {
        connection.markHeartbeat(Instant.now());
    }

    public boolean isTimedOut(SessionConnection connection, Instant now) {
        return timeout != null && timeout.toMillis() > 0 && connection.lastHeartbeatAt().plus(timeout).isBefore(now);
    }
}
