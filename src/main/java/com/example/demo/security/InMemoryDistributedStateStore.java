package com.example.demo.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.example.demo.config.AiFriendProperties;

import org.springframework.stereotype.Component;

@Component
public class InMemoryDistributedStateStore implements DistributedStateStore {
    private final AiFriendProperties properties;
    private final Map<String, ExpiringValue> values = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final RedisRespClient redisRespClient;

    public InMemoryDistributedStateStore(AiFriendProperties properties, RedisRespClient redisRespClient) {
        this.properties = properties;
        this.redisRespClient = redisRespClient;
    }

    @Override
    public boolean putIfAbsent(String key, String value, Duration ttl) {
        if (properties.getRedis().isEnabled()) {
            return redisRespClient.putIfAbsent(key, value, ttl);
        }
        Instant now = Instant.now();
        values.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        return values.putIfAbsent(key, new ExpiringValue(value, now.plus(ttl))) == null;
    }

    @Override
    public LimitResult incrementAndCheck(String key, Duration window, int limit) {
        if (properties.getRedis().isEnabled()) {
            return redisRespClient.incrementAndCheck(key, window, limit);
        }
        Instant now = Instant.now();
        Counter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAt().isBefore(now)) {
                return new Counter(new AtomicLong(1), now.plus(window));
            }
            existing.value().incrementAndGet();
            return existing;
        });
        long current = counter.value().get();
        int retryAfter = Math.max(1, (int) Duration.between(now, counter.expiresAt()).toSeconds());
        return new LimitResult(current <= limit, current, limit, retryAfter);
    }

    private record ExpiringValue(String value, Instant expiresAt) {
    }

    private record Counter(AtomicLong value, Instant expiresAt) {
    }
}
