package io.github.zhi.pm.danmaku.limiter;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.List;
import reactor.core.publisher.Mono;

public class RedisRateLimiter implements RateLimiter {
    private static final RedisScript<Long> SLIDING_WINDOW_SCRIPT = RedisScript.of(
            """
            local key = KEYS[1]
            local window_ms = tonumber(ARGV[1])
            local max_count = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local window_start = now - window_ms
            redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)
            local count = redis.call('ZCARD', key)
            if count < max_count then
                redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
                redis.call('PEXPIRE', key, window_ms)
                return 1
            end
            return 0
            """, Long.class);

    private final ReactiveRedisTemplate<String, String> redis;
    private final String prefix;
    private final long windowMs;
    private final int maxCount;

    public RedisRateLimiter(ReactiveRedisTemplate<String, String> redis, String prefix,
                            int maxCountPerSecond) {
        this.redis = redis;
        this.prefix = prefix.endsWith(":") ? prefix : prefix + ":";
        this.windowMs = 1000;
        this.maxCount = maxCountPerSecond;
    }

    @Override
    public Mono<Boolean> tryAcquire(String key) {
        String redisKey = prefix + "rate_limit:" + key;
        return redis.execute(
                SLIDING_WINDOW_SCRIPT,
                List.of(redisKey),
                List.of(String.valueOf(windowMs), String.valueOf(maxCount), String.valueOf(System.currentTimeMillis()))
        ).next()
         .map(result -> result != null && result == 1L)
         .defaultIfEmpty(false);
    }
}
