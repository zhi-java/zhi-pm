package io.github.zhi.pm.core.broker;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageBroker {
    Mono<Void> publish(BrokerMessage message);
    Flux<BrokerMessage> subscribe();
}
