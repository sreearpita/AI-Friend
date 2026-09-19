package com.example.demo.integration;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
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

import com.example.demo.controller.ChatController;
import com.example.demo.model.ModelClient;
import com.example.demo.repository.ApiKeyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmbeddedChatFunctionalIntegrationTest {
    private static final String ADMIN_ISSUER = "https://admin.functional.test";
    private static final String ADMIN_AUDIENCE = "ai-friend-admin";
    private static final String ADMIN_KID = "functional-admin-key";
    private static KeyPair adminKeyPair;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @MockBean
    private ModelClient modelClient;

    @BeforeAll
    static void beforeAll() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        adminKeyPair = generator.generateKeyPair();
    }

    @DynamicPropertySource
    static void adminProperties(DynamicPropertyRegistry registry) {
        registry.add("aif.admin.issuer", () -> ADMIN_ISSUER);
        registry.add("aif.admin.audience", () -> ADMIN_AUDIENCE);
        registry.add("aif.admin.jwks-uri", () -> inlineJwks(ADMIN_KID, (RSAPublicKey) adminKeyPair.getPublic()));
        registry.add("aif.admin.required-role", () -> "aif-admin");
    }

    @Test
    void provisionsTenantThenCompletesEmbeddedV2Chat() throws Exception {
        String tenantSlug = "functional-" + UUID.randomUUID();
        KeyPair userKeyPair = generateKeyPair();
        String userIssuer = "https://flowelle.functional.test";
        String userKid = "functional-user-key";
        String adminBearer = "Bearer " + adminToken("functional-admin");

        mockMvc.perform(put("/internal/admin/tenants/{tenantSlug}", tenantSlug)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "%s",
                                  "displayName": "Functional Flowelle",
                                  "active": true
                                }
                                """.formatted(tenantSlug)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/internal/admin/tenants/{tenantSlug}/user-auth", tenantSlug)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "issuer": "%s",
                                  "audience": "flowelle-chat",
                                  "jwksUri": "%s",
                                  "allowedAlgorithm": "RS256",
                                  "maxTokenLifetimeSeconds": 300,
                                  "jwksCacheTtlSeconds": 300,
                                  "active": true
                                }
                                """.formatted(userIssuer, inlineJwks(userKid, (RSAPublicKey) userKeyPair.getPublic()))))
                .andExpect(status().isOk());

        MvcResult apiKeyResult = mockMvc.perform(post("/internal/admin/tenants/{tenantSlug}/api-keys", tenantSlug)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"functional chat key\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(startsWith("aif_live_")))
                .andReturn();
        String tenantApiKey = objectMapper.readTree(apiKeyResult.getResponse().getContentAsString()).get("key").asText();

        when(modelClient.generate(anyList())).thenReturn("Your next-period estimate is available from Flowelle.");
        String userToken = userToken(userKeyPair, userIssuer, userKid, tenantSlug, "flowelle-user-42");

        MvcResult chatResult = mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", tenantApiKey)
                        .header(ChatController.USER_CONTEXT_HEADER, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"When is my next period?\",\"locale\":\"en-US\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.answer").value("Your next-period estimate is available from Flowelle."))
                .andReturn();

        JsonNode response = objectMapper.readTree(chatResult.getResponse().getContentAsString());
        UUID apiKeyId = UUID.fromString(objectMapper.readTree(apiKeyResult.getResponse().getContentAsString()).get("id").asText());
        org.assertj.core.api.Assertions.assertThat(response.get("sessionId").asText()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(apiKeyRepository.findById(apiKeyId).orElseThrow().getLastUsedAt()).isNotNull();
    }

    @Test
    void adminRejectsLiteralToolSecretInProductionConfiguration() throws Exception {
        String tenantSlug = "tool-config-" + UUID.randomUUID();
        String adminBearer = "Bearer " + adminToken("functional-admin");

        mockMvc.perform(put("/internal/admin/tenants/{tenantSlug}", tenantSlug)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"%s\",\"displayName\":\"Tool Config\",\"active\":true}".formatted(tenantSlug)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/internal/admin/tenants/{tenantSlug}/tools/cycle-summary", tenantSlug)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "cycle-summary",
                                  "callbackUrl": "https://flowelle.test/aif/tools/cycle-summary",
                                  "secretRef": "literal://should-not-be-stored",
                                  "signingKeyId": "flowelle-v1",
                                  "allowedScopes": ["cycle:read"],
                                  "active": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SECRET_REF"));
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        return generator.generateKeyPair();
    }

    private static String adminToken(String subject) throws Exception {
        long now = Instant.now().getEpochSecond();
        String claims = "{\"iss\":\"%s\",\"sub\":\"%s\",\"aud\":\"%s\",\"iat\":%d,\"exp\":%d,\"jti\":\"%s\",\"roles\":[\"aif-admin\"]}"
                .formatted(ADMIN_ISSUER, subject, ADMIN_AUDIENCE, now, now + 300, UUID.randomUUID());
        return sign(adminKeyPair, ADMIN_KID, claims);
    }

    private static String userToken(
            KeyPair keyPair,
            String issuer,
            String kid,
            String tenantSlug,
            String subject) throws Exception {
        long now = Instant.now().getEpochSecond();
        String claims = "{\"iss\":\"%s\",\"sub\":\"%s\",\"aud\":\"flowelle-chat\",\"iat\":%d,\"exp\":%d,\"jti\":\"%s\",\"tenant\":\"%s\",\"scope\":[\"wellness:chat\"]}"
                .formatted(issuer, subject, now, now + 60, UUID.randomUUID(), tenantSlug);
        return sign(keyPair, kid, claims);
    }

    private static String sign(KeyPair keyPair, String kid, String claims) throws Exception {
        String header = "{\"alg\":\"RS256\",\"kid\":\"%s\",\"typ\":\"JWT\"}".formatted(kid);
        String input = base64Url(header.getBytes(StandardCharsets.UTF_8)) + "."
                + base64Url(claims.getBytes(StandardCharsets.UTF_8));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(input.getBytes(StandardCharsets.US_ASCII));
        return input + "." + base64Url(signature.sign());
    }

    private static String inlineJwks(String kid, RSAPublicKey publicKey) {
        String jwks = "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"%s\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"%s\",\"e\":\"%s\"}]}"
                .formatted(kid, base64Url(publicKey.getModulus().toByteArray()), base64Url(publicKey.getPublicExponent().toByteArray()));
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
