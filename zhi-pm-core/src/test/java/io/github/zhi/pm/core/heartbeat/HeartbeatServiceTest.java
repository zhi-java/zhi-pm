package io.github.zhi.pm.core.heartbeat;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.session.DefaultSessionConnection;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartbeatServiceTest {
    @Test
    void detectsPingAndTimeout() {
        HeartbeatService service = new HeartbeatService(Duration.ofSeconds(5));
        WsMessage<?> ping = WsMessage.of("heartbeat.ping", null);
        assertTrue(service.isPing(ping));
        assertEquals("heartbeat.pong", service.pongFor(ping).getType());
        DefaultSessionConnection connection = new DefaultSessionConnection("s1", "u1", java.util.Collections.emptyMap(), 2, reason -> Mono.empty());
        connection.markHeartbeat(Instant.now().minusSeconds(10));
        assertTrue(service.isTimedOut(connection, Instant.now()));
    }
}
