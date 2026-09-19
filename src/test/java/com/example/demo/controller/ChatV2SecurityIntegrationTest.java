package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.util.Set;
import java.util.UUID;

import com.example.demo.model.ModelClient;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantUserAuthConfig;
import com.example.demo.repository.ChatSessionRepository;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.TenantRepository;
import com.example.demo.repository.TenantUserAuthConfigRepository;
import com.example.demo.service.FlowelleToolClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "aif.rate-limits.user-per-minute=1",
        "aif.rate-limits.tenant-per-minute=100",
        "aif.rate-limits.tenant-per-day=100"
})
@AutoConfigureMockMvc
class ChatV2SecurityIntegrationTest {
    private static final String API_KEY = "dev-aif-demo-key";
    private static final String ISSUER = "https://flowelle.example";
    private static final String KEY_ID = "flowelle-test-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantUserAuthConfigRepository authConfigRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MockBean
    private ModelClient modelClient;

    @MockBean
    private FlowelleToolClient flowelleToolClient;

    private KeyPair keyPair;
    private Tenant tenant;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = generateKeyPair();
        authConfigRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatSessionRepository.deleteAll();
        tenant = tenantRepository.findBySlug("demo").orElseThrow();
        authConfigRepository.save(new TenantUserAuthConfig(
                tenant,
                ISSUER,
                "ai-friend-chat",
                inlineJwks((RSAPublicKey) keyPair.getPublic()),
                "RS256",
                60,
                300,
                true));
        when(modelClient.generate(anyList())).thenReturn("A v2 grounded answer.");
    }

    @Test
    void v2UsesTokenIdentityAndIgnoresRequestIdentityFields() throws Exception {
        String token = token("42", "jti-" + UUID.randomUUID(), "demo",
                List.of("wellness:chat", "cycle:read", "preferences:read"), 60);

        MvcResult result = mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "attacker-controlled-body-user",
                                  "message": "When is my next period?",
                                  "scopes": ["admin"],
                                  "locale": "en-US"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.answer").value("A v2 grounded answer."))
                .andReturn();

        String sessionId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString())
                .get("sessionId")
                .asText();
        assertThat(chatSessionRepository.findById(UUID.fromString(sessionId)).orElseThrow().getExternalUserId())
                .isEqualTo("42");
    }

    @Test
    void v2AllowsGeneralChatButSkipsFlowelleWhenConsentIsDisabled() throws Exception {
        String token = token("4201", "jti-" + UUID.randomUUID(), "demo",
                List.of("wellness:chat", "cycle:read"), 60, false);

        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"When is my next period?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("A v2 grounded answer."))
                .andExpect(jsonPath("$.toolCalls[0].status").value("SKIPPED"))
                .andExpect(jsonPath("$.toolCalls[0].summary").value("Flowelle data is unavailable because AI coaching consent is not enabled."));

        org.mockito.Mockito.verifyNoInteractions(flowelleToolClient);
    }

    @Test
    void v2TreatsMissingConsentClaimAsDisabled() throws Exception {
        String token = tokenWithoutConsent("4202", "jti-" + UUID.randomUUID(), "demo",
                List.of("wellness:chat", "cycle:read"), 60);

        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"When is my next period?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolCalls[0].status").value("SKIPPED"));

        org.mockito.Mockito.verifyNoInteractions(flowelleToolClient);
    }

    @Test
    void v2RejectsReplayedUserContextToken() throws Exception {
        String token = token("flowelle-user-replay", "jti-" + UUID.randomUUID(), "demo",
                List.of("wellness:chat"), 60);
        String body = """
                {
                  "message": "Suggest light exercise"
                }
                """;

        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REPLAYED_USER_CONTEXT"));
    }

    @Test
    void v2RejectsWrongTenantClaim() throws Exception {
        String token = token("flowelle-user-wrong-tenant", "jti-" + UUID.randomUUID(), "other-tenant",
                List.of("wellness:chat"), 60);

        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_USER_CONTEXT"));
    }

    @Test
    void v2RequiresUserContextJwt() throws Exception {
        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_MISSING_USER_CONTEXT"));
    }

    @Test
    void v2RequiresWellnessChatScope() throws Exception {
        String token = token("flowelle-user-no-scope", "jti-" + UUID.randomUUID(), "demo",
                List.of("cycle:read"), 60);

        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_INSUFFICIENT_SCOPE"));
    }

    @Test
    void v2RejectsMalformedJsonBeforeCallingModel() throws Exception {
        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER,
                                token("flowelle-user-malformed", "jti-" + UUID.randomUUID(), "demo",
                                        List.of("wellness:chat"), 60))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void v2RejectsExpiredUserContextToken() throws Exception {
        String token = token("flowelle-user-expired", "jti-" + UUID.randomUUID(), "demo",
                List.of("wellness:chat"), -1);

        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_USER_CONTEXT"));
    }

    @Test
    void v2AppliesUserRateLimitWithRetryHeader() throws Exception {
        String userId = "flowelle-user-limited-" + UUID.randomUUID();
        String firstToken = token(userId, "jti-" + UUID.randomUUID(), "demo", List.of("wellness:chat"), 60);
        String secondToken = token(userId, "jti-" + UUID.randomUUID(), "demo", List.of("wellness:chat"), 60);
        String body = """
                {
                  "message": "Hello"
                }
                """;

        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v2/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .header(ChatController.USER_CONTEXT_HEADER, secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String token(String subject, String jwtId, String tenantSlug, List<String> scopes, long lifetimeSeconds) throws Exception {
        return token(subject, jwtId, tenantSlug, scopes, lifetimeSeconds, true);
    }

    private String token(String subject, String jwtId, String tenantSlug, List<String> scopes, long lifetimeSeconds, boolean aiCoachEnabled) throws Exception {
        long now = Instant.now().getEpochSecond();
        String header = "{\"alg\":\"RS256\",\"kid\":\"%s\",\"typ\":\"JWT\"}".formatted(KEY_ID);
        String scopeJson = scopes.stream().map(scope -> "\"" + scope + "\"").reduce((left, right) -> left + "," + right).orElse("");
        String claims = """
                {"iss":"%s","sub":"%s","aud":"ai-friend-chat","iat":%d,"exp":%d,"jti":"%s","tenant":"%s","scope":[%s],"aiCoachEnabled":%s}
                """.formatted(ISSUER, subject, now, now + lifetimeSeconds, jwtId, tenantSlug, scopeJson, aiCoachEnabled).trim();
        return signedToken(header, claims);
    }

    private String tokenWithoutConsent(String subject, String jwtId, String tenantSlug, List<String> scopes, long lifetimeSeconds) throws Exception {
        long now = Instant.now().getEpochSecond();
        String header = "{\"alg\":\"RS256\",\"kid\":\"%s\",\"typ\":\"JWT\"}".formatted(KEY_ID);
        String scopeJson = scopes.stream().map(scope -> "\"" + scope + "\"").reduce((left, right) -> left + "," + right).orElse("");
        String claims = """
                {"iss":"%s","sub":"%s","aud":"ai-friend-chat","iat":%d,"exp":%d,"jti":"%s","tenant":"%s","scope":[%s]}
                """.formatted(ISSUER, subject, now, now + lifetimeSeconds, jwtId, tenantSlug, scopeJson).trim();
        return signedToken(header, claims);
    }

    private String signedToken(String header, String claims) throws Exception {
        String signingInput = base64Url(header.getBytes(StandardCharsets.UTF_8))
                + "."
                + base64Url(claims.getBytes(StandardCharsets.UTF_8));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + base64Url(signature.sign());
    }

    private String inlineJwks(RSAPublicKey publicKey) {
        String jwks = """
                {"keys":[{"kty":"RSA","kid":"%s","alg":"RS256","use":"sig","n":"%s","e":"%s"}]}
                """.formatted(
                KEY_ID,
                base64Url(publicKey.getModulus().toByteArray()),
                base64Url(publicKey.getPublicExponent().toByteArray())).trim();
        return "inline://" + base64Url(jwks.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stripLeadingZero(value));
    }

    private byte[] stripLeadingZero(byte[] value) {
        if (value.length > 1 && value[0] == 0) {
            return java.util.Arrays.copyOfRange(value, 1, value.length);
        }
        return value;
    }
}
