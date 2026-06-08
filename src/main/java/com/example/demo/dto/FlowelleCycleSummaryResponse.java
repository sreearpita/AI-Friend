package com.example.demo.dto;

public record FlowelleCycleSummaryResponse(
        String nextPeriod,
        String fertileWindowStart,
        String fertileWindowEnd,
        String ovulationDay,
        Integer confidence,
        String basis,
        Boolean isPredicted,
        Integer cycleLength,
        Integer periodLength,
        String summary,
        String userExplanation) {
}
