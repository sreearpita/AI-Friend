package com.example.demo.security;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import com.example.demo.exception.ApiException;
import com.example.demo.model.TenantUserAuthConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtVerifier {
    private final ObjectMapper objectMapper;
    private final JwksClient jwksClient;

    public JwtVerifier(ObjectMapper objectMapper, JwksClient jwksClient) {
        this.objectMapper = objectMapper;
        this.jwksClient = jwksClient;
    }

    public ParsedJwt verify(String token, TenantUserAuthConfig config) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw invalidToken("JWT must have three segments.");
            }

            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            String algorithm = header.path("alg").asText();
            String keyId = header.path("kid").asText();
            if (!config.getAllowedAlgorithm().equals(algorithm) || !"RS256".equals(algorithm)) {
                throw invalidToken("JWT algorithm is not allowed.");
            }
            if (!StringUtils.hasText(keyId)) {
                throw invalidToken("JWT key id is required.");
            }

            PublicKey publicKey = jwksClient.findRsaPublicKey(config.getJwksUri(), keyId, config.getJwksCacheTtlSeconds());
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!signature.verify(Base64.getUrlDecoder().decode(parts[2]))) {
                throw invalidToken("JWT signature is invalid.");
            }

            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            validateStandardClaims(claims, config);
            return new ParsedJwt(token, algorithm, keyId, claims);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken("JWT could not be verified.");
        }
    }

    public ParsedJwt verifyAdmin(String token, String issuer, String audience, String jwksUri, int maxLifetimeSeconds) {
        TenantUserAuthConfig config = new TenantUserAuthConfig(null, issuer, audience, jwksUri, "RS256", maxLifetimeSeconds, 300, true);
        return verify(token, config);
    }

    private void validateStandardClaims(JsonNode claims, TenantUserAuthConfig config) {
        Instant now = Instant.now();
        String issuer = claims.path("iss").asText();
        if (!config.getIssuer().equals(issuer)) {
            throw invalidToken("JWT issuer is invalid.");
        }
        if (!containsString(claims.path("aud"), config.getAudience())) {
            throw invalidToken("JWT audience is invalid.");
        }
        long iat = requiredEpoch(claims, "iat");
        long exp = requiredEpoch(claims, "exp");
        if (exp <= now.getEpochSecond() || iat > now.plusSeconds(30).getEpochSecond()) {
            throw invalidToken("JWT timestamps are invalid.");
        }
        if (exp - iat > config.getMaxTokenLifetimeSeconds()) {
            throw invalidToken("JWT lifetime is too long.");
        }
        if (!StringUtils.hasText(claims.path("sub").asText())
                || !StringUtils.hasText(claims.path("jti").asText())) {
            throw invalidToken("JWT subject and id are required.");
        }
    }

    private long requiredEpoch(JsonNode claims, String claimName) {
        JsonNode value = claims.path(claimName);
        if (!value.canConvertToLong()) {
            throw invalidToken("JWT " + claimName + " is required.");
        }
        return value.asLong();
    }

    private boolean containsString(JsonNode node, String expected) {
        if (node.isTextual()) {
            return expected.equals(node.asText());
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (expected.equals(item.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private ApiException invalidToken(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_USER_CONTEXT", message);
    }
}
