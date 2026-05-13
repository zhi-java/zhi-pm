package io.github.zhi.pm.admin;

import io.github.zhi.pm.core.registry.InMemoryConnectionRegistry;
import io.github.zhi.pm.core.send.LocalMessageSender;
import io.github.zhi.pm.core.session.DefaultSessionConnection;
import io.github.zhi.pm.observability.GatewayMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {

    private InMemoryConnectionRegistry registry;
    private LocalMessageSender sender;
    private GatewayMetrics metrics;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        registry = new InMemoryConnectionRegistry();
        sender = new LocalMessageSender(registry);
        metrics = new GatewayMetrics(new SimpleMeterRegistry());
        adminService = new AdminService(registry, sender, metrics);
    }

    @Test
    void listConnectionsReturnsActiveConnections() {
        DefaultSessionConnection alice = new DefaultSessionConnection("s1", "alice", Collections.emptyMap(), 64, reason -> Mono.empty());
        DefaultSessionConnection bob = new DefaultSessionConnection("s2", "bob", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(alice).then(registry.register(bob))).verifyComplete();

        StepVerifier.create(adminService.listConnections().collectList())
                .assertNext(list -> {
                    assertEquals(2, list.size());
                    assertTrue(list.stream().anyMatch(m -> "alice".equals(m.get("userId"))));
                    assertTrue(list.stream().anyMatch(m -> "bob".equals(m.get("userId"))));
                })
                .verifyComplete();
    }

    @Test
    void getConnectionReturnsDetails() {
        DefaultSessionConnection alice = new DefaultSessionConnection("s1", "alice", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(alice)).verifyComplete();

        StepVerifier.create(adminService.getConnection("s1"))
                .assertNext(info -> {
                    assertEquals("s1", info.get("sessionId"));
                    assertEquals("alice", info.get("userId"));
                })
                .verifyComplete();
    }

    @Test
    void getConnectionReturnsEmptyForMissing() {
        StepVerifier.create(adminService.getConnection("nonexistent"))
                .verifyComplete();
    }

    @Test
    void kickConnectionClosesSession() {
        DefaultSessionConnection alice = new DefaultSessionConnection("s1", "alice", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(alice)).verifyComplete();

        StepVerifier.create(adminService.kickConnection("s1"))
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    @Test
    void kickConnectionReturnsFalseForMissing() {
        StepVerifier.create(adminService.kickConnection("nonexistent"))
                .assertNext(success -> assertFalse(success))
                .verifyComplete();
    }

    @Test
    void broadcastMessageSendsToAll() {
        DefaultSessionConnection alice = new DefaultSessionConnection("s1", "alice", Collections.emptyMap(), 64, reason -> Mono.empty());
        DefaultSessionConnection bob = new DefaultSessionConnection("s2", "bob", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(alice).then(registry.register(bob))).verifyComplete();

        StepVerifier.create(adminService.broadcastMessage("admin.test", Map.of("key", "value")))
                .assertNext(result -> {
                    assertEquals(2, ((Number) result.get("sent")).intValue());
                    assertEquals("admin.test", result.get("type"));
                })
                .verifyComplete();
    }

    @Test
    void getStatsReturnsCurrentMetrics() {
        metrics.recordConnection();
        metrics.recordConnection();
        metrics.recordPushSuccess();
        metrics.recordPushFailure();

        StepVerifier.create(adminService.getStats())
                .assertNext(stats -> {
                    assertEquals(2, ((Number) stats.get("activeConnections")).intValue());
                    assertEquals(0, ((Number) stats.get("onlineUsers")).intValue());
                    assertEquals(0, ((Number) stats.get("activeRooms")).intValue());
                    double rate = (Double) stats.get("pushSuccessRate");
                    assertEquals(0.5, rate, 0.001);
                })
                .verifyComplete();
    }
}
