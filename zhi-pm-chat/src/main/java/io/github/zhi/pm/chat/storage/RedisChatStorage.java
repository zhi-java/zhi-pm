package io.github.zhi.pm.chat.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.chat.model.ChatMessageModel;
import io.github.zhi.pm.chat.model.ConversationModel;
import io.github.zhi.pm.chat.model.DeliveryRecord;
import java.util.Map;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RedisChatStorage implements ChatStorage {
    private final ReactiveRedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;
    private final String prefix;
    private final int maxHistoryPerConversation;

    public RedisChatStorage(ReactiveRedisTemplate<String, String> redis, ObjectMapper objectMapper,
                            String prefix, int maxHistoryPerConversation) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.prefix = prefix.endsWith(":") ? prefix : prefix + ":";
        this.maxHistoryPerConversation = maxHistoryPerConversation;
    }

    // --- Key helpers ---

    private String convKey(String convId) { return prefix + "chat:conv:" + convId; }
    private String messagesKey(String convId) { return prefix + "chat:conv:" + convId + ":messages"; }
    private String unreadKey(String convId, String userId) { return prefix + "chat:conv:" + convId + ":unread:" + userId; }
    private String deliveryKey(String msgId, String userId) { return prefix + "chat:msg:" + msgId + ":delivery:" + userId; }
    private String offlineKey(String userId) { return prefix + "chat:user:" + userId + ":offline"; }
    private String convIdsKey() { return prefix + "chat:conversations"; }

    // --- Conversation ---

    @Override
    public Mono<ConversationModel> getOrCreateConversation(String conversationId, String type) {
        return redis.opsForHash().entries(convKey(conversationId))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(entries -> {
                    if (entries != null && !entries.isEmpty()) {
                        ConversationModel model = new ConversationModel(conversationId, type);
                        String membersStr = (String) entries.get("members");
                        if (membersStr != null && !membersStr.isBlank()) {
                            for (String m : membersStr.split(",")) {
                                if (!m.isBlank()) model.addMember(m);
                            }
                        }
                        return Mono.just(model);
                    }
                    ConversationModel model = new ConversationModel(conversationId, type);
                    Map<String, String> fields = Map.of("type", type, "members", "");
                    return redis.opsForHash().putAll(convKey(conversationId), fields)
                            .then(redis.opsForSet().add(convIdsKey(), conversationId))
                            .thenReturn(model);
                });
    }

    @Override
    public Mono<ConversationModel> getConversation(String conversationId) {
        return redis.opsForHash().entries(convKey(conversationId))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(entries -> {
                    if (entries == null || entries.isEmpty()) return Mono.empty();
                    ConversationModel model = new ConversationModel(conversationId,
                            (String) entries.getOrDefault("type", "single"));
                    String membersStr = (String) entries.get("members");
                    if (membersStr != null && !membersStr.isBlank()) {
                        for (String m : membersStr.split(",")) {
                            if (!m.isBlank()) model.addMember(m);
                        }
                    }
                    return Mono.just(model);
                });
    }

    @Override
    public Mono<Void> addMemberToConversation(String conversationId, String userId) {
        return getConversation(conversationId)
                .flatMap(conversation -> {
                    conversation.addMember(userId);
                    String members = String.join(",", conversation.getMembers());
                    return redis.opsForHash().put(convKey(conversationId), "members", members).then();
                })
                .switchIfEmpty(Mono.empty());
    }

    // --- Messages ---

    @Override
    public Mono<Void> saveMessage(ChatMessageModel message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            String key = messagesKey(message.conversationId());
            return redis.opsForList().leftPush(key, json)
                    .then(redis.opsForList().trim(key, 0, maxHistoryPerConversation - 1))
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to serialize message", e));
        }
    }

    @Override
    public Flux<ChatMessageModel> getHistory(String conversationId, int limit) {
        return redis.opsForList().range(messagesKey(conversationId), 0, limit - 1)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, ChatMessageModel.class));
                    } catch (JsonProcessingException e) {
                        return Mono.empty(); // skip corrupted entry
                    }
                });
    }

    // --- Delivery ---

    @Override
    public Mono<Void> saveDelivery(DeliveryRecord record) {
        try {
            String json = objectMapper.writeValueAsString(record);
            return redis.opsForValue().set(deliveryKey(record.messageId(), record.receiverId()), json).then();
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to serialize delivery record", e));
        }
    }

    @Override
    public Mono<DeliveryRecord> getDelivery(String messageId, String receiverId) {
        return redis.opsForValue().get(deliveryKey(messageId, receiverId))
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, DeliveryRecord.class));
                    } catch (JsonProcessingException e) {
                        return Mono.empty();
                    }
                });
    }

    @Override
    public Mono<Void> updateDelivery(DeliveryRecord record) {
        return saveDelivery(record);
    }

    // --- Unread ---

    @Override
    public Mono<Void> incrementUnread(String conversationId, String userId) {
        return redis.opsForValue().increment(unreadKey(conversationId, userId)).then();
    }

    @Override
    public Mono<Long> getUnreadCount(String conversationId, String userId) {
        return redis.opsForValue().get(unreadKey(conversationId, userId))
                .map(val -> {
                    try {
                        return Long.parseLong(val);
                    } catch (NumberFormatException e) {
                        return 0L;
                    }
                })
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Void> resetUnread(String conversationId, String userId) {
        return redis.delete(unreadKey(conversationId, userId)).then();
    }

    // --- Conversation IDs ---

    @Override
    public Flux<String> getConversationIds() {
        return redis.opsForSet().members(convIdsKey());
    }

    // --- Offline messages ---

    @Override
    public Mono<Void> addOfflineMessage(String userId, ChatMessageModel message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            return redis.opsForList().leftPush(offlineKey(userId), json).then();
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to serialize offline message", e));
        }
    }

    @Override
    public Flux<ChatMessageModel> drainOfflineMessages(String userId, int limit) {
        String key = offlineKey(userId);
        return Flux.range(0, limit)
                .concatMap(i -> redis.opsForList().rightPop(key))
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, ChatMessageModel.class));
                    } catch (JsonProcessingException e) {
                        return Mono.empty(); // skip corrupted entry
                    }
                });
    }
}
