package com.example.demo.security;

public record LimitResult(
        boolean allowed,
        long current,
        int limit,
        int retryAfterSeconds) {
}
