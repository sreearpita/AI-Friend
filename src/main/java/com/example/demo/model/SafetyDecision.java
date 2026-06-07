package com.example.demo.model;

public record SafetyDecision(
        SafetyStatus status,
        boolean shouldCallModel,
        String directResponse) {
}
