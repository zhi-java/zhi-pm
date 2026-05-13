package io.github.zhi.pm.chat.model;

import java.time.Instant;

public record ChatMessageModel(
        String messageId,
        String clientMessageId,
        String conversationId,
        String conversationType,
        String senderId,
        String contentType,
        String content,
        String status,
        Instant createdAt
) {
    public static ChatMessageModel create(String messageId, String clientMessageId, String conversationId,
                                           String conversationType, String senderId, String contentType, String content) {
        return new ChatMessageModel(messageId, clientMessageId, conversationId, conversationType,
                senderId, contentType, content, "sent", Instant.now());
    }
}
