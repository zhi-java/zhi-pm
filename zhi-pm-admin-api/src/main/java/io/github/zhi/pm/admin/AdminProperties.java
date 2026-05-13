package io.github.zhi.pm.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "realtime.admin")
public class AdminProperties {
    private boolean enabled = true;
    private String path = "/admin/api";

    private final Live live = new Live();
    private final MessageTrace messageTrace = new MessageTrace();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Live getLive() { return live; }
    public MessageTrace getMessageTrace() { return messageTrace; }

    public static class Live {
        private boolean enabled = true;
        private long pushIntervalMs = 1000;
        private int maxConnections = 20;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getPushIntervalMs() { return pushIntervalMs; }
        public void setPushIntervalMs(long pushIntervalMs) { this.pushIntervalMs = pushIntervalMs; }
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
    }

    public static class MessageTrace {
        private boolean enabled = true;
        private double sampleRate = 0.01;
        private int maxBufferSize = 1000;
        private boolean includePayload = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public double getSampleRate() { return sampleRate; }
        public void setSampleRate(double sampleRate) { this.sampleRate = sampleRate; }
        public int getMaxBufferSize() { return maxBufferSize; }
        public void setMaxBufferSize(int maxBufferSize) { this.maxBufferSize = maxBufferSize; }
        public boolean isIncludePayload() { return includePayload; }
        public void setIncludePayload(boolean includePayload) { this.includePayload = includePayload; }
    }
}
