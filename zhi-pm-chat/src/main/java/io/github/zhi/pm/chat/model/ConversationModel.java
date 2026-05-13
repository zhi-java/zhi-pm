package io.github.zhi.pm.chat.model;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationModel {
    private final String conversationId;
    private final String conversationType;
    private final Set<String> members = ConcurrentHashMap.newKeySet();
    private final Instant createdAt;

    public ConversationModel(String conversationId, String conversationType) {
        this.conversationId = conversationId;
        this.conversationType = conversationType;
        this.createdAt = Instant.now();
    }

    public String getConversationId() { return conversationId; }
    public String getConversationType() { return conversationType; }
    public Set<String> getMembers() { return members; }
    public Instant getCreatedAt() { return createdAt; }

    public void addMember(String userId) { members.add(userId); }
    public void removeMember(String userId) { members.remove(userId); }
    public boolean isMember(String userId) { return members.contains(userId); }
}
