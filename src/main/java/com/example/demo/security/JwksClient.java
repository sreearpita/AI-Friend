package com.example.demo.security;

import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.demo.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class JwksClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedJwks> cache = new ConcurrentHashMap<>();

    public JwksClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(2))
                .build();
        this.objectMapper = objectMapper;
    }

    public PublicKey findRsaPublicKey(String jwksUri, String keyId, int cacheTtlSeconds) {
        JsonNode jwks = loadJwks(jwksUri, cacheTtlSeconds);
        JsonNode keys = jwks.path("keys");
        if (!keys.isArray()) {
            throw invalidToken("JWKS does not contain keys.");
        }
        for (JsonNode key : keys) {
            if (keyId.equals(key.path("kid").asText())) {
                return toPublicKey(key);
            }
        }
        throw invalidToken("JWT key id was not found.");
    }

    private JsonNode loadJwks(String jwksUri, int cacheTtlSeconds) {
        CachedJwks cached = cache.get(jwksUri);
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.jwks();
        }
        try {
            String body;
            if (jwksUri.startsWith("inline://")) {
                body = new String(Base64.getUrlDecoder().decode(jwksUri.substring("inline://".length())));
            } else {
                body = restTemplate.getForObject(URI.create(jwksUri), String.class);
            }
            JsonNode jwks = objectMapper.readTree(body);
            cache.put(jwksUri, new CachedJwks(jwks, now.plusSeconds(Math.max(1, cacheTtlSeconds))));
            return jwks;
        } catch (Exception exception) {
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            if (exception instanceof RestClientException || exception instanceof IllegalArgumentException) {
                throw invalidToken("JWKS could not be loaded.");
            }
            throw invalidToken("JWKS could not be parsed.");
        }
    }

    private PublicKey toPublicKey(JsonNode key) {
        try {
            if (!"RSA".equalsIgnoreCase(key.path("kty").asText())) {
                throw invalidToken("JWT key is not RSA.");
            }
            BigInteger modulus = unsignedUrlInteger(key.path("n").asText());
            BigInteger exponent = unsignedUrlInteger(key.path("e").asText());
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (Exception exception) {
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            throw invalidToken("JWT key could not be parsed.");
        }
    }

    private BigInteger unsignedUrlInteger(String value) {
        return new BigInteger(1, Base64.getUrlDecoder().decode(value));
    }

    private ApiException invalidToken(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_USER_CONTEXT", message);
    }

    private record CachedJwks(JsonNode jwks, Instant expiresAt) {
    }
}
