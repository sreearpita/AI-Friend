package com.example.demo.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank @Size(max = 128) String externalUserId,
        UUID sessionId,
        @NotBlank @Size(max = 4000) String message,
        @Size(max = 20) String locale,
        Set<@Size(max = 80) String> scopes) {
}
