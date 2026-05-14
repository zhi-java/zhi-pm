package io.github.zhi.pm.chat.storage;

import io.github.zhi.pm.chat.model.ChatMessageModel;
import io.github.zhi.pm.chat.model.ConversationModel;
import io.github.zhi.pm.chat.model.DeliveryRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryChatStorage implements ChatStorage {
    private final int maxHistoryPerConversation;
    private final Map<String, ConversationModel> conversations = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedDeque<ChatMessageModel>> messagesByConversation = new ConcurrentHashMap<>();
    private final Map<String, Map<String, DeliveryRecord>> deliveriesByConversation = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AtomicLong>> unreadCounts = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedDeque<ChatMessageModel>> offlineMessages = new ConcurrentHashMap<>();

    public InMemoryChatStorage(int maxHistoryPerConversation) {
        this.maxHistoryPerConversation = maxHistoryPerConversation;
    }

    public ConversationModel getOrCreateConversation(String conversationId, String type) {
        return conversations.computeIfAbsent(conversationId, id -> new ConversationModel(id, type));
    }

    public ConversationModel getConversation(String conversationId) {
        return conversations.get(conversationId);
    }

    public void saveMessage(ChatMessageModel message) {
        ConcurrentLinkedDeque<ChatMessageModel> deque = messagesByConversation
                .computeIfAbsent(message.conversationId(), k -> new ConcurrentLinkedDeque<>());
        deque.addLast(message);
        while (deque.size() > maxHistoryPerConversation) {
            deque.pollFirst();
        }
    }

    public List<ChatMessageModel> getHistory(String conversationId, int limit) {
        ConcurrentLinkedDeque<ChatMessageModel> deque = messagesByConversation.get(conversationId);
        if (deque == null) return Collections.emptyList();
        List<ChatMessageModel> list = new ArrayList<>(deque);
        int from = Math.max(0, list.size() - limit);
        return list.subList(from, list.size());
    }

    public void saveDelivery(DeliveryRecord record) {
        deliveriesByConversation
                .computeIfAbsent(record.messageId(), k -> new ConcurrentHashMap<>())
                .put(record.receiverId(), record);
    }

    public DeliveryRecord getDelivery(String messageId, String receiverId) {
        Map<String, DeliveryRecord> map = deliveriesByConversation.get(messageId);
        return map == null ? null : map.get(receiverId);
    }

    public void updateDelivery(DeliveryRecord record) {
        Map<String, Map<String, DeliveryRecord>> deliveriesByMsg = deliveriesByConversation;
        Map<String, DeliveryRecord> map = deliveriesByMsg.get(record.messageId());
        if (map != null) {
            map.put(record.receiverId(), record);
        }
    }

    public void incrementUnread(String conversationId, String userId) {
        unreadCounts.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> new AtomicLong(0))
                .incrementAndGet();
    }

    public long getUnreadCount(String conversationId, String userId) {
        Map<String, AtomicLong> map = unreadCounts.get(conversationId);
        if (map == null) return 0;
        AtomicLong count = map.get(userId);
        return count == null ? 0 : count.get();
    }

    public void resetUnread(String conversationId, String userId) {
        Map<String, AtomicLong> map = unreadCounts.get(conversationId);
        if (map != null) {
            AtomicLong count = map.get(userId);
            if (count != null) count.set(0);
        }
    }

    public Set<String> getConversationIds() {
        return Set.copyOf(conversations.keySet());
    }

    @Override
    public void addOfflineMessage(String userId, ChatMessageModel message) {
        offlineMessages.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>()).addLast(message);
    }

    @Override
    public List<ChatMessageModel> drainOfflineMessages(String userId, int limit) {
        ConcurrentLinkedDeque<ChatMessageModel> deque = offlineMessages.get(userId);
        if (deque == null || deque.isEmpty()) return Collections.emptyList();
        List<ChatMessageModel> result = new ArrayList<>();
        for (int i = 0; i < limit && !deque.isEmpty(); i++) {
            ChatMessageModel msg = deque.pollFirst();
            if (msg != null) result.add(msg);
        }
        if (deque.isEmpty()) offlineMessages.remove(userId);
        return result;
    }
}
