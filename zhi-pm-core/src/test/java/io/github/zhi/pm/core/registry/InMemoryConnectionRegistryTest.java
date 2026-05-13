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

    @Test
    void joinAndLeaveRoom() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", java.util.Collections.emptyMap(), 2, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn).then(registry.joinRoom("room1", "s1")).then(registry.countRoomConnections("room1")))
                .expectNext(1L).verifyComplete();
        StepVerifier.create(registry.getRoomConnections("room1").count()).expectNext(1L).verifyComplete();
        StepVerifier.create(registry.leaveRoom("room1", "s1").then(registry.countRoomConnections("room1")))
                .expectNext(0L).verifyComplete();
    }

    @Test
    void leaveAllRoomsOnUnregister() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", java.util.Collections.emptyMap(), 2, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn).then(registry.joinRoom("room1", "s1")).then(registry.joinRoom("room2", "s1"))).verifyComplete();
        StepVerifier.create(registry.unregister("s1").then(registry.countRoomConnections("room1"))).expectNext(0L).verifyComplete();
        StepVerifier.create(registry.countRoomConnections("room2")).expectNext(0L).verifyComplete();
    }

    @Test
    void getSessionRooms() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", java.util.Collections.emptyMap(), 2, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn).then(registry.joinRoom("room1", "s1")).then(registry.joinRoom("room2", "s1"))).verifyComplete();
        StepVerifier.create(registry.getSessionRooms("s1").sort().collectList())
                .expectNext(java.util.List.of("room1", "room2")).verifyComplete();
    }

    @Test
    void leaveAllRoomsExplicitly() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", java.util.Collections.emptyMap(), 2, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn).then(registry.joinRoom("room1", "s1")).then(registry.joinRoom("room2", "s1"))
                .then(registry.leaveAllRooms("s1"))
                .then(registry.countRoomConnections("room1"))).expectNext(0L).verifyComplete();
    }
}
