package io.github.zhi.pm.core.broker;

import io.github.zhi.pm.core.message.WsMessage;
import java.util.Objects;

public final class BrokerMessage {
    public enum TargetType { USER, ROOM, BROADCAST }

    private final TargetType targetType;
    private final String targetId;
    private final WsMessage<?> message;
    private final String sourceInstanceId;

    public BrokerMessage(TargetType targetType, String targetId, WsMessage<?> message, String sourceInstanceId) {
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.targetId = targetId;
        this.message = Objects.requireNonNull(message, "message");
        this.sourceInstanceId = sourceInstanceId;
    }

    public static BrokerMessage forUser(String userId, WsMessage<?> message, String sourceInstanceId) {
        return new BrokerMessage(TargetType.USER, userId, message, sourceInstanceId);
    }

    public static BrokerMessage forRoom(String roomId, WsMessage<?> message, String sourceInstanceId) {
        return new BrokerMessage(TargetType.ROOM, roomId, message, sourceInstanceId);
    }

    public static BrokerMessage forBroadcast(WsMessage<?> message, String sourceInstanceId) {
        return new BrokerMessage(TargetType.BROADCAST, null, message, sourceInstanceId);
    }

    public TargetType targetType() { return targetType; }
    public String targetId() { return targetId; }
    public WsMessage<?> message() { return message; }
    public String sourceInstanceId() { return sourceInstanceId; }
}
