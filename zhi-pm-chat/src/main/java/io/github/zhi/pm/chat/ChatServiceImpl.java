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
import io.github.zhi.pm.chat.storage.ChatStorage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ChatServiceImpl implements ChatService {
    private final MessageSender sender;
    private final ConnectionRegistry registry;
    private final ChatStorage storage;
    private final int maxMessageLength;
    private final boolean offlineMessageEnabled;

    public ChatServiceImpl(MessageSender sender, ConnectionRegistry registry,
                           ChatStorage storage, int maxMessageLength, boolean offlineMessageEnabled) {
        this.sender = sender;
        this.registry = registry;
        this.storage = storage;
        this.maxMessageLength = maxMessageLength;
        this.offlineMessageEnabled = offlineMessageEnabled;
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
        String finalConversationType = conversationType;
        String finalContentType = contentType;

        ChatMessageModel msgModel = ChatMessageModel.create(messageId, clientMessageId,
                conversationId, conversationType, connection.userId(), contentType, content);

        return storage.getOrCreateConversation(conversationId, conversationType)
                .then(storage.addMemberToConversation(conversationId, connection.userId()))
                .then(storage.saveMessage(msgModel))
                .then(storage.getConversation(conversationId))
                .flatMap(conversation -> {
                    WsMessage<?> chatMsg = new WsMessage<>(messageId, "chat.message", message.getTraceId(),
                            null, connection.userId(), null, conversationId, Instant.now(),
                            Map.of("messageId", messageId, "conversationId", conversationId,
                                    "conversationType", finalConversationType, "contentType", finalContentType,
                                    "content", content, "senderId", connection.userId()));

                    Mono<Void> deliveryAndUnread = Flux.fromIterable(conversation.getMembers())
                            .filter(uid -> !uid.equals(connection.userId()))
                            .flatMap(uid -> storage.saveDelivery(DeliveryRecord.pending(messageId, uid))
                                    .then(storage.incrementUnread(conversationId, uid)))
                            .then();

                    Mono<Void> sendMessage;
                    if ("single".equals(finalConversationType)) {
                        List<String> targets = List.copyOf(conversation.getMembers());
                        sendMessage = sender.sendToUsers(targets, chatMsg).then();
                    } else {
                        sendMessage = sender.sendToRoom(conversationId, chatMsg).then();
                    }

                    Mono<Void> offline = Mono.empty();
                    if (offlineMessageEnabled && "single".equals(finalConversationType)) {
                        List<String> targets = List.copyOf(conversation.getMembers());
                        offline = enqueueOfflineForOffline(targets, connection.userId(), msgModel);
                    }

                    return deliveryAndUnread.then(sendMessage).then(offline);
                });
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
        return storage.getDelivery(messageId, connection.userId())
                .flatMap(record -> storage.updateDelivery(record.delivered()))
                .then(sendAckMessage(connection, messageId, message.getTraceId()));
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
        return storage.resetUnread(conversationId, connection.userId())
                .then(sender.sendToRoom(conversationId, new WsMessage<>(null, "chat.read", message.getTraceId(),
                        null, connection.userId(), null, conversationId, Instant.now(),
                        Map.of("conversationId", conversationId, "userId", connection.userId())))).then();
    }

    @Override
    public Mono<List<Map<String, Object>>> getHistory(String conversationId, int limit) {
        return storage.getHistory(conversationId, limit)
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
                .collectList();
    }

    @Override
    public Mono<Long> getUnreadCount(String conversationId, String userId) {
        return storage.getUnreadCount(conversationId, userId);
    }

    @Override
    public Mono<Void> drainOfflineMessages(String userId) {
        return storage.drainOfflineMessages(userId, 100)
                .flatMap(msg -> {
                    WsMessage<?> wsMsg = new WsMessage<>(msg.messageId(), "chat.message", null,
                            null, msg.senderId(), userId, msg.conversationId(), msg.createdAt(),
                            Map.of("messageId", msg.messageId(), "conversationId", msg.conversationId(),
                                    "conversationType", msg.conversationType(), "contentType", msg.contentType(),
                                    "content", msg.content(), "senderId", msg.senderId(),
                                    "offline", true));
                    return sender.sendToUser(userId, wsMsg);
                })
                .then();
    }

    public ChatStorage getStorage() {
        return storage;
    }

    private Mono<Void> enqueueOfflineForOffline(List<String> memberIds, String senderId, ChatMessageModel message) {
        return Flux.fromIterable(memberIds)
                .filter(uid -> !uid.equals(senderId))
                .flatMap(uid -> registry.isOnline(uid).map(online -> Map.entry(uid, online)))
                .filter(entry -> !entry.getValue())
                .flatMap(entry -> storage.addOfflineMessage(entry.getKey(), message))
                .then();
    }

    private Mono<Void> sendAckMessage(SessionConnection connection, String messageId, String traceId) {
        WsMessage<?> ackMsg = new WsMessage<>(null, "chat.ack", traceId,
                null, null, connection.userId(), null, Instant.now(),
                Map.of("messageId", messageId, "status", "delivered"));
        SendOutcome outcome = connection.trySend(ackMsg);
        if (outcome == SendOutcome.OVERFLOW) {
            return connection.close("outbound buffer overflow");
        }
        return Mono.empty();
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
