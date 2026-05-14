package io.github.zhi.pm.registry.redis;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "realtime.registry.redis")
public class RedisRegistryProperties {
    private String keyPrefix = "realtime";
    private Duration sessionTtl = Duration.ofSeconds(120);

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public Duration getSessionTtl() { return sessionTtl; }
    public void setSessionTtl(Duration sessionTtl) { this.sessionTtl = sessionTtl; }
}
