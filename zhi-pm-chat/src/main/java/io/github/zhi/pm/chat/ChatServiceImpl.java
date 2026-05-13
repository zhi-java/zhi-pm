package io.github.zhi.pm.chat;

import io.github.zhi.pm.core.chat.ChatService;
import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.core.session.SendOutcome;
import io.github.zhi.pm.core.session.SessionConnection;
import io.github.zhi.pm.chat.model.ChatMessageModel;
import io.github.zhi.pm.chat.model.ConversationModel;
import io.github.zhi.pm.chat.model.DeliveryRecord;
import io.github.zhi.pm.chat.storage.InMemoryChatStorage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Mono;

public class ChatServiceImpl implements ChatService {
    private final MessageSender sender;
    private final ConnectionRegistry registry;
    private final InMemoryChatStorage storage;
    private final int maxMessageLength;

    public ChatServiceImpl(MessageSender sender, ConnectionRegistry registry,
                           InMemoryChatStorage storage, int maxMessageLength) {
        this.sender = sender;
        this.registry = registry;
        this.storage = storage;
        this.maxMessageLength = maxMessageLength;
    }

    @Override
    public boolean isChatMessage(WsMessage<?> message) {
        if (message == null) return false;
        String type = message.getType();
        return "chat.send".equals(type) || "chat.ack".equals(type) || "chat.read".equals(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Void> processChatMessage(SessionConnection connection, WsMessage<?> message) {
        Object payload = message.getPayload();
        if (!(payload instanceof Map<?, ?> map)) {
            return sendError(connection, "invalid payload");
        }
        String conversationId = getString(map, "conversationId");
        if (conversationId == null || conversationId.isBlank()) {
            return sendError(connection, "conversationId is required");
        }
        String conversationType = getString(map, "conversationType");
        if (conversationType == null) conversationType = "single";
        String content = getString(map, "content");
        if (content == null || content.isBlank()) {
            return sendError(connection, "content is required");
        }
        if (content.length() > maxMessageLength) {
            return sendError(connection, "content too long");
        }
        String contentType = getString(map, "contentType");
        if (contentType == null) contentType = "text";
        String clientMessageId = getString(map, "clientMessageId");

        String messageId = UUID.randomUUID().toString().replace("-", "");
        ChatMessageModel msgModel = ChatMessageModel.create(messageId, clientMessageId,
                conversationId, conversationType, connection.userId(), contentType, content);

        ConversationModel conversation = storage.getOrCreateConversation(conversationId, conversationType);
        conversation.addMember(connection.userId());

        storage.saveMessage(msgModel);

        WsMessage<?> chatMsg = new WsMessage<>(messageId, "chat.message", message.getTraceId(),
                null, connection.userId(), null, conversationId, Instant.now(),
                Map.of("messageId", messageId, "conversationId", conversationId,
                        "conversationType", conversationType, "contentType", contentType,
                        "content", content, "senderId", connection.userId()));

        for (String memberId : conversation.getMembers()) {
            if (!memberId.equals(connection.userId())) {
                DeliveryRecord record = DeliveryRecord.pending(messageId, memberId);
                storage.saveDelivery(record);
                storage.incrementUnread(conversationId, memberId);
            }
        }

        if ("single".equals(conversationType)) {
            return sender.sendToUsers(List.copyOf(conversation.getMembers()), chatMsg).then();
        } else {
            return sender.sendToRoom(conversationId, chatMsg).then();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Void> processAck(SessionConnection connection, WsMessage<?> message) {
        Object payload = message.getPayload();
        if (!(payload instanceof Map<?, ?> map)) {
            return sendError(connection, "invalid payload");
        }
        String messageId = getString(map, "messageId");
        if (messageId == null) {
            return sendError(connection, "messageId is required");
        }
        DeliveryRecord record = storage.getDelivery(messageId, connection.userId());
        if (record != null) {
            storage.updateDelivery(record.delivered());
        }
        WsMessage<?> ackMsg = new WsMessage<>(null, "chat.ack", message.getTraceId(),
                null, null, connection.userId(), null, Instant.now(),
                Map.of("messageId", messageId, "status", "delivered"));
        SendOutcome outcome = connection.trySend(ackMsg);
        if (outcome == SendOutcome.OVERFLOW) {
            return connection.close("outbound buffer overflow");
        }
        return Mono.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Void> processReadReceipt(SessionConnection connection, WsMessage<?> message) {
        Object payload = message.getPayload();
        if (!(payload instanceof Map<?, ?> map)) {
            return sendError(connection, "invalid payload");
        }
        String conversationId = getString(map, "conversationId");
        if (conversationId == null) {
            return sendError(connection, "conversationId is required");
        }
        storage.resetUnread(conversationId, connection.userId());
        WsMessage<?> readMsg = new WsMessage<>(null, "chat.read", message.getTraceId(),
                null, connection.userId(), null, conversationId, Instant.now(),
                Map.of("conversationId", conversationId, "userId", connection.userId()));
        return sender.sendToRoom(conversationId, readMsg).then();
    }

    @Override
    public Mono<List<Map<String, Object>>> getHistory(String conversationId, int limit) {
        List<ChatMessageModel> messages = storage.getHistory(conversationId, limit);
        List<Map<String, Object>> result = messages.stream()
                .map(m -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("messageId", m.messageId());
                    map.put("conversationId", m.conversationId());
                    map.put("senderId", m.senderId());
                    map.put("contentType", m.contentType());
                    map.put("content", m.content());
                    map.put("status", m.status());
                    map.put("createdAt", m.createdAt().toString());
                    return map;
                })
                .toList();
        return Mono.just(result);
    }

    @Override
    public Mono<Long> getUnreadCount(String conversationId, String userId) {
        return Mono.just(storage.getUnreadCount(conversationId, userId));
    }

    public InMemoryChatStorage getStorage() {
        return storage;
    }

    private String getString(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v instanceof String s ? s : null;
    }

    private Mono<Void> sendError(SessionConnection connection, String error) {
        SendOutcome outcome = connection.trySend(WsMessage.of("chat.error", Map.of("message", error)));
        if (outcome == SendOutcome.OVERFLOW) {
            return connection.close("outbound buffer overflow");
        }
        return Mono.empty();
    }
}
