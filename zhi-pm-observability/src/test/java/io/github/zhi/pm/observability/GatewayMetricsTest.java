package io.github.zhi.pm.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatewayMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private GatewayMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new GatewayMetrics(meterRegistry);
    }

    @Test
    void recordsConnection() {
        metrics.recordConnection();
        assertEquals(1, metrics.getActiveConnections());
        assertEquals(1.0, meterRegistry.counter("ws.connections.total").count());
    }

    @Test
    void recordsDisconnection() {
        metrics.recordConnection();
        metrics.recordConnection();
        metrics.recordDisconnection();
        assertEquals(1, metrics.getActiveConnections());
        assertEquals(1.0, meterRegistry.counter("ws.disconnections.total").count());
    }

    @Test
    void recordsInboundMessage() {
        metrics.recordInboundMessage();
        metrics.recordInboundMessage();
        assertEquals(2.0, meterRegistry.counter("ws.messages.inbound.total").count());
    }

    @Test
    void recordsOutboundMessage() {
        metrics.recordOutboundMessage();
        assertEquals(1.0, meterRegistry.counter("ws.messages.outbound.total").count());
    }

    @Test
    void recordsMessageFailed() {
        metrics.recordMessageFailed();
        assertEquals(1.0, meterRegistry.counter("ws.messages.failed.total").count());
    }

    @Test
    void recordsMessageDropped() {
        metrics.recordMessageDropped();
        assertEquals(1.0, meterRegistry.counter("ws.messages.dropped.total").count());
    }

    @Test
    void recordsHeartbeatTimeout() {
        metrics.recordHeartbeatTimeout();
        assertEquals(1.0, meterRegistry.counter("ws.heartbeat.timeout.total").count());
    }

    @Test
    void recordsPushSuccess() {
        metrics.recordPushSuccess();
        metrics.recordPushSuccess();
        assertEquals(2.0, meterRegistry.counter("ws.push.success.total").count());
    }

    @Test
    void recordsPushFailure() {
        metrics.recordPushFailure();
        assertEquals(1.0, meterRegistry.counter("ws.push.failure.total").count());
    }

    @Test
    void calculatesPushSuccessRate() {
        metrics.recordPushSuccess();
        metrics.recordPushSuccess();
        metrics.recordPushFailure();
        assertEquals(2.0 / 3.0, metrics.getPushSuccessRate(), 0.001);
    }

    @Test
    void pushSuccessRateDefaultsToOneWhenNoPushes() {
        assertEquals(1.0, metrics.getPushSuccessRate());
    }

    @Test
    void updatesOnlineUsers() {
        metrics.updateOnlineUsers(42);
        assertEquals(42, metrics.getOnlineUsers());
    }

    @Test
    void updatesActiveRooms() {
        metrics.updateActiveRooms(5);
        assertEquals(5, metrics.getActiveRooms());
    }

    @Test
    void recordsPushLatency() {
        metrics.recordPushLatency(100);
        assertEquals(1, meterRegistry.timer("ws.push.latency").count());
    }
}
