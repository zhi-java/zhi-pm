package io.github.zhi.pm.registry.redis;

import io.github.zhi.pm.core.broker.MessageBroker;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.ClusterAwareMessageSender;
import io.github.zhi.pm.core.send.MessageSender;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(ReactiveRedisTemplate.class)
@ConditionalOnProperty(prefix = "realtime.registry", name = "type", havingValue = "redis")
@AutoConfigureBefore(name = "io.github.zhi.pm.autoconfigure.ReactiveWebSocketGatewayAutoConfiguration")
@EnableConfigurationProperties(RedisRegistryProperties.class)
public class RedisRegistryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConnectionRegistry.class)
    RedisConnectionRegistry redisConnectionRegistry(ReactiveRedisTemplate<String, String> redis, RedisRegistryProperties properties) {
        return new RedisConnectionRegistry(redis, properties.getKeyPrefix(), properties.getSessionTtl());
    }

    @Bean
    @ConditionalOnMissingBean(MessageSender.class)
    MessageSender clusterAwareMessageSender(ConnectionRegistry registry, ObjectProvider<MessageBroker> brokerProvider) {
        MessageBroker broker = brokerProvider.getIfAvailable();
        if (broker != null) {
            return new ClusterAwareMessageSender(registry, broker, UUID.randomUUID().toString());
        }
        return new io.github.zhi.pm.core.send.LocalMessageSender(registry);
    }
}
