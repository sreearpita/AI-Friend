package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.model.Memory;
import com.example.demo.repository.MemoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MemoryService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryService.class);

    private static final String EXTRACTOR_SYSTEM_PROMPT =
            "You are a memory extraction assistant. Extract personal facts about the user from the message.\n"
            + "Return ONLY a valid JSON object with this exact format:\n"
            + "{\"memories\":[{\"fact\":\"...\",\"category\":\"...\",\"confidence\":0.9}]}\n"
            + "Categories: profile, preference, habit, relationship, goal, other.\n"
            + "If no facts found, return {\"memories\":[]}\n"
            + "Do NOT include any explanation or text outside the JSON.";

    private final MemoryRepository memoryRepository;
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;

    @Value("${app.memory.top-k:10}")
    private int topK;

    public MemoryService(MemoryRepository memoryRepository,
                         OllamaService ollamaService,
                         ObjectMapper objectMapper) {
        this.memoryRepository = memoryRepository;
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the top-K most recently updated memories for a user.
     */
    public List<Memory> getTopMemories(UUID userId, int limit) {
        return memoryRepository.findTopByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(0, limit));
    }

    public List<Memory> getTopMemories(UUID userId) {
        return getTopMemories(userId, topK);
    }

    /**
     * Calls the LLM to extract facts from a message and upserts them into the DB.
     * Runs asynchronously so it does not add latency to the chat response.
     */
    @Async
    @Transactional
    public void extractAndSave(AppUser user, String userMessage) {
        try {
            String raw = callExtractor(userMessage);
            List<ExtractedMemory> extracted = parseExtractorResponse(raw);
            for (ExtractedMemory em : extracted) {
                if (em.fact != null && !em.fact.isBlank()) {
                    upsertMemory(user, em);
                }
            }
        } catch (Exception e) {
            logger.warn("Memory extraction failed for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private String callExtractor(String userMessage) {
        List<OllamaService.OllamaMessage> messages = List.of(
                new OllamaService.OllamaMessage("system", EXTRACTOR_SYSTEM_PROMPT),
                new OllamaService.OllamaMessage("user", userMessage)
        );
        return ollamaService.chat(messages);
    }

    /**
     * Parses the extractor JSON response. Package-private for testing.
     */
    List<ExtractedMemory> parseExtractorResponse(String raw) {
        try {
            // Find the JSON object within the response (LLM may include extra text)
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start < 0 || end < 0) {
                logger.debug("No JSON object found in extractor response");
                return List.of();
            }
            String json = raw.substring(start, end + 1);
            JsonNode root = objectMapper.readTree(json);
            JsonNode memoriesNode = root.get("memories");
            if (memoriesNode == null || !memoriesNode.isArray()) {
                return List.of();
            }
            return objectMapper.readerForListOf(ExtractedMemory.class).readValue(memoriesNode);
        } catch (Exception e) {
            logger.warn("Failed to parse extractor response: {}", e.getMessage());
            return List.of();
        }
    }

    private void upsertMemory(AppUser user, ExtractedMemory em) {
        memoryRepository.findByUserIdAndFactIgnoreCase(user.getId(), em.fact)
                .ifPresentOrElse(existing -> {
                    existing.setConfidence(em.confidence);
                    existing.setLastConfirmedAt(LocalDateTime.now());
                    memoryRepository.save(existing);
                }, () -> {
                    Memory memory = new Memory();
                    memory.setId(UUID.randomUUID());
                    memory.setUser(user);
                    memory.setFact(em.fact);
                    memory.setCategory(em.category);
                    memory.setConfidence(em.confidence);
                    memoryRepository.save(memory);
                });
    }

    /** Simple POJO for deserialized memory facts from the extractor LLM. */
    static class ExtractedMemory {
        public String fact;
        public String category;
        public Float confidence;
    }
}
