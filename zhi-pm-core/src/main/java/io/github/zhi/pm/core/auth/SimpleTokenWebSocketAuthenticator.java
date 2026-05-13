package io.github.zhi.pm.core.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import reactor.core.publisher.Mono;

public final class SimpleTokenWebSocketAuthenticator implements WebSocketAuthenticator {
    private final Map<String, String> tokenToUserId;
    private final boolean acceptNonBlankTokenWhenNoTokensConfigured;

    public SimpleTokenWebSocketAuthenticator(Map<String, String> tokenToUserId, boolean acceptNonBlankTokenWhenNoTokensConfigured) {
        this.tokenToUserId = tokenToUserId == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(tokenToUserId));
        this.acceptNonBlankTokenWhenNoTokensConfigured = acceptNonBlankTokenWhenNoTokensConfigured;
    }

    @Override
    public Mono<AuthenticationResult> authenticate(AuthenticationRequest request) {
        return Mono.fromSupplier(() -> {
            String token = request == null ? null : request.token();
            if (token == null || token.trim().isEmpty()) {
                return AuthenticationResult.rejected();
            }
            String userId = tokenToUserId.get(token);
            if (userId != null && !userId.trim().isEmpty()) {
                return AuthenticationResult.authenticated(userId);
            }
            if (tokenToUserId.isEmpty() && acceptNonBlankTokenWhenNoTokensConfigured) {
                return AuthenticationResult.authenticated(token);
            }
            return AuthenticationResult.rejected();
        });
    }
}
