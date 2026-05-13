package io.github.zhi.pm.sample.chat;

import io.github.zhi.pm.core.chat.ChatService;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatRoomController {
    private final ChatService chatService;
    private final ConnectionRegistry registry;

    public ChatRoomController(ChatService chatService, ConnectionRegistry registry) {
        this.chatService = chatService;
        this.registry = registry;
    }

    @GetMapping("/conversations/{conversationId}/history")
    public Mono<Map<String, Object>> getHistory(@PathVariable("conversationId") String conversationId,
                                                 @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return chatService.getHistory(conversationId, limit)
                .map(messages -> Map.of("conversationId", conversationId, "messages", messages, "count", messages.size()));
    }

    @GetMapping("/conversations/{conversationId}/unread/{userId}")
    public Mono<Map<String, Object>> getUnreadCount(@PathVariable("conversationId") String conversationId,
                                                     @PathVariable("userId") String userId) {
        return chatService.getUnreadCount(conversationId, userId)
                .map(count -> Map.of("conversationId", conversationId, "userId", userId, "unreadCount", count));
    }

    @GetMapping("/rooms/{roomId}/count")
    public Mono<Map<String, Object>> roomCount(@PathVariable("roomId") String roomId) {
        return registry.countRoomConnections(roomId).map(count -> Map.of("roomId", roomId, "count", count));
    }
}
