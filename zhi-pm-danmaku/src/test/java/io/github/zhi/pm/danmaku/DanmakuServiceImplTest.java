package io.github.zhi.pm.danmaku;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.InMemoryConnectionRegistry;
import io.github.zhi.pm.core.send.LocalMessageSender;
import io.github.zhi.pm.core.session.DefaultSessionConnection;
import io.github.zhi.pm.danmaku.filter.ContentFilter;
import io.github.zhi.pm.danmaku.limiter.RateLimiter;
import io.github.zhi.pm.danmaku.limiter.TokenBucketRateLimiter;
import io.github.zhi.pm.danmaku.mute.InMemoryMuteService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DanmakuServiceImplTest {

    private DanmakuServiceImpl createService(int userLimit, int roomLimit, List<String> sensitiveWords) {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        LocalMessageSender sender = new LocalMessageSender(registry);
        ContentFilter filter = new ContentFilter(sensitiveWords);
        InMemoryMuteService muteService = new InMemoryMuteService();
        RateLimiter userLimiter = new TokenBucketRateLimiter(userLimit);
        RateLimiter roomLimiter = new TokenBucketRateLimiter(roomLimit);
        return new DanmakuServiceImpl(sender, registry, filter, muteService, 100, userLimiter, roomLimiter);
    }

    @Test
    void sendsDanmakuToRoom() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        LocalMessageSender sender = new LocalMessageSender(registry);
        ContentFilter filter = new ContentFilter(Collections.emptyList());
        InMemoryMuteService muteService = new InMemoryMuteService();
        RateLimiter userLimiter = new TokenBucketRateLimiter(10);
        RateLimiter roomLimiter = new TokenBucketRateLimiter(5000);
        DanmakuServiceImpl service = new DanmakuServiceImpl(sender, registry, filter, muteService, 100, userLimiter, roomLimiter);

        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn).then(registry.joinRoom("live1", "s1"))).verifyComplete();

        WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "danmaku.send", null, null, "u1", null, "live1", null, Map.of("content", "hello"));
        StepVerifier.create(service.processDanmaku(conn, msg)).verifyComplete();
    }

    @Test
    void blocksMutedUser() {
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        LocalMessageSender sender = new LocalMessageSender(registry);
        ContentFilter filter = new ContentFilter(Collections.emptyList());
        InMemoryMuteService muteService = new InMemoryMuteService();
        RateLimiter userLimiter = new TokenBucketRateLimiter(10);
        RateLimiter roomLimiter = new TokenBucketRateLimiter(5000);
        DanmakuServiceImpl service = new DanmakuServiceImpl(sender, registry, filter, muteService, 100, userLimiter, roomLimiter);

        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn).then(registry.joinRoom("live1", "s1"))).verifyComplete();

        muteService.mute("live1", "u1");
        WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "danmaku.send", null, null, "u1", null, "live1", null, Map.of("content", "hello"));
        StepVerifier.create(service.processDanmaku(conn, msg)).verifyComplete();
    }

    @Test
    void blocksSensitiveContent() {
        DanmakuServiceImpl service = createService(10, 5000, List.of("spam"));
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn).then(registry.joinRoom("live1", "s1"))).verifyComplete();

        WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "danmaku.send", null, null, "u1", null, "live1", null, Map.of("content", "this is spam"));
        StepVerifier.create(service.processDanmaku(conn, msg)).verifyComplete();
    }

    @Test
    void rateLimitsUser() {
        DanmakuServiceImpl service = createService(1, 5000, Collections.emptyList());
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn).then(registry.joinRoom("live1", "s1"))).verifyComplete();

        WsMessage<Map<String, Object>> msg1 = new WsMessage<>(null, "danmaku.send", null, null, "u1", null, "live1", null, Map.of("content", "first"));
        WsMessage<Map<String, Object>> msg2 = new WsMessage<>(null, "danmaku.send", null, null, "u1", null, "live1", null, Map.of("content", "second"));
        StepVerifier.create(service.processDanmaku(conn, msg1)).verifyComplete();
        StepVerifier.create(service.processDanmaku(conn, msg2)).verifyComplete();
    }

    @Test
    void rejectsMissingRoomId() {
        DanmakuServiceImpl service = createService(10, 5000, Collections.emptyList());
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn)).verifyComplete();

        WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "danmaku.send", null, null, "u1", null, null, null, Map.of("content", "hello"));
        StepVerifier.create(service.processDanmaku(conn, msg)).verifyComplete();
    }

    @Test
    void rejectsContentTooLong() {
        DanmakuServiceImpl service = createService(10, 5000, Collections.emptyList());
        InMemoryConnectionRegistry registry = new InMemoryConnectionRegistry();
        DefaultSessionConnection conn = new DefaultSessionConnection("s1", "u1", Collections.emptyMap(), 64, reason -> Mono.empty());
        StepVerifier.create(registry.register(conn).then(registry.joinRoom("live1", "s1"))).verifyComplete();

        String longContent = "a".repeat(101);
        WsMessage<Map<String, Object>> msg = new WsMessage<>(null, "danmaku.send", null, null, "u1", null, "live1", null, Map.of("content", longContent));
        StepVerifier.create(service.processDanmaku(conn, msg)).verifyComplete();
    }

    @Test
    void isDanmakuDetectsType() {
        DanmakuServiceImpl service = createService(10, 5000, Collections.emptyList());
        WsMessage<Map<String, Object>> danmaku = new WsMessage<>(null, "danmaku.send", null, null, "u1", null, "live1", null, Map.of("content", "hi"));
        WsMessage<Map<String, Object>> other = new WsMessage<>(null, "chat.message", null, null, "u1", null, "live1", null, Map.of("content", "hi"));
        assert service.isDanmaku(danmaku);
        assert !service.isDanmaku(other);
        assert !service.isDanmaku(null);
    }
}
