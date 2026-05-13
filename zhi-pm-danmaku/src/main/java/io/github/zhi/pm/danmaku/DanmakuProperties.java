package io.github.zhi.pm.danmaku;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "realtime.danmaku")
public class DanmakuProperties {
    private boolean enabled = true;
    private int maxContentLength = 100;
    private int maxMessagePerUserPerSecond = 2;
    private int maxMessagePerRoomPerSecond = 5000;
    private boolean dropWhenOverloaded = true;
    private List<String> sensitiveWords = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getMaxContentLength() { return maxContentLength; }
    public void setMaxContentLength(int maxContentLength) { this.maxContentLength = maxContentLength; }
    public int getMaxMessagePerUserPerSecond() { return maxMessagePerUserPerSecond; }
    public void setMaxMessagePerUserPerSecond(int maxMessagePerUserPerSecond) { this.maxMessagePerUserPerSecond = maxMessagePerUserPerSecond; }
    public int getMaxMessagePerRoomPerSecond() { return maxMessagePerRoomPerSecond; }
    public void setMaxMessagePerRoomPerSecond(int maxMessagePerRoomPerSecond) { this.maxMessagePerRoomPerSecond = maxMessagePerRoomPerSecond; }
    public boolean isDropWhenOverloaded() { return dropWhenOverloaded; }
    public void setDropWhenOverloaded(boolean dropWhenOverloaded) { this.dropWhenOverloaded = dropWhenOverloaded; }
    public List<String> getSensitiveWords() { return sensitiveWords; }
    public void setSensitiveWords(List<String> sensitiveWords) { this.sensitiveWords = sensitiveWords; }
}
