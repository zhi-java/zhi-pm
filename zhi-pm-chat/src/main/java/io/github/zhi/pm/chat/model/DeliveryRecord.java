package io.github.zhi.pm.chat.model;

import java.time.Instant;

public record DeliveryRecord(
        String messageId,
        String receiverId,
        String deliveryStatus,
        Instant deliveredAt,
        Instant readAt
) {
    public static DeliveryRecord pending(String messageId, String receiverId) {
        return new DeliveryRecord(messageId, receiverId, "pending", null, null);
    }

    public DeliveryRecord delivered() {
        return new DeliveryRecord(messageId, receiverId, "delivered", Instant.now(), readAt);
    }

    public DeliveryRecord read() {
        return new DeliveryRecord(messageId, receiverId, "read", deliveredAt == null ? Instant.now() : deliveredAt, Instant.now());
    }
}
