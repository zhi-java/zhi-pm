package io.github.zhi.pm.chat.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.chat.model.ChatMessageModel;
import io.github.zhi.pm.chat.model.ConversationModel;
import io.github.zhi.pm.chat.model.DeliveryRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

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
    public ConversationModel getOrCreateConversation(String conversationId, String type) {
        Map<Object, Object> entries = redis.opsForHash().entries(convKey(conversationId))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue).block();
        if (entries != null && !entries.isEmpty()) {
            ConversationModel model = new ConversationModel(conversationId, type);
            String membersStr = (String) entries.get("members");
            if (membersStr != null && !membersStr.isBlank()) {
                for (String m : membersStr.split(",")) {
                    if (!m.isBlank()) model.addMember(m);
                }
            }
            return model;
        }
        ConversationModel model = new ConversationModel(conversationId, type);
        Map<String, String> fields = Map.of("type", type, "members", "");
        redis.opsForHash().putAll(convKey(conversationId), fields).block();
        redis.opsForSet().add(convIdsKey(), conversationId).block();
        return model;
    }

    @Override
    public ConversationModel getConversation(String conversationId) {
        Map<Object, Object> entries = redis.opsForHash().entries(convKey(conversationId))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue).block();
        if (entries == null || entries.isEmpty()) return null;
        ConversationModel model = new ConversationModel(conversationId, (String) entries.getOrDefault("type", "single"));
        String membersStr = (String) entries.get("members");
        if (membersStr != null && !membersStr.isBlank()) {
            for (String m : membersStr.split(",")) {
                if (!m.isBlank()) model.addMember(m);
            }
        }
        return model;
    }

    private void updateConversationMembers(ConversationModel conversation) {
        String members = String.join(",", conversation.getMembers());
        redis.opsForHash().put(convKey(conversation.getConversationId()), "members", members).block();
    }

    // --- Messages ---

    @Override
    public void saveMessage(ChatMessageModel message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            String key = messagesKey(message.conversationId());
            redis.opsForList().leftPush(key, json).block();
            redis.opsForList().trim(key, 0, maxHistoryPerConversation - 1).block();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    @Override
    public List<ChatMessageModel> getHistory(String conversationId, int limit) {
        List<String> jsonList = redis.opsForList().range(messagesKey(conversationId), 0, limit - 1).collectList().block();
        if (jsonList == null || jsonList.isEmpty()) return Collections.emptyList();
        List<ChatMessageModel> result = new ArrayList<>();
        for (String json : jsonList) {
            try {
                result.add(objectMapper.readValue(json, ChatMessageModel.class));
            } catch (JsonProcessingException e) {
                // skip corrupted entry
            }
        }
        return result;
    }

    // --- Delivery ---

    @Override
    public void saveDelivery(DeliveryRecord record) {
        try {
            String json = objectMapper.writeValueAsString(record);
            redis.opsForValue().set(deliveryKey(record.messageId(), record.receiverId()), json).block();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize delivery record", e);
        }
    }

    @Override
    public DeliveryRecord getDelivery(String messageId, String receiverId) {
        String json = redis.opsForValue().get(deliveryKey(messageId, receiverId)).block();
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, DeliveryRecord.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public void updateDelivery(DeliveryRecord record) {
        saveDelivery(record);
    }

    // --- Unread ---

    @Override
    public void incrementUnread(String conversationId, String userId) {
        redis.opsForValue().increment(unreadKey(conversationId, userId)).block();
    }

    @Override
    public long getUnreadCount(String conversationId, String userId) {
        String val = redis.opsForValue().get(unreadKey(conversationId, userId)).block();
        if (val == null) return 0;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void resetUnread(String conversationId, String userId) {
        redis.delete(unreadKey(conversationId, userId)).block();
    }

    // --- Conversation IDs ---

    @Override
    public Set<String> getConversationIds() {
        Set<String> ids = redis.opsForSet().members(convIdsKey()).collect(Collectors.toSet()).block();
        return ids == null ? Set.of() : Set.copyOf(ids);
    }

    // --- Offline messages ---

    @Override
    public void addOfflineMessage(String userId, ChatMessageModel message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redis.opsForList().leftPush(offlineKey(userId), json).block();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize offline message", e);
        }
    }

    @Override
    public List<ChatMessageModel> drainOfflineMessages(String userId, int limit) {
        String key = offlineKey(userId);
        List<ChatMessageModel> result = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String json = redis.opsForList().rightPop(key).block();
            if (json == null) break;
            try {
                result.add(objectMapper.readValue(json, ChatMessageModel.class));
            } catch (JsonProcessingException e) {
                // skip corrupted entry
            }
        }
        return result;
    }
}
