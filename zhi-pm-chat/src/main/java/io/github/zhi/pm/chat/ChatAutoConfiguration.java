package io.github.zhi.pm.chat;

import io.github.zhi.pm.core.chat.ChatService;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.chat.storage.InMemoryChatStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ChatService.class)
@EnableConfigurationProperties(ChatProperties.class)
@ConditionalOnProperty(prefix = "realtime.chat", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    InMemoryChatStorage chatStorage(ChatProperties properties) {
        return new InMemoryChatStorage(properties.getMaxHistoryPerConversation());
    }

    @Bean
    @ConditionalOnMissingBean(ChatService.class)
    ChatServiceImpl chatService(MessageSender sender, ConnectionRegistry registry,
                                 InMemoryChatStorage storage, ChatProperties properties) {
        return new ChatServiceImpl(sender, registry, storage, properties.getMaxMessageLength());
    }
}
