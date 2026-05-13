package io.github.zhi.pm.autoconfigure;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "realtime.websocket")
public class RealtimeWebSocketProperties {
    private boolean enabled = true;
    private String path = "/ws";
    private int outboundBufferSize = 256;
    private final Auth auth = new Auth();
    private final Heartbeat heartbeat = new Heartbeat();
    private final Room room = new Room();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public int getOutboundBufferSize() { return outboundBufferSize; }
    public void setOutboundBufferSize(int outboundBufferSize) { this.outboundBufferSize = outboundBufferSize; }
    public Auth getAuth() { return auth; }
    public Heartbeat getHeartbeat() { return heartbeat; }
    public Room getRoom() { return room; }

    public static class Auth {
        private boolean enabled = true;
        private String tokenParamName = "access_token";
        private String headerName = "Authorization";
        private Map<String, String> demoTokens = new LinkedHashMap<>();
        private boolean acceptNonBlankTokenWhenNoTokensConfigured = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTokenParamName() { return tokenParamName; }
        public void setTokenParamName(String tokenParamName) { this.tokenParamName = tokenParamName; }
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
        public Map<String, String> getDemoTokens() { return demoTokens; }
        public void setDemoTokens(Map<String, String> demoTokens) { this.demoTokens = demoTokens; }
        public boolean isAcceptNonBlankTokenWhenNoTokensConfigured() { return acceptNonBlankTokenWhenNoTokensConfigured; }
        public void setAcceptNonBlankTokenWhenNoTokensConfigured(boolean accept) { this.acceptNonBlankTokenWhenNoTokensConfigured = accept; }
    }

    public static class Heartbeat {
        private boolean enabled = true;
        private Duration clientTimeout = Duration.ofSeconds(60);
        private Duration checkInterval = Duration.ofSeconds(30);
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Duration getClientTimeout() { return clientTimeout; }
        public void setClientTimeout(Duration clientTimeout) { this.clientTimeout = clientTimeout; }
        public Duration getCheckInterval() { return checkInterval; }
        public void setCheckInterval(Duration checkInterval) { this.checkInterval = checkInterval; }
    }

    public static class Room {
        private boolean enabled = true;
        private int maxRoomsPerUser = 20;
        private int maxMembersPerRoom = 100000;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxRoomsPerUser() { return maxRoomsPerUser; }
        public void setMaxRoomsPerUser(int maxRoomsPerUser) { this.maxRoomsPerUser = maxRoomsPerUser; }
        public int getMaxMembersPerRoom() { return maxMembersPerRoom; }
        public void setMaxMembersPerRoom(int maxMembersPerRoom) { this.maxMembersPerRoom = maxMembersPerRoom; }
    }
}
