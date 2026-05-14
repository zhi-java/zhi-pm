package io.github.zhi.pm.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.core.chat.ChatService;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.chat.storage.ChatStorage;
import io.github.zhi.pm.chat.storage.InMemoryChatStorage;
import io.github.zhi.pm.chat.storage.RedisChatStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(ChatService.class)
@EnableConfigurationProperties(ChatProperties.class)
@ConditionalOnProperty(prefix = "realtime.chat", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChatAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ReactiveRedisTemplate.class)
    static class RedisStorageConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatStorage.class)
        @ConditionalOnProperty(prefix = "realtime.chat", name = "storage-type", havingValue = "redis")
        ChatStorage redisChatStorage(ReactiveRedisTemplate<String, String> redis,
                                      ObjectMapper objectMapper, ChatProperties properties) {
            return new RedisChatStorage(redis, objectMapper, properties.getRedisKeyPrefix(),
                    properties.getMaxHistoryPerConversation());
        }
    }

    @Bean
    @ConditionalOnMissingBean(ChatStorage.class)
    ChatStorage inMemoryChatStorage(ChatProperties properties) {
        return new InMemoryChatStorage(properties.getMaxHistoryPerConversation());
    }

    @Bean
    @ConditionalOnMissingBean(ChatService.class)
    ChatServiceImpl chatService(MessageSender sender, ConnectionRegistry registry,
                                 ChatStorage storage, ChatProperties properties) {
        return new ChatServiceImpl(sender, registry, storage,
                properties.getMaxMessageLength(), properties.isOfflineMessageEnabled());
    }
}
