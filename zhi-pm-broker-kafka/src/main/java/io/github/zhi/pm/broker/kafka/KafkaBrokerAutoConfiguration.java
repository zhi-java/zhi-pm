package io.github.zhi.pm.broker.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.core.broker.MessageBroker;
import java.util.Collections;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

@AutoConfiguration
@ConditionalOnClass(KafkaSender.class)
@ConditionalOnProperty(prefix = "realtime.broker", name = "type", havingValue = "kafka")
@EnableConfigurationProperties(KafkaBrokerProperties.class)
public class KafkaBrokerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    String brokerInstanceId() {
        return UUID.randomUUID().toString();
    }

    @Bean
    @ConditionalOnMissingBean
    KafkaSender<String, String> kafkaSender(KafkaBrokerProperties properties) {
        var props = new java.util.HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        SenderOptions<String, String> senderOptions = SenderOptions.create(props);
        return KafkaSender.create(senderOptions);
    }

    @Bean
    @ConditionalOnMissingBean
    KafkaReceiver<String, String> kafkaReceiver(KafkaBrokerProperties properties) {
        var props = new java.util.HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getConsumerGroup());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(props)
                .subscription(Collections.singleton(properties.getTopic()));

        return KafkaReceiver.create(receiverOptions);
    }

    @Bean
    @ConditionalOnMissingBean(MessageBroker.class)
    KafkaMessageBroker kafkaMessageBroker(KafkaSender<String, String> sender,
                                           KafkaReceiver<String, String> receiver,
                                           ObjectMapper objectMapper,
                                           KafkaBrokerProperties properties,
                                           String brokerInstanceId) {
        return new KafkaMessageBroker(
                sender,
                receiver,
                objectMapper,
                properties.getTopic(),
                properties.getDeadLetterTopic(),
                brokerInstanceId,
                properties.getRetry().getMaxAttempts(),
                properties.getRetry().getBackoffMs()
        );
    }
}
