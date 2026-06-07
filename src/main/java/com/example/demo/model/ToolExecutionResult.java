package com.example.demo.model;

import java.util.List;

import com.example.demo.dto.ToolCallResponse;

public record ToolExecutionResult(
        List<ToolCallResponse> toolCalls,
        List<String> promptContexts) {
    public static ToolExecutionResult empty() {
        return new ToolExecutionResult(List.of(), List.of());
    }
}
