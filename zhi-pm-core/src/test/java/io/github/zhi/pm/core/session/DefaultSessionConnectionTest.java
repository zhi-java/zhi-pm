package io.github.zhi.pm.core.session;

import io.github.zhi.pm.core.message.WsMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSessionConnectionTest {
    @Test
    void reportsOverflowForBoundedOutboundQueue() {
        DefaultSessionConnection connection = new DefaultSessionConnection("s1", "u1", java.util.Collections.emptyMap(), 1, reason -> Mono.empty());
        StepVerifier.create(connection.outboundMessages(), 0)
                .then(() -> assertEquals(SendOutcome.SENT, connection.trySend(WsMessage.of("one", null))))
                .then(() -> assertEquals(SendOutcome.OVERFLOW, connection.trySend(WsMessage.of("two", null))))
                .thenCancel()
                .verify();
    }
}
