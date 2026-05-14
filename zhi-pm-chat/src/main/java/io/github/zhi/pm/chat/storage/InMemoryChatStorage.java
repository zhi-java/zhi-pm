package io.github.zhi.pm.chat.storage;

import io.github.zhi.pm.chat.model.ChatMessageModel;
import io.github.zhi.pm.chat.model.ConversationModel;
import io.github.zhi.pm.chat.model.DeliveryRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InMemoryChatStorage implements ChatStorage {
    private final int maxHistoryPerConversation;
    private final Map<String, ConversationModel> conversations = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedDeque<ChatMessageModel>> messagesByConversation = new ConcurrentHashMap<>();
    private final Map<String, Map<String, DeliveryRecord>> deliveriesByMessage = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AtomicLong>> unreadCounts = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedDeque<ChatMessageModel>> offlineMessages = new ConcurrentHashMap<>();

    public InMemoryChatStorage(int maxHistoryPerConversation) {
        this.maxHistoryPerConversation = maxHistoryPerConversation;
    }

    @Override
    public Mono<ConversationModel> getOrCreateConversation(String conversationId, String type) {
        return Mono.just(conversations.computeIfAbsent(conversationId, id -> new ConversationModel(id, type)));
    }

    @Override
    public Mono<ConversationModel> getConversation(String conversationId) {
        return Mono.justOrEmpty(conversations.get(conversationId));
    }

    @Override
    public Mono<Void> addMemberToConversation(String conversationId, String userId) {
        ConversationModel conversation = conversations.get(conversationId);
        if (conversation != null) {
            conversation.addMember(userId);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> saveMessage(ChatMessageModel message) {
        ConcurrentLinkedDeque<ChatMessageModel> deque = messagesByConversation
                .computeIfAbsent(message.conversationId(), k -> new ConcurrentLinkedDeque<>());
        deque.addLast(message);
        while (deque.size() > maxHistoryPerConversation) {
            deque.pollFirst();
        }
        return Mono.empty();
    }

    @Override
    public Flux<ChatMessageModel> getHistory(String conversationId, int limit) {
        ConcurrentLinkedDeque<ChatMessageModel> deque = messagesByConversation.get(conversationId);
        if (deque == null) return Flux.empty();
        List<ChatMessageModel> list = new ArrayList<>(deque);
        int from = Math.max(0, list.size() - limit);
        return Flux.fromIterable(list.subList(from, list.size()));
    }

    @Override
    public Mono<Void> saveDelivery(DeliveryRecord record) {
        deliveriesByMessage
                .computeIfAbsent(record.messageId(), k -> new ConcurrentHashMap<>())
                .put(record.receiverId(), record);
        return Mono.empty();
    }

    @Override
    public Mono<DeliveryRecord> getDelivery(String messageId, String receiverId) {
        Map<String, DeliveryRecord> map = deliveriesByMessage.get(messageId);
        DeliveryRecord record = map == null ? null : map.get(receiverId);
        return Mono.justOrEmpty(record);
    }

    @Override
    public Mono<Void> updateDelivery(DeliveryRecord record) {
        Map<String, DeliveryRecord> map = deliveriesByMessage.get(record.messageId());
        if (map != null) {
            map.put(record.receiverId(), record);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> incrementUnread(String conversationId, String userId) {
        unreadCounts.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> new AtomicLong(0))
                .incrementAndGet();
        return Mono.empty();
    }

    @Override
    public Mono<Long> getUnreadCount(String conversationId, String userId) {
        Map<String, AtomicLong> map = unreadCounts.get(conversationId);
        if (map == null) return Mono.just(0L);
        AtomicLong count = map.get(userId);
        return Mono.just(count == null ? 0L : count.get());
    }

    @Override
    public Mono<Void> resetUnread(String conversationId, String userId) {
        Map<String, AtomicLong> map = unreadCounts.get(conversationId);
        if (map != null) {
            AtomicLong count = map.get(userId);
            if (count != null) count.set(0);
        }
        return Mono.empty();
    }

    @Override
    public Flux<String> getConversationIds() {
        return Flux.fromIterable(conversations.keySet());
    }

    @Override
    public Mono<Void> addOfflineMessage(String userId, ChatMessageModel message) {
        offlineMessages.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>()).addLast(message);
        return Mono.empty();
    }

    @Override
    public Flux<ChatMessageModel> drainOfflineMessages(String userId, int limit) {
        ConcurrentLinkedDeque<ChatMessageModel> deque = offlineMessages.get(userId);
        if (deque == null || deque.isEmpty()) return Flux.empty();
        List<ChatMessageModel> result = new ArrayList<>();
        for (int i = 0; i < limit && !deque.isEmpty(); i++) {
            ChatMessageModel msg = deque.pollFirst();
            if (msg != null) result.add(msg);
        }
        if (deque.isEmpty()) offlineMessages.remove(userId);
        return Flux.fromIterable(result);
    }
}
