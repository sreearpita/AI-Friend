package com.example.demo.dto;

import java.util.Map;

public record HostToolResponse(
        String toolName,
        String status,
        String summary,
        Map<String, Object> facts,
        String userExplanation) {
}
