package io.github.zhi.pm.sample.basic;

import io.github.zhi.pm.core.message.WsMessage;
import io.github.zhi.pm.core.send.MessageSender;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/push")
public class PushController {
    private final MessageSender sender;

    public PushController(MessageSender sender) {
        this.sender = sender;
    }

    @PostMapping("/users/{userId}")
    public Mono<Map<String, Object>> pushToUser(@PathVariable("userId") String userId, @RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = new WsMessage<>(null, "sample.user-push", null, Instant.now(), payload == null ? Collections.emptyMap() : payload);
        return sender.sendToUser(userId, message).map(sent -> Collections.singletonMap("sent", sent));
    }

    @PostMapping("/broadcast")
    public Mono<Map<String, Object>> broadcast(@RequestBody(required = false) Map<String, Object> payload) {
        WsMessage<?> message = new WsMessage<>(null, "sample.broadcast", null, Instant.now(), payload == null ? Collections.emptyMap() : payload);
        return sender.broadcast(message).map(count -> Collections.singletonMap("sent", count));
    }
}
