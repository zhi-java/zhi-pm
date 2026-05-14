package io.github.zhi.pm.danmaku.limiter;

public interface RateLimiter {
    boolean tryAcquire(String key);
}
