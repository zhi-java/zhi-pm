package io.github.zhi.pm.broker.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.core.broker.MessageBroker;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(ReactiveRedisTemplate.class)
@ConditionalOnProperty(prefix = "realtime.broker", name = "type", havingValue = "redis")
@EnableConfigurationProperties(RedisBrokerProperties.class)
public class RedisBrokerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    String brokerInstanceId() {
        return UUID.randomUUID().toString();
    }

    @Bean
    @ConditionalOnMissingBean(MessageBroker.class)
    RedisMessageBroker redisMessageBroker(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, RedisBrokerProperties properties, String brokerInstanceId) {
        return new RedisMessageBroker(redisTemplate, objectMapper, properties.getTopic(), brokerInstanceId);
    }
}
