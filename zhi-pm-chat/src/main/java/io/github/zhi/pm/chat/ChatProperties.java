package io.github.zhi.pm.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "realtime.chat")
public class ChatProperties {
    private boolean enabled = true;
    private String storageType = "memory";
    private String redisKeyPrefix = "realtime";
    private boolean ackEnabled = true;
    private boolean offlineMessageEnabled = true;
    private int maxMessageLength = 2000;
    private int maxHistoryPerConversation = 200;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public String getRedisKeyPrefix() { return redisKeyPrefix; }
    public void setRedisKeyPrefix(String redisKeyPrefix) { this.redisKeyPrefix = redisKeyPrefix; }
    public boolean isAckEnabled() { return ackEnabled; }
    public void setAckEnabled(boolean ackEnabled) { this.ackEnabled = ackEnabled; }
    public boolean isOfflineMessageEnabled() { return offlineMessageEnabled; }
    public void setOfflineMessageEnabled(boolean offlineMessageEnabled) { this.offlineMessageEnabled = offlineMessageEnabled; }
    public int getMaxMessageLength() { return maxMessageLength; }
    public void setMaxMessageLength(int maxMessageLength) { this.maxMessageLength = maxMessageLength; }
    public int getMaxHistoryPerConversation() { return maxHistoryPerConversation; }
    public void setMaxHistoryPerConversation(int maxHistoryPerConversation) { this.maxHistoryPerConversation = maxHistoryPerConversation; }
}
