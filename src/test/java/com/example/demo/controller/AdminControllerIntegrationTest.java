package com.example.demo.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.example.demo.model.ModelClient;
import com.example.demo.repository.ApiKeyRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {
    private static final String ISSUER = "https://admin.example";
    private static final String KEY_ID = "admin-test-key";
    private static KeyPair keyPair;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @MockBean
    private ModelClient modelClient;

    @BeforeAll
    static void beforeAll() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @DynamicPropertySource
    static void adminProperties(DynamicPropertyRegistry registry) {
        registry.add("aif.admin.issuer", () -> ISSUER);
        registry.add("aif.admin.audience", () -> "ai-friend-admin");
        registry.add("aif.admin.jwks-uri", () -> inlineJwks((RSAPublicKey) keyPair.getPublic()));
        registry.add("aif.admin.required-role", () -> "aif-admin");
    }

    @Test
    void adminCanCreateListAndRevokeApiKeyWithoutRedisclosingSecret() throws Exception {
        String tenantSlug = "tenant-" + UUID.randomUUID();
        String bearer = "Bearer " + token("admin-user", List.of("aif-admin"));

        mockMvc.perform(put("/internal/admin/tenants/{tenantSlug}", tenantSlug)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "%s",
                                  "displayName": "Flowelle",
                                  "active": true
                                }
                                """.formatted(tenantSlug)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(tenantSlug));

        MvcResult created = mockMvc.perform(post("/internal/admin/tenants/{tenantSlug}/api-keys", tenantSlug)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Flowelle server key"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key", startsWith("aif_live_")))
                .andExpect(jsonPath("$.keyPrefix").isNotEmpty())
                .andReturn();

        UUID keyId = UUID.fromString(JsonMapper.builder().build()
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText());

        mockMvc.perform(get("/internal/admin/tenants/{tenantSlug}/api-keys", tenantSlug)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").doesNotExist())
                .andExpect(jsonPath("$[0].keyPrefix").isNotEmpty());

        mockMvc.perform(delete("/internal/admin/tenants/{tenantSlug}/api-keys/{keyId}", tenantSlug, keyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(apiKeyRepository.findById(keyId).orElseThrow().getRevokedAt())
                .isNotNull();
    }

    @Test
    void adminTokenWithoutRoleIsRejected() throws Exception {
        mockMvc.perform(get("/internal/admin/tenants/demo/api-keys")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("admin-user", List.of("viewer"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTH_INSUFFICIENT_ROLE"));
    }

    @Test
    void adminRequestWithoutBearerTokenIsRejected() throws Exception {
        mockMvc.perform(get("/internal/admin/tenants/demo/api-keys"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTH_MISSING_TOKEN"));
    }

    private static String token(String subject, List<String> roles) throws Exception {
        long now = Instant.now().getEpochSecond();
        String header = "{\"alg\":\"RS256\",\"kid\":\"%s\",\"typ\":\"JWT\"}".formatted(KEY_ID);
        String roleJson = roles.stream().map(role -> "\"" + role + "\"").reduce((left, right) -> left + "," + right).orElse("");
        String claims = """
                {"iss":"%s","sub":"%s","aud":"ai-friend-admin","iat":%d,"exp":%d,"jti":"%s","roles":[%s]}
                """.formatted(ISSUER, subject, now, now + 300, "jti-" + UUID.randomUUID(), roleJson).trim();
        String signingInput = base64Url(header.getBytes(StandardCharsets.UTF_8))
                + "."
                + base64Url(claims.getBytes(StandardCharsets.UTF_8));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + base64Url(signature.sign());
    }

    private static String inlineJwks(RSAPublicKey publicKey) {
        String jwks = """
                {"keys":[{"kty":"RSA","kid":"%s","alg":"RS256","use":"sig","n":"%s","e":"%s"}]}
                """.formatted(
                KEY_ID,
                base64Url(publicKey.getModulus().toByteArray()),
                base64Url(publicKey.getPublicExponent().toByteArray())).trim();
        return "inline://" + base64Url(jwks.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stripLeadingZero(value));
    }

    private static byte[] stripLeadingZero(byte[] value) {
        if (value.length > 1 && value[0] == 0) {
            return java.util.Arrays.copyOfRange(value, 1, value.length);
        }
        return value;
    }
}
