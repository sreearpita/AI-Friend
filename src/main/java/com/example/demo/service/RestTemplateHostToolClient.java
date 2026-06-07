package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.dto.HostToolRequest;
import com.example.demo.dto.HostToolResponse;
import com.example.demo.model.HostToolClientException;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.security.HostToolSigner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class RestTemplateHostToolClient implements HostToolClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HostToolSigner hostToolSigner;

    public RestTemplateHostToolClient(
            RestTemplateBuilder restTemplateBuilder,
            AiFriendProperties properties,
            ObjectMapper objectMapper,
            HostToolSigner hostToolSigner) {
        Duration timeout = Duration.ofMillis(properties.getTools().getRequestTimeoutMs());
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
        this.objectMapper = objectMapper;
        this.hostToolSigner = hostToolSigner;
    }

    @Override
    public HostToolResponse invoke(TenantToolConfig toolConfig, HostToolRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            String timestamp = Instant.now().toString();
            String signature = hostToolSigner.sign(timestamp, body, toolConfig.getSigningSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HostToolSigner.TENANT_HEADER, request.tenantSlug());
            headers.set(HostToolSigner.TIMESTAMP_HEADER, timestamp);
            headers.set(HostToolSigner.SIGNATURE_HEADER, signature);
            headers.set(HostToolSigner.REQUEST_ID_HEADER, request.requestId().toString());

            ResponseEntity<String> response = restTemplate.postForEntity(
                    toolConfig.getCallbackUrl(),
                    new HttpEntity<>(body, headers),
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new HostToolClientException("Host tool returned a non-success response", null);
            }
            return objectMapper.readValue(response.getBody(), HostToolResponse.class);
        } catch (JsonProcessingException | RestClientException exception) {
            throw new HostToolClientException("Host tool callback failed", exception);
        }
    }
}
