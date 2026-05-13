package io.github.zhi.pm.core.auth;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class SimpleTokenWebSocketAuthenticatorTest {
    @Test
    void validatesConfiguredTokens() {
        SimpleTokenWebSocketAuthenticator authenticator = new SimpleTokenWebSocketAuthenticator(Collections.singletonMap("token-a", "alice"), false);
        StepVerifier.create(authenticator.authenticate(new AuthenticationRequest("token-a", Collections.emptyMap(), null)))
                .expectNextMatches(result -> result.authenticated() && "alice".equals(result.userId()))
                .verifyComplete();
        StepVerifier.create(authenticator.authenticate(new AuthenticationRequest("wrong", Collections.emptyMap(), null)))
                .expectNextMatches(result -> !result.authenticated())
                .verifyComplete();
    }

    @Test
    void rejectsUnmappedTokensWhenFallbackIsDisabled() {
        SimpleTokenWebSocketAuthenticator authenticator = new SimpleTokenWebSocketAuthenticator(Collections.emptyMap(), false);
        StepVerifier.create(authenticator.authenticate(new AuthenticationRequest("raw-token-value", Collections.emptyMap(), null)))
                .expectNextMatches(result -> !result.authenticated())
                .verifyComplete();
    }
}
