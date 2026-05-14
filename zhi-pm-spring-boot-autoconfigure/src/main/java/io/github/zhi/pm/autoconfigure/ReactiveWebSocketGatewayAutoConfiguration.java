package io.github.zhi.pm.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhi.pm.core.auth.JwtWebSocketAuthenticator;
import io.github.zhi.pm.core.auth.SimpleTokenWebSocketAuthenticator;
import io.github.zhi.pm.core.auth.WebSocketAuthenticator;
import io.github.zhi.pm.core.chat.ChatService;
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
    @ConditionalOnProperty(prefix = "realtime.websocket.auth", name = "type", havingValue = "demo", matchIfMissing = true)
    WebSocketAuthenticator demoWebSocketAuthenticator(RealtimeWebSocketProperties properties) {
        return new SimpleTokenWebSocketAuthenticator(properties.getAuth().getDemoTokens(), properties.getAuth().isAcceptNonBlankTokenWhenNoTokensConfigured());
    }

    @Bean @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "realtime.websocket.auth", name = "type", havingValue = "jwt")
    WebSocketAuthenticator jwtWebSocketAuthenticator(RealtimeWebSocketProperties properties) {
        RealtimeWebSocketProperties.Jwt jwt = properties.getAuth().getJwt();
        if (jwt.getSecret() != null && !jwt.getSecret().isBlank()) {
            return JwtWebSocketAuthenticator.withHmacSecret(jwt.getSecret(), jwt.getIssuer(),
                    jwt.getAudience(), jwt.getUserIdClaim(), jwt.isAllowGuestWhenNoToken());
        }
        if (jwt.getPublicKey() != null && !jwt.getPublicKey().isBlank()) {
            java.security.KeyFactory kf;
            java.security.PublicKey publicKey;
            try {
                kf = java.security.KeyFactory.getInstance("RSA");
                String pem = jwt.getPublicKey()
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");
                publicKey = kf.generatePublic(new java.security.spec.X509EncodedKeySpec(
                        java.util.Base64.getDecoder().decode(pem)));
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse realtime.websocket.auth.jwt.public-key", e);
            }
            return JwtWebSocketAuthenticator.withSigningKey(publicKey, jwt.getIssuer(),
                    jwt.getAudience(), jwt.getUserIdClaim(), jwt.isAllowGuestWhenNoToken());
        }
        throw new IllegalArgumentException("realtime.websocket.auth.jwt.secret or public-key is required for JWT authentication");
    }

    @Bean @ConditionalOnMissingBean
    HeartbeatService heartbeatService(RealtimeWebSocketProperties properties) { return new HeartbeatService(properties.getHeartbeat().getClientTimeout()); }

    @Bean @ConditionalOnMissingBean
    ObjectMapper objectMapper() { return new ObjectMapper(); }

    @Bean @ConditionalOnMissingBean
    GatewayWebSocketHandler gatewayWebSocketHandler(ConnectionRegistry registry, MessageSender sender, WebSocketAuthenticator authenticator, HeartbeatService heartbeatService, ObjectMapper objectMapper, RealtimeWebSocketProperties properties, ObjectProvider<DanmakuService> danmakuServiceProvider, ObjectProvider<ChatService> chatServiceProvider) {
        return new GatewayWebSocketHandler(registry, sender, authenticator, heartbeatService, objectMapper, properties, danmakuServiceProvider.getIfAvailable(), chatServiceProvider.getIfAvailable());
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
