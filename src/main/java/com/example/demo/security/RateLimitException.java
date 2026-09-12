package com.example.demo.security;

import org.springframework.http.HttpStatus;

import com.example.demo.exception.ApiException;

public class RateLimitException extends ApiException {
    private final int retryAfterSeconds;

    public RateLimitException(String code, String message, int retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
