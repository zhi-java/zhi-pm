package io.github.zhi.pm.core.send;

import io.github.zhi.pm.core.message.WsMessage;
import java.util.Collection;
import reactor.core.publisher.Mono;

public interface MessageSender {
    Mono<Boolean> sendToUser(String userId, WsMessage<?> message);
    Mono<Integer> sendToUsers(Collection<String> userIds, WsMessage<?> message);
    Mono<Integer> sendToRoom(String roomId, WsMessage<?> message);
    Mono<Integer> broadcast(WsMessage<?> message);
}
