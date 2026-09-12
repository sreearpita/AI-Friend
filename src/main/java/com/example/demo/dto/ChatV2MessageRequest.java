package com.example.demo.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatV2MessageRequest(
        UUID sessionId,
        @NotBlank @Size(max = 4000) String message,
        @Size(max = 20) String locale) {
}
