package com.example.demo.security;

import java.time.Duration;

public interface DistributedStateStore {
    boolean putIfAbsent(String key, String value, Duration ttl);

    LimitResult incrementAndCheck(String key, Duration window, int limit);
}
