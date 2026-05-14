package io.github.zhi.pm.chat.storage;

import io.github.zhi.pm.chat.model.ChatMessageModel;
import io.github.zhi.pm.chat.model.ConversationModel;
import io.github.zhi.pm.chat.model.DeliveryRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatStorage {

    Mono<ConversationModel> getOrCreateConversation(String conversationId, String type);

    Mono<ConversationModel> getConversation(String conversationId);

    Mono<Void> addMemberToConversation(String conversationId, String userId);

    Mono<Void> saveMessage(ChatMessageModel message);

    Flux<ChatMessageModel> getHistory(String conversationId, int limit);

    Mono<Void> saveDelivery(DeliveryRecord record);

    Mono<DeliveryRecord> getDelivery(String messageId, String receiverId);

    Mono<Void> updateDelivery(DeliveryRecord record);

    Mono<Void> incrementUnread(String conversationId, String userId);

    Mono<Long> getUnreadCount(String conversationId, String userId);

    Mono<Void> resetUnread(String conversationId, String userId);

    Flux<String> getConversationIds();

    // Offline message queue

    Mono<Void> addOfflineMessage(String userId, ChatMessageModel message);

    Flux<ChatMessageModel> drainOfflineMessages(String userId, int limit);
}
