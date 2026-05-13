package io.github.zhi.pm.broker.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "realtime.broker.kafka")
public class KafkaBrokerProperties {
    private String bootstrapServers = "localhost:9092";
    private String topic = "realtime-ws-message";
    private String consumerGroup = "realtime-ws-gateway";
    private String deadLetterTopic = "realtime-ws-message-dlt";
    private Retry retry = new Retry();

    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getConsumerGroup() { return consumerGroup; }
    public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }

    public String getDeadLetterTopic() { return deadLetterTopic; }
    public void setDeadLetterTopic(String deadLetterTopic) { this.deadLetterTopic = deadLetterTopic; }

    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }

    public static class Retry {
        private int maxAttempts = 3;
        private long backoffMs = 1000;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public long getBackoffMs() { return backoffMs; }
        public void setBackoffMs(long backoffMs) { this.backoffMs = backoffMs; }
    }
}
