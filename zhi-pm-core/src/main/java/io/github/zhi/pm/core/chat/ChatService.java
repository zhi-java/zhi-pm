package io.github.zhi.pm.core.chat;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.session.SessionConnection;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatService {

    boolean isChatMessage(WsMessage<?> message);

    Mono<Void> processChatMessage(SessionConnection connection, WsMessage<?> message);

    Mono<Void> processAck(SessionConnection connection, WsMessage<?> message);

    Mono<Void> processReadReceipt(SessionConnection connection, WsMessage<?> message);

    Mono<List<Map<String, Object>>> getHistory(String conversationId, int limit);

    Mono<Long> getUnreadCount(String conversationId, String userId);

    Mono<Void> drainOfflineMessages(String userId);
}
