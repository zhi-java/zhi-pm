package io.github.zhi.pm.broker.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "realtime.broker.redis")
public class RedisBrokerProperties {
    private String topic = "realtime:ws:message";

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}
