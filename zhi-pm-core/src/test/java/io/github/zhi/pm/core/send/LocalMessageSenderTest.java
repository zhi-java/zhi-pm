package io.github.zhi.pm.core.send;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.InMemoryConnectionRegistry;
import io.github.zhi.pm.core.session.DefaultSessionConnection;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class LocalMessageSenderTest {
    @Test
    void sendsToUserAndBroadcasts() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection one = new DefaultSessionConnection("s1", "u1", java.util.Collections.emptyMap(), 4, reason -> Mono.empty());
        DefaultSessionConnection two = new DefaultSessionConnection("s2", "u2", java.util.Collections.emptyMap(), 4, reason -> Mono.empty());
        LocalMessageSender sender = new LocalMessageSender(registry);
        StepVerifier.create(registry.register(one).then(registry.register(two))).verifyComplete();
        StepVerifier.create(sender.sendToUser("u1", WsMessage.of("notice", java.util.Collections.singletonMap("ok", true)))).expectNext(true).verifyComplete();
        StepVerifier.create(sender.broadcast(WsMessage.of("broadcast", null))).expectNext(2).verifyComplete();
    }

    @Test
    void closesConnectionWhenOutboundBufferOverflows() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        AtomicInteger closes = new AtomicInteger();
        DefaultSessionConnection connection = new DefaultSessionConnection("s1", "u1", java.util.Collections.emptyMap(), 1, reason -> {
            closes.incrementAndGet();
            return Mono.empty();
        });
        LocalMessageSender sender = new LocalMessageSender(registry);
        StepVerifier.create(registry.register(connection)).verifyComplete();

        StepVerifier.create(connection.outboundMessages(), 0)
                .then(() -> StepVerifier.create(sender.sendToUser("u1", WsMessage.of("first", null))).expectNext(true).verifyComplete())
                .then(() -> StepVerifier.create(sender.sendToUser("u1", WsMessage.of("second", null))).expectNext(false).verifyComplete())
                .thenCancel()
                .verify();
        org.junit.jupiter.api.Assertions.assertEquals(1, closes.get());
    }
}
