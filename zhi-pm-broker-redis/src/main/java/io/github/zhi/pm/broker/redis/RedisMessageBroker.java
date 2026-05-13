package io.github.zhi.pm.broker.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.core.broker.BrokerMessage;
import io.github.zhi.pm.core.broker.MessageBroker;
import java.io.IOException;
import java.util.Objects;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class RedisMessageBroker implements MessageBroker {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String instanceId;

    public RedisMessageBroker(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, String topic, String instanceId) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.topic = Objects.requireNonNull(topic, "topic");
        this.instanceId = instanceId;
    }

    @Override
    public Mono<Void> publish(BrokerMessage message) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(new SerializableBrokerMessage(message)))
                .flatMap(json -> redisTemplate.convertAndSend(topic, json))
                .then();
    }

    @Override
    public Flux<BrokerMessage> subscribe() {
        return redisTemplate.listenTo(ChannelTopic.of(topic))
                .map(message -> deserialize(message.getMessage()))
                .filter(brokerMessage -> !Objects.equals(brokerMessage.sourceInstanceId(), instanceId));
    }

    private BrokerMessage deserialize(String json) {
        try {
            SerializableBrokerMessage serializable = objectMapper.readValue(json, SerializableBrokerMessage.class);
            return serializable.toBrokerMessage();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to deserialize BrokerMessage from Redis", ex);
        }
    }

    static class SerializableBrokerMessage {
        private String targetType;
        private String targetId;
        private String messageJson;
        private String sourceInstanceId;

        public SerializableBrokerMessage() {
        }

        SerializableBrokerMessage(BrokerMessage bm) {
            this.targetType = bm.targetType().name();
            this.targetId = bm.targetId();
            this.sourceInstanceId = bm.sourceInstanceId();
            try {
                this.messageJson = new ObjectMapper().writeValueAsString(bm.message());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to serialize WsMessage", e);
            }
        }

        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }
        public String getTargetId() { return targetId; }
        public void setTargetId(String targetId) { this.targetId = targetId; }
        public String getMessageJson() { return messageJson; }
        public void setMessageJson(String messageJson) { this.messageJson = messageJson; }
        public String getSourceInstanceId() { return sourceInstanceId; }
        public void setSourceInstanceId(String sourceInstanceId) { this.sourceInstanceId = sourceInstanceId; }

        BrokerMessage toBrokerMessage() {
            try {
                ObjectMapper mapper = new ObjectMapper();
                io.github.zhi.pm.core.message.WsMessage<?> wsMessage = mapper.readValue(messageJson, io.github.zhi.pm.core.message.WsMessage.class);
                return new BrokerMessage(
                        BrokerMessage.TargetType.valueOf(targetType),
                        targetId,
                        wsMessage,
                        sourceInstanceId
                );
            } catch (IOException e) {
                throw new IllegalStateException("Failed to deserialize WsMessage from broker payload", e);
            }
        }
    }
}
