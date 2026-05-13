package io.github.zhi.pm.danmaku.limiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class TokenBucketRateLimiter {
    private final int maxTokens;
    private final long refillIntervalNanos;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int maxTokensPerSecond) {
        this.maxTokens = maxTokensPerSecond;
        this.refillIntervalNanos = 1_000_000_000L;
    }

    public boolean tryAcquire(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(maxTokens, System.nanoTime()));
        return bucket.tryAcquire(maxTokens, refillIntervalNanos);
    }

    private static class Bucket {
        private final AtomicLong tokens;
        private volatile long lastRefillNanos;

        Bucket(int initialTokens, long now) {
            this.tokens = new AtomicLong(initialTokens);
            this.lastRefillNanos = now;
        }

        synchronized boolean tryAcquire(int maxTokens, long refillIntervalNanos) {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed >= refillIntervalNanos) {
                long refill = elapsed / refillIntervalNanos;
                tokens.set(Math.min(maxTokens, tokens.get() + refill));
                lastRefillNanos = now;
            }
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
    }
}
