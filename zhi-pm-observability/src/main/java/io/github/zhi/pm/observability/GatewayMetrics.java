package io.github.zhi.pm.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central metrics collector for the WebSocket gateway.
 * Tracks connections, messages, heartbeat timeouts, and push outcomes.
 */
public class GatewayMetrics {
    private final MeterRegistry registry;

    // Counters
    private final Counter connectionsTotal;
    private final Counter disconnectionsTotal;
    private final Counter inboundMessagesTotal;
    private final Counter outboundMessagesTotal;
    private final Counter messagesFailedTotal;
    private final Counter messagesDroppedTotal;
    private final Counter heartbeatTimeoutsTotal;
    private final Counter pushSuccessTotal;
    private final Counter pushFailureTotal;

    // Gauges (using AtomicLong for current values)
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong onlineUsers = new AtomicLong(0);
    private final AtomicLong activeRooms = new AtomicLong(0);

    // Timers
    private final Timer pushLatency;

    public GatewayMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.connectionsTotal = Counter.builder("ws.connections.total")
                .description("Total number of WebSocket connections established")
                .register(registry);

        this.disconnectionsTotal = Counter.builder("ws.disconnections.total")
                .description("Total number of WebSocket disconnections")
                .register(registry);

        this.inboundMessagesTotal = Counter.builder("ws.messages.inbound.total")
                .description("Total number of inbound WebSocket messages")
                .register(registry);

        this.outboundMessagesTotal = Counter.builder("ws.messages.outbound.total")
                .description("Total number of outbound WebSocket messages")
                .register(registry);

        this.messagesFailedTotal = Counter.builder("ws.messages.failed.total")
                .description("Total number of failed message deliveries")
                .register(registry);

        this.messagesDroppedTotal = Counter.builder("ws.messages.dropped.total")
                .description("Total number of dropped messages")
                .register(registry);

        this.heartbeatTimeoutsTotal = Counter.builder("ws.heartbeat.timeout.total")
                .description("Total number of heartbeat timeouts")
                .register(registry);

        this.pushSuccessTotal = Counter.builder("ws.push.success.total")
                .description("Total number of successful push operations")
                .register(registry);

        this.pushFailureTotal = Counter.builder("ws.push.failure.total")
                .description("Total number of failed push operations")
                .register(registry);

        this.pushLatency = Timer.builder("ws.push.latency")
                .description("Latency of push operations")
                .register(registry);

        // Register gauges
        registry.gauge("ws.connections.active", activeConnections, AtomicLong::get);
        registry.gauge("ws.users.online", onlineUsers, AtomicLong::get);
        registry.gauge("ws.rooms.active", activeRooms, AtomicLong::get);
    }

    public void recordConnection() {
        connectionsTotal.increment();
        activeConnections.incrementAndGet();
    }

    public void recordDisconnection() {
        disconnectionsTotal.increment();
        activeConnections.decrementAndGet();
    }

    public void recordInboundMessage() {
        inboundMessagesTotal.increment();
    }

    public void recordOutboundMessage() {
        outboundMessagesTotal.increment();
    }

    public void recordMessageFailed() {
        messagesFailedTotal.increment();
    }

    public void recordMessageDropped() {
        messagesDroppedTotal.increment();
    }

    public void recordHeartbeatTimeout() {
        heartbeatTimeoutsTotal.increment();
    }

    public void recordPushSuccess() {
        pushSuccessTotal.increment();
    }

    public void recordPushFailure() {
        pushFailureTotal.increment();
    }

    public void recordPushLatency(long durationMs) {
        pushLatency.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void updateOnlineUsers(long count) {
        onlineUsers.set(count);
    }

    public void updateActiveRooms(long count) {
        activeRooms.set(count);
    }

    public long getActiveConnections() {
        return activeConnections.get();
    }

    public long getOnlineUsers() {
        return onlineUsers.get();
    }

    public long getActiveRooms() {
        return activeRooms.get();
    }

    public double getPushSuccessRate() {
        double success = pushSuccessTotal.count();
        double failure = pushFailureTotal.count();
        double total = success + failure;
        return total > 0 ? success / total : 1.0;
    }
}
