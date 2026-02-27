package com.example.demo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

/**
 * Low-level service that calls the Ollama /api/chat REST endpoint directly.
 * Properties use the app.ollama.* namespace (Spring AI dependency was removed).
 */
@Service
public class OllamaService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);

    private final RestTemplate restTemplate;

    @Value("${app.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.model:mistral}")
    private String model;

    public OllamaService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }

    /** Calls Ollama and returns the assistant's reply text. */
    public String chat(List<OllamaMessage> messages) {
        String url = ollamaBaseUrl + "/api/chat";
        OllamaChatRequest request = new OllamaChatRequest(model, messages, false);
        logger.debug("Calling Ollama at {} with {} messages", url, messages.size());
        OllamaChatResponse response = restTemplate.postForObject(url, request, OllamaChatResponse.class);
        if (response == null || response.message() == null) {
            throw new RuntimeException("Empty response from Ollama");
        }
        return response.message().content();
    }

    // ---- request / response records ----

    public record OllamaMessage(String role, String content) {}

    record OllamaChatRequest(String model, List<OllamaMessage> messages, boolean stream) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaChatResponse(OllamaMessage message, Boolean done) {}
}
