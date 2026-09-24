package com.example.demo.dto;

import java.util.List;

public record FlowelleWellnessSignalsResponse(String windowStart, String windowEnd,
        List<DailyEvent> dailyEvents, List<SymptomEvent> symptomEvents, String summary, String userExplanation) {
    public record DailyEvent(String date, String periodFlow, String mood, Integer energy) {}
    public record SymptomEvent(String date, String type, Integer severity) {}
}
