package com.example.demo.dto;

public record FlowelleUserPreferencesResponse(
        Integer cycleLength,
        Integer periodLength,
        Boolean birthControlUse,
        Boolean notificationsEnabled,
        Boolean aiCoachEnabled,
        Boolean voiceProcessingEnabled,
        Boolean analyticsOptIn,
        String summary,
        String userExplanation) {
}
