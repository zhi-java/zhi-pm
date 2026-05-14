package io.github.zhi.pm.chat.storage;

import io.github.zhi.pm.chat.model.ChatMessageModel;
import io.github.zhi.pm.chat.model.ConversationModel;
import io.github.zhi.pm.chat.model.DeliveryRecord;
import java.util.List;
import java.util.Set;

public interface ChatStorage {

    ConversationModel getOrCreateConversation(String conversationId, String type);

    ConversationModel getConversation(String conversationId);

    void saveMessage(ChatMessageModel message);

    List<ChatMessageModel> getHistory(String conversationId, int limit);

    void saveDelivery(DeliveryRecord record);

    DeliveryRecord getDelivery(String messageId, String receiverId);

    void updateDelivery(DeliveryRecord record);

    void incrementUnread(String conversationId, String userId);

    long getUnreadCount(String conversationId, String userId);

    void resetUnread(String conversationId, String userId);

    Set<String> getConversationIds();

    // Offline message queue

    void addOfflineMessage(String userId, ChatMessageModel message);

    List<ChatMessageModel> drainOfflineMessages(String userId, int limit);
}
