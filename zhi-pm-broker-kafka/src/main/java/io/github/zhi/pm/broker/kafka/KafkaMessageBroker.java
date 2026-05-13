package io.github.zhi.pm.broker.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.zhi.pm.core.broker.BrokerMessage;
import io.github.zhi.pm.core.broker.MessageBroker;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

public final class KafkaMessageBroker implements MessageBroker {
    private static final Logger log = LoggerFactory.getLogger(KafkaMessageBroker.class);
    private static final String HEADER_RETRY_COUNT = "retry-count";
    private static final String HEADER_SOURCE_INSTANCE = "source-instance-id";
    private static final ObjectMapper SERIALIZATION_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final KafkaSender<String, String> sender;
    private final KafkaReceiver<String, String> receiver;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String deadLetterTopic;
    private final String instanceId;
    private final int maxRetries;
    private final long backoffMs;

    public KafkaMessageBroker(KafkaSender<String, String> sender,
                              KafkaReceiver<String, String> receiver,
                              ObjectMapper objectMapper,
                              String topic,
                              String deadLetterTopic,
                              String instanceId,
                              int maxRetries,
                              long backoffMs) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.receiver = Objects.requireNonNull(receiver, "receiver");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.topic = Objects.requireNonNull(topic, "topic");
        this.deadLetterTopic = Objects.requireNonNull(deadLetterTopic, "deadLetterTopic");
        this.instanceId = instanceId;
        this.maxRetries = maxRetries;
        this.backoffMs = backoffMs;
    }

    @Override
    public Mono<Void> publish(BrokerMessage message) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(new SerializableBrokerMessage(message)))
                .flatMap(json -> {
                    ProducerRecord<String, String> record = new ProducerRecord<>(topic, null, json);
                    record.headers().add(new RecordHeader(HEADER_SOURCE_INSTANCE, instanceId.getBytes()));
                    record.headers().add(new RecordHeader(HEADER_RETRY_COUNT, "0".getBytes()));
                    return sender.send(Mono.just(SenderRecord.create(record, null)))
                            .then();
                });
    }

    @Override
    public Flux<BrokerMessage> subscribe() {
        return receiver.receive()
                .flatMap(this::processRecord, 1);
    }

    private Flux<BrokerMessage> processRecord(ConsumerRecord<String, String> record) {
        String sourceInstanceId = getHeader(record, HEADER_SOURCE_INSTANCE);

        // Skip messages from the same instance
        if (Objects.equals(sourceInstanceId, instanceId)) {
            return Flux.empty();
        }

        String retryCountStr = getHeader(record, HEADER_RETRY_COUNT);
        int retryCount = retryCountStr != null ? Integer.parseInt(retryCountStr) : 0;

        try {
            BrokerMessage message = deserialize(record.value());
            return Flux.just(message);
        } catch (Exception ex) {
            log.warn("Failed to deserialize BrokerMessage from Kafka, attempt {}/{}", retryCount + 1, maxRetries, ex);
            return handleRetry(record, retryCount, ex);
        }
    }

    private Flux<BrokerMessage> handleRetry(ConsumerRecord<String, String> record, int currentRetryCount, Exception error) {
        if (currentRetryCount >= maxRetries) {
            log.error("Max retries ({}) reached for message, sending to dead letter topic: {}", maxRetries, deadLetterTopic);
            return sendToDeadLetter(record, currentRetryCount);
        }

        int nextRetryCount = currentRetryCount + 1;
        log.info("Scheduling retry {}/{} with backoff {}ms", nextRetryCount, maxRetries, backoffMs);

        // Create retry record with incremented retry count
        ProducerRecord<String, String> retryRecord = new ProducerRecord<>(topic, record.key(), record.value());
        copyHeaders(record, retryRecord);
        retryRecord.headers().remove(HEADER_RETRY_COUNT);
        retryRecord.headers().add(new RecordHeader(HEADER_RETRY_COUNT, String.valueOf(nextRetryCount).getBytes()));

        // Apply backoff delay before sending the retry record
        return Mono.delay(Duration.ofMillis(backoffMs))
                .thenMany(sender.send(Mono.just(SenderRecord.create(retryRecord, null))))
                .thenMany(Flux.empty());
    }

    private Flux<BrokerMessage> sendToDeadLetter(ConsumerRecord<String, String> record, int retryCount) {
        ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(deadLetterTopic, record.key(), record.value());
        copyHeaders(record, dlqRecord);
        dlqRecord.headers().add(new RecordHeader("final-retry-count", String.valueOf(retryCount).getBytes()));
        dlqRecord.headers().add(new RecordHeader("original-topic", topic.getBytes()));

        return sender.send(Mono.just(SenderRecord.create(dlqRecord, null)))
                .doOnError(e -> log.error("Failed to send message to dead letter topic", e))
                .thenMany(Flux.empty());
    }

    private void copyHeaders(ConsumerRecord<String, String> source, ProducerRecord<String, String> target) {
        source.headers().forEach(header -> {
            if (!HEADER_RETRY_COUNT.equals(header.key())) {
                target.headers().add(header);
            }
        });
    }

    private String getHeader(ConsumerRecord<String, String> record, String headerName) {
        RecordHeader header = (RecordHeader) record.headers().lastHeader(headerName);
        return header != null ? new String(header.value()) : null;
    }

    private BrokerMessage deserialize(String json) {
        try {
            SerializableBrokerMessage serializable = objectMapper.readValue(json, SerializableBrokerMessage.class);
            return serializable.toBrokerMessage();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to deserialize BrokerMessage from Kafka", ex);
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
                this.messageJson = SERIALIZATION_MAPPER.writeValueAsString(bm.message());
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
                io.github.zhi.pm.core.message.WsMessage<?> wsMessage = SERIALIZATION_MAPPER.readValue(messageJson, io.github.zhi.pm.core.message.WsMessage.class);
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
