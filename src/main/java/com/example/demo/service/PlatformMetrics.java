package com.example.demo.service;

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

@Component
public class PlatformMetrics {
    private final MeterRegistry meterRegistry;

    public PlatformMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordChat(String tenantSlug, String safetyStatus, Duration duration) {
        meterRegistry.timer("aif.chat.latency", "tenant", tenantSlug, "safety_status", safetyStatus)
                .record(duration);
    }

    public void recordAuthFailure(String reason) {
        meterRegistry.counter("aif.auth.failures", "reason", reason).increment();
    }

    public void recordRateLimit(String tenantSlug, String code) {
        meterRegistry.counter("aif.rate_limit.rejections", "tenant", tenantSlug, "code", code).increment();
    }

    public void recordToolOutcome(String tenantSlug, String toolName, String status) {
        meterRegistry.counter("aif.host_tool.outcomes", "tenant", tenantSlug, "tool", toolName, "status", status)
                .increment();
    }

    public void recordModelFailure(String tenantSlug) {
        meterRegistry.counter("aif.model.failures", "tenant", tenantSlug).increment();
    }
}
