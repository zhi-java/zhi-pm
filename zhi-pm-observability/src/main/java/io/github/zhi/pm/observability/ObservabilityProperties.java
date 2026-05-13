package io.github.zhi.pm.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "realtime.observability")
public class ObservabilityProperties {
    private boolean enabled = true;
    private boolean connectionMetrics = true;
    private boolean messageMetrics = true;
    private boolean heartbeatMetrics = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isConnectionMetrics() { return connectionMetrics; }
    public void setConnectionMetrics(boolean connectionMetrics) { this.connectionMetrics = connectionMetrics; }
    public boolean isMessageMetrics() { return messageMetrics; }
    public void setMessageMetrics(boolean messageMetrics) { this.messageMetrics = messageMetrics; }
    public boolean isHeartbeatMetrics() { return heartbeatMetrics; }
    public void setHeartbeatMetrics(boolean heartbeatMetrics) { this.heartbeatMetrics = heartbeatMetrics; }
}
