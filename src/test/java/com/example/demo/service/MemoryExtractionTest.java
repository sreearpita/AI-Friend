package com.example.demo.service;

import com.example.demo.repository.MemoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MemoryExtractionTest {

    private MemoryService memoryService;

    @BeforeEach
    void setUp() {
        memoryService = new MemoryService(
                mock(MemoryRepository.class),
                mock(OllamaService.class),
                new ObjectMapper());
    }

    @Test
    void parseValidMemoriesJson() {
        String json = "{\"memories\":[{\"fact\":\"User loves hiking\",\"category\":\"preference\",\"confidence\":0.9}]}";
        List<MemoryService.ExtractedMemory> result = memoryService.parseExtractorResponse(json);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).fact).isEqualTo("User loves hiking");
        assertThat(result.get(0).category).isEqualTo("preference");
        assertThat(result.get(0).confidence).isEqualTo(0.9f);
    }

    @Test
    void parseEmptyMemoriesArray() {
        String json = "{\"memories\":[]}";
        List<MemoryService.ExtractedMemory> result = memoryService.parseExtractorResponse(json);
        assertThat(result).isEmpty();
    }

    @Test
    void parseMultipleMemories() {
        String json = "{\"memories\":["
                + "{\"fact\":\"User is 30 years old\",\"category\":\"profile\",\"confidence\":0.95},"
                + "{\"fact\":\"User likes coffee\",\"category\":\"preference\",\"confidence\":0.85}"
                + "]}";
        List<MemoryService.ExtractedMemory> result = memoryService.parseExtractorResponse(json);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).fact).isEqualTo("User is 30 years old");
        assertThat(result.get(1).fact).isEqualTo("User likes coffee");
    }

    @Test
    void parseResponseWithExtraText() {
        String raw = "Sure! Here is the JSON:\n{\"memories\":[{\"fact\":\"User is a developer\",\"category\":\"profile\",\"confidence\":0.8}]}\nDone.";
        List<MemoryService.ExtractedMemory> result = memoryService.parseExtractorResponse(raw);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).fact).isEqualTo("User is a developer");
    }

    @Test
    void parseInvalidJsonReturnsEmpty() {
        String raw = "This is not JSON at all";
        List<MemoryService.ExtractedMemory> result = memoryService.parseExtractorResponse(raw);
        assertThat(result).isEmpty();
    }

    @Test
    void parseMissingMemoriesKeyReturnsEmpty() {
        String json = "{\"data\":[]}";
        List<MemoryService.ExtractedMemory> result = memoryService.parseExtractorResponse(json);
        assertThat(result).isEmpty();
    }
}
