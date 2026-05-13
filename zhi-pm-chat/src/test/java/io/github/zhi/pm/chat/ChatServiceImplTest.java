package io.github.zhi.pm.chat;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.InMemoryConnectionRegistry;
import io.github.zhi.pm.core.send.LocalMessageSender;
import io.github.zhi.pm.core.session.DefaultSessionConnection;
import io.github.zhi.pm.chat.storage.InMemoryChatStorage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ChatServiceImplTest {

    private ChatServiceImpl createService() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        LocalMessageSender sender = new LocalMessageSender(registry);
        InMemoryChatStorage storage = new InMemoryChatStorage(100);
        return new ChatServiceImpl(sender, registry, storage, 2000);
    }

    @Test
    void sendsSingleChatMessage() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        LocalMessageSender sender = new LocalMessageSender(registry);
        InMemoryChatStorage storage = new InMemoryChatStorage(100);
        ChatServiceImpl service = new ChatServiceImpl(sender, registry, storage, 2000);

        DefaultSessionConnection alice = new DefaultSessionConnection("s1", "alice", Collections.emptyMap(), 64, reason -> Mono.empty());
        DefaultSessionConnection bob = new DefaultSessionConnection("s2", "bob", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(alice).then(registry.register(bob))).verifyComplete();

        WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "chat.send", null, null, "alice", null, null, null,
                Map.of("conversationId", "conv-1", "conversationType", "single", "content", "hello bob", "contentType", "text"));
        StepVerifier.create(service.processChatMessage(alice, msg)).verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(1, storage.getHistory("conv-1", 10).size());
    }

    @Test
    void sendsGroupChatMessage() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        LocalMessageSender sender = new LocalMessageSender(registry);
        InMemoryChatStorage storage = new InMemoryChatStorage(100);
        ChatServiceImpl service = new ChatServiceImpl(sender, registry, storage, 2000);

        DefaultSessionConnection alice = new DefaultSessionConnection("s1", "alice", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(alice).then(registry.joinRoom("group-1", "s1"))).verifyComplete();

        WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "chat.send", null, null, "alice", null, "group-1", null,
                Map.of("conversationId", "group-1", "conversationType", "group", "content", "hello group"));
        StepVerifier.create(service.processChatMessage(alice, msg)).verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(1, storage.getHistory("group-1", 10).size());
    }

    @Test
    void tracksUnreadCount() {
        InMemoryChatStorage storage = new InMemoryChatStorage(100);
        storage.incrementUnread("conv-1", "bob");
        storage.incrementUnread("conv-1", "bob");
        org.junit.jupiter.api.Assertions.assertEquals(2, storage.getUnreadCount("conv-1", "bob"));
        storage.resetUnread("conv-1", "bob");
        org.junit.jupiter.api.Assertions.assertEquals(0, storage.getUnreadCount("conv-1", "bob"));
    }

    @Test
    void processesAck() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        LocalMessageSender sender = new LocalMessageSender(registry);
        InMemoryChatStorage storage = new InMemoryChatStorage(100);
        ChatServiceImpl service = new ChatServiceImpl(sender, registry, storage, 2000);

        DefaultSessionConnection bob = new DefaultSessionConnection("s2", "bob", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(bob)).verifyComplete();

        WsMessage<Map<String, Object>> ack = new WsMessage<>(null, "chat.ack", null, null, "bob", null, null, null,
                Map.of("messageId", "msg-123"));
        StepVerifier.create(service.processAck(bob, ack)).verifyComplete();
    }

    @Test
    void rejectsEmptyContent() {
        ChatServiceImpl service = createService();
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection alice = new DefaultSessionConnection("s1", "alice", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(alice)).verifyComplete();

        WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "chat.send", null, null, "alice", null, null, null,
                Map.of("conversationId", "conv-1", "content", ""));
        StepVerifier.create(service.processChatMessage(alice, msg)).verifyComplete();
    }

    @Test
    void rejectsContentTooLong() {
        InMemoryChatStorage storage = new InMemoryChatStorage(100);
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        LocalMessageSender sender = new LocalMessageSender(registry);
        ChatServiceImpl service = new ChatServiceImpl(sender, registry, storage, 10);

        DefaultSessionConnection alice = new DefaultSessionConnection("s1", "alice", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(alice)).verifyComplete();

        WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "chat.send", null, null, "alice", null, null, null,
                Map.of("conversationId", "conv-1", "content", "this message is too long"));
        StepVerifier.create(service.processChatMessage(alice, msg)).verifyComplete();
    }

    @Test
    void isChatMessageDetectsType() {
        ChatServiceImpl service = createService();
        WsMessage<Map<String, Object>> chatSend = new WsMessage<>(null, "chat.send", null, null, "u1", null, null, null, Map.of("content", "hi"));
        WsMessage<Map<String, Object>> chatAck = new WsMessage<>(null, "chat.ack", null, null, "u1", null, null, null, Map.of("messageId", "m1"));
        WsMessage<Map<String, Object>> other = new WsMessage<>(null, "echo", null, null, "u1", null, null, null, null);
        org.junit.jupiter.api.Assertions.assertTrue(service.isChatMessage(chatSend));
        org.junit.jupiter.api.Assertions.assertTrue(service.isChatMessage(chatAck));
        org.junit.jupiter.api.Assertions.assertFalse(service.isChatMessage(other));
        org.junit.jupiter.api.Assertions.assertFalse(service.isChatMessage(null));
    }

    @Test
    void getHistoryReturnsMessages() {
        InMemoryChatStorage storage = new InMemoryChatStorage(100);
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        LocalMessageSender sender = new LocalMessageSender(registry);
        ChatServiceImpl service = new ChatServiceImpl(sender, registry, storage, 2000);

        DefaultSessionConnection alice = new DefaultSessionConnection("s1", "alice", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(alice).then(registry.joinRoom("conv-1", "s1"))).verifyComplete();

        for (int i = 0; i < 5; i++) {
            WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "chat.send", null, null, "alice", null, null, null,
                    Map.of("conversationId", "conv-1", "conversationType", "group", "content", "msg-" + i));
            StepVerifier.create(service.processChatMessage(alice, msg)).verifyComplete();
        }

        StepVerifier.create(service.getHistory("conv-1", 3))
                .assertNext(list -> org.junit.jupiter.api.Assertions.assertEquals(3, list.size()))
                .verifyComplete();
    }
}
