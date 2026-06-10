package com.example.demo.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.dto.CitationResponse;
import com.example.demo.model.ContentChunk;
import com.example.demo.repository.ContentChunkRepository;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RetrievalService {
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "can", "for", "from", "how",
            "i", "in", "is", "it", "me", "my", "of", "on", "or", "the", "to", "what",
            "when", "with", "you", "your");

    private final AiFriendProperties properties;
    private final ContentChunkRepository contentChunkRepository;

    public RetrievalService(AiFriendProperties properties, ContentChunkRepository contentChunkRepository) {
        this.properties = properties;
        this.contentChunkRepository = contentChunkRepository;
    }

    public List<CitationResponse> findRelevantCitations(String message) {
        if (!properties.getRetrieval().isEnabled() || !StringUtils.hasText(message)) {
            return List.of();
        }

        List<String> queryTerms = queryTerms(message);
        if (queryTerms.isEmpty()) {
            return List.of();
        }

        return contentChunkRepository.findApprovedActiveChunksWithSource().stream()
                .map(chunk -> new RankedChunk(chunk, score(chunk, queryTerms)))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(RankedChunk::score).reversed())
                .limit(Math.max(0, properties.getRetrieval().getMaxCitations()))
                .map(result -> toCitation(result.chunk()))
                .toList();
    }

    public List<String> findPromptContexts(String message) {
        if (!properties.getRetrieval().isEnabled() || !StringUtils.hasText(message)) {
            return List.of();
        }

        List<String> queryTerms = queryTerms(message);
        if (queryTerms.isEmpty()) {
            return List.of();
        }

        return contentChunkRepository.findApprovedActiveChunksWithSource().stream()
                .map(chunk -> new RankedChunk(chunk, score(chunk, queryTerms)))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(RankedChunk::score).reversed())
                .limit(Math.max(0, properties.getRetrieval().getMaxCitations()))
                .map(result -> promptContext(result.chunk()))
                .toList();
    }

    private List<String> queryTerms(String message) {
        int minLength = Math.max(1, properties.getRetrieval().getMinQueryLength());
        return Arrays.stream(message.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(term -> term.length() >= minLength)
                .filter(term -> !STOP_WORDS.contains(term))
                .distinct()
                .collect(Collectors.toList());
    }

    private int score(ContentChunk chunk, List<String> queryTerms) {
        String haystack = (chunk.getTopic() + " " + chunk.getKeywords() + " " + chunk.getChunkText())
                .toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : queryTerms) {
            if (haystack.contains(term)) {
                score++;
            }
        }
        return score;
    }

    private CitationResponse toCitation(ContentChunk chunk) {
        return new CitationResponse(
                chunk.getSource().getTitle(),
                chunk.getSource().getPublisher(),
                chunk.getSource().getUrl());
    }

    private String promptContext(ContentChunk chunk) {
        return "Source: " + chunk.getSource().getTitle()
                + " (" + chunk.getSource().getPublisher() + ", " + chunk.getSource().getUrl() + ")"
                + "\nRelevant guidance: " + chunk.getChunkText();
    }

    private record RankedChunk(ContentChunk chunk, int score) {
    }
}
