package com.example.demo.dto;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCapabilityRequest(
        @NotBlank @Size(max = 160) String displayName,
        @NotBlank @Size(max = 500) String description,
        @Size(max = 20) List<@NotBlank @Size(max = 120) String> triggerPhrases,
        @Size(max = 20) Set<@NotBlank @Size(max = 80) String> requiredScopes,
        @Size(max = 20) List<@NotBlank @Size(max = 80) String> toolNames,
        @Size(max = 20) Set<@NotBlank @Size(max = 100) String> retrievalTopics,
        int priority,
        boolean active) {
}
