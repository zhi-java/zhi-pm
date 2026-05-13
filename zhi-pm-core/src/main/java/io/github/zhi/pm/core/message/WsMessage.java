package io.github.zhi.pm.core.message;

import java.time.Instant;
import java.util.Objects;

public final class WsMessage<T> {
    private String id;
    private String type;
    private String traceId;
    private Instant timestamp;
    private T payload;

    public WsMessage() {
    }

    public WsMessage(String id, String type, String traceId, Instant timestamp, T payload) {
        this.id = id;
        this.type = type;
        this.traceId = traceId;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public static <T> WsMessage<T> of(String type, T payload) {
        return new WsMessage<>(null, type, null, Instant.now(), payload);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }

    public WsMessage<T> withType(String newType) {
        return new WsMessage<>(id, Objects.requireNonNull(newType, "type"), traceId, Instant.now(), payload);
    }
}
