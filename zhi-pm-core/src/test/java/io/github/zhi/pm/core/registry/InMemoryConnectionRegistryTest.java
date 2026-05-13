package io.github.zhi.pm.core.registry;

import io.github.zhi.pm.core.session.DefaultSessionConnection;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class InMemoryConnectionRegistryTest {
    @Test
    void registersAndUnregistersUserIndexes() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection connection = new DefaultSessionConnection("s1", "u1", java.util.Collections.emptyMap(), 2, reason -> Mono.empty());
        StepVerifier.create(registry.register(connection).then(registry.countConnections())).expectNext(1L).verifyComplete();
        StepVerifier.create(registry.getUserConnections("u1").count()).expectNext(1L).verifyComplete();
        StepVerifier.create(registry.unregister("s1").then(registry.isOnline("u1"))).expectNext(false).verifyComplete();
    }
}
