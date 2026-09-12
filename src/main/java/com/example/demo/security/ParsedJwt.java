package com.example.demo.security;

import com.fasterxml.jackson.databind.JsonNode;

public record ParsedJwt(
        String token,
        String algorithm,
        String keyId,
        JsonNode claims) {
}
