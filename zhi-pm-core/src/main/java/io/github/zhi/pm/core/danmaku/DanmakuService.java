package io.github.zhi.pm.core.danmaku;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.session.SessionConnection;
import reactor.core.publisher.Mono;

public interface DanmakuService {

    boolean isDanmaku(WsMessage<?> message);

    Mono<Void> processDanmaku(SessionConnection connection, WsMessage<?> message);
}
