package io.github.zhi.pm.core.auth;

import reactor.core.publisher.Mono;

public interface WebSocketAuthenticator {
    Mono<AuthenticationResult> authenticate(AuthenticationRequest request);
}
