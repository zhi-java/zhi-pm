package io.github.zhi.pm.sample.danmaku;

import io.github.zhi.pm.core.danmaku.DanmakuService;
import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.danmaku.DanmakuServiceImpl;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/danmaku")
public class DanmakuController {
    private final ConnectionRegistry registry;
    private final MessageSender sender;
    private final DanmakuServiceImpl danmakuService;

    public DanmakuController(ConnectionRegistry registry, MessageSender sender, DanmakuService danmakuService) {
        this.registry = registry;
        this.sender = sender;
        this.danmakuService = (DanmakuServiceImpl) danmakuService;
    }

    @PostMapping("/rooms/{roomId}")
    public Mono<Map<String, Object>> sendDanmaku(@PathVariable("roomId") String roomId, @RequestParam("content") String content, @RequestParam(value = "userId", defaultValue = "system") String userId) {
        WsMessage<Map<String, Object>> message = new WsMessage<>(null, "danmaku.message", null, null, userId, null, roomId, Instant.now(), Map.of("content", content));
        return sender.sendToRoom(roomId, message).map(count -> Map.of("roomId", roomId, "sent", count));
    }

    @PostMapping("/rooms/{roomId}/mute/{userId}")
    public Map<String, Object> muteUser(@PathVariable("roomId") String roomId, @PathVariable("userId") String userId) {
        danmakuService.getMuteService().mute(roomId, userId);
        return Map.of("roomId", roomId, "userId", userId, "muted", true);
    }

    @PostMapping("/rooms/{roomId}/unmute/{userId}")
    public Map<String, Object> unmuteUser(@PathVariable("roomId") String roomId, @PathVariable("userId") String userId) {
        danmakuService.getMuteService().unmute(roomId, userId);
        return Map.of("roomId", roomId, "userId", userId, "muted", false);
    }

    @GetMapping("/rooms/{roomId}/muted")
    public Map<String, Object> getMutedUsers(@PathVariable("roomId") String roomId) {
        Set<String> muted = danmakuService.getMuteService().getMutedUsers(roomId);
        return Map.of("roomId", roomId, "mutedUsers", muted);
    }

    @GetMapping("/rooms/{roomId}/count")
    public Mono<Map<String, Object>> roomCount(@PathVariable("roomId") String roomId) {
        return registry.countRoomConnections(roomId).map(count -> Map.of("roomId", roomId, "count", count));
    }
}
