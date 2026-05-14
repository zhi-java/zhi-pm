package io.github.zhi.pm.danmaku.limiter;

import reactor.core.publisher.Mono;

public interface RateLimiter {
    Mono<Boolean> tryAcquire(String key);
}
