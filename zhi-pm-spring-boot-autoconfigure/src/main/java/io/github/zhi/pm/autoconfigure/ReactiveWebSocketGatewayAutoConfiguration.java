package io.github.zhi.pm.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.core.auth.SimpleTokenWebSocketAuthenticator;
import io.github.zhi.pm.core.auth.WebSocketAuthenticator;
import io.github.zhi.pm.core.danmaku.DanmakuService;
import io.github.zhi.pm.core.heartbeat.HeartbeatService;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.registry.InMemoryConnectionRegistry;
import io.github.zhi.pm.core.send.LocalMessageSender;
import io.github.zhi.pm.core.send.MessageSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

@AutoConfiguration
@ConditionalOnClass(WebSocketHandler.class)
@EnableConfigurationProperties(RealtimeWebSocketProperties.class)
@ConditionalOnProperty(prefix = "realtime.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReactiveWebSocketGatewayAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    ConnectionRegistry connectionRegistry() { return new InMemoryConnectionRegistry(); }

    @Bean @ConditionalOnMissingBean
    MessageSender messageSender(ConnectionRegistry registry) { return new LocalMessageSender(registry); }

    @Bean @ConditionalOnMissingBean
    WebSocketAuthenticator webSocketAuthenticator(RealtimeWebSocketProperties properties) {
        return new SimpleTokenWebSocketAuthenticator(properties.getAuth().getDemoTokens(), properties.getAuth().isAcceptNonBlankTokenWhenNoTokensConfigured());
    }

    @Bean @ConditionalOnMissingBean
    HeartbeatService heartbeatService(RealtimeWebSocketProperties properties) { return new HeartbeatService(properties.getHeartbeat().getClientTimeout()); }

    @Bean @ConditionalOnMissingBean
    ObjectMapper objectMapper() { return new ObjectMapper(); }

    @Bean @ConditionalOnMissingBean
    GatewayWebSocketHandler gatewayWebSocketHandler(ConnectionRegistry registry, MessageSender sender, WebSocketAuthenticator authenticator, HeartbeatService heartbeatService, ObjectMapper objectMapper, RealtimeWebSocketProperties properties, ObjectProvider<DanmakuService> danmakuServiceProvider) {
        return new GatewayWebSocketHandler(registry, sender, authenticator, heartbeatService, objectMapper, properties, danmakuServiceProvider.getIfAvailable());
    }

    @Bean @ConditionalOnMissingBean(name = "realtimeWebSocketHandlerMapping")
    HandlerMapping realtimeWebSocketHandlerMapping(GatewayWebSocketHandler handler, RealtimeWebSocketProperties properties) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(java.util.Collections.singletonMap(properties.getPath(), handler));
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean @ConditionalOnMissingBean
    WebSocketHandlerAdapter webSocketHandlerAdapter() { return new WebSocketHandlerAdapter(); }
}
