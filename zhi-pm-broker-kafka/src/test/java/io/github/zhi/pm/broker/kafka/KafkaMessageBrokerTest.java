package io.github.zhi.pm.broker.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.core.broker.BrokerMessage;
import io.github.zhi.pm.core.message.WsMessage;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KafkaMessageBrokerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializableBrokerMessageShouldRoundTrip() throws Exception {
        WsMessage<Map<String, Object>> wsMessage = new WsMessage<>(
                "msg-1", "test.type", "trace-1", Instant.now(), Map.of("key", "value")
        );
        BrokerMessage original = BrokerMessage.forUser("user-1", wsMessage, "instance-1");

        KafkaMessageBroker.SerializableBrokerMessage serializable = new KafkaMessageBroker.SerializableBrokerMessage(original);
        String json = objectMapper.writeValueAsString(serializable);

        KafkaMessageBroker.SerializableBrokerMessage deserialized = objectMapper.readValue(json, KafkaMessageBroker.SerializableBrokerMessage.class);
        BrokerMessage result = deserialized.toBrokerMessage();

        assertNotNull(result);
        assertEquals(BrokerMessage.TargetType.USER, result.targetType());
        assertEquals("user-1", result.targetId());
        assertEquals("instance-1", result.sourceInstanceId());
        assertEquals("msg-1", result.message().getId());
        assertEquals("test.type", result.message().getType());
    }

    @Test
    void serializableBrokerMessageShouldHandleRoomTarget() throws Exception {
        WsMessage<String> wsMessage = new WsMessage<>(
                "msg-2", "room.message", "trace-2", Instant.now(), "hello"
        );
        BrokerMessage original = BrokerMessage.forRoom("room-1", wsMessage, "instance-2");

        KafkaMessageBroker.SerializableBrokerMessage serializable = new KafkaMessageBroker.SerializableBrokerMessage(original);
        String json = objectMapper.writeValueAsString(serializable);

        KafkaMessageBroker.SerializableBrokerMessage deserialized = objectMapper.readValue(json, KafkaMessageBroker.SerializableBrokerMessage.class);
        BrokerMessage result = deserialized.toBrokerMessage();

        assertNotNull(result);
        assertEquals(BrokerMessage.TargetType.ROOM, result.targetType());
        assertEquals("room-1", result.targetId());
        assertEquals("instance-2", result.sourceInstanceId());
    }

    @Test
    void serializableBrokerMessageShouldHandleBroadcastTarget() throws Exception {
        WsMessage<String> wsMessage = new WsMessage<>(
                "msg-3", "broadcast", "trace-3", Instant.now(), "announcement"
        );
        BrokerMessage original = BrokerMessage.forBroadcast(wsMessage, "instance-3");

        KafkaMessageBroker.SerializableBrokerMessage serializable = new KafkaMessageBroker.SerializableBrokerMessage(original);
        String json = objectMapper.writeValueAsString(serializable);

        KafkaMessageBroker.SerializableBrokerMessage deserialized = objectMapper.readValue(json, KafkaMessageBroker.SerializableBrokerMessage.class);
        BrokerMessage result = deserialized.toBrokerMessage();

        assertNotNull(result);
        assertEquals(BrokerMessage.TargetType.BROADCAST, result.targetType());
        assertEquals("instance-3", result.sourceInstanceId());
        assertEquals("msg-3", result.message().getId());
    }

    @Test
    void propertiesShouldHaveCorrectDefaults() {
        KafkaBrokerProperties properties = new KafkaBrokerProperties();

        assertEquals("localhost:9092", properties.getBootstrapServers());
        assertEquals("realtime-ws-message", properties.getTopic());
        assertEquals("realtime-ws-gateway", properties.getConsumerGroup());
        assertEquals("realtime-ws-message-dlt", properties.getDeadLetterTopic());
        assertNotNull(properties.getRetry());
        assertEquals(3, properties.getRetry().getMaxAttempts());
        assertEquals(1000, properties.getRetry().getBackoffMs());
    }

    @Test
    void propertiesShouldAllowCustomization() {
        KafkaBrokerProperties properties = new KafkaBrokerProperties();
        properties.setBootstrapServers("kafka1:9092,kafka2:9092");
        properties.setTopic("custom-topic");
        properties.setConsumerGroup("custom-group");
        properties.setDeadLetterTopic("custom-dlt");
        properties.getRetry().setMaxAttempts(5);
        properties.getRetry().setBackoffMs(2000);

        assertEquals("kafka1:9092,kafka2:9092", properties.getBootstrapServers());
        assertEquals("custom-topic", properties.getTopic());
        assertEquals("custom-group", properties.getConsumerGroup());
        assertEquals("custom-dlt", properties.getDeadLetterTopic());
        assertEquals(5, properties.getRetry().getMaxAttempts());
        assertEquals(2000, properties.getRetry().getBackoffMs());
    }
}
