package com.example.demo.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.demo.model.SafetyStatus;

public record ChatMessageResponse(
        UUID requestId,
        UUID sessionId,
        String answer,
        SafetyStatus safetyStatus,
        List<CitationResponse> citations,
        List<ToolCallResponse> toolCalls,
        Instant createdAt) {
    public ChatMessageResponse(
            UUID sessionId,
            String answer,
            SafetyStatus safetyStatus,
            List<CitationResponse> citations,
            List<ToolCallResponse> toolCalls,
            Instant createdAt) {
        this(null, sessionId, answer, safetyStatus, citations, toolCalls, createdAt);
    }
}
