package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.dto.CitationResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class RetrievalServiceIntegrationTest {
    @Autowired
    private RetrievalService retrievalService;

    @Test
    void returnsRelevantCitationsForPmsNutritionPrompt() {
        List<CitationResponse> citations = retrievalService.findRelevantCitations(
                "What food and hydration can help with PMS?");

        assertThat(citations)
                .extracting(CitationResponse::title)
                .contains("PMS nutrition and hydration basics");
    }

    @Test
    void returnsNoCitationsForUnrelatedPrompt() {
        List<CitationResponse> citations = retrievalService.findRelevantCitations(
                "Tell me a joke about a spaceship");

        assertThat(citations).isEmpty();
    }

    @Test
    void promptContextsIncludeBoundedChunkTextAndSource() {
        List<String> contexts = retrievalService.findPromptContexts("Can I do light exercise during my period?");

        assertThat(contexts)
                .anySatisfy(context -> assertThat(context)
                        .contains("Gentle exercise during PMS or period")
                        .contains("Relevant guidance:")
                        .contains("walking"));
    }
}
