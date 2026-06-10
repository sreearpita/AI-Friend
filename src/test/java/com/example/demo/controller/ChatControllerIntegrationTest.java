package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.demo.dto.HostToolResponse;
import com.example.demo.model.AuditEvent;
import com.example.demo.model.ModelClient;
import com.example.demo.model.ModelClientException;
import com.example.demo.model.ChatPromptMessage;
import com.example.demo.model.HostToolClientException;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.repository.AuditEventRepository;
import com.example.demo.repository.TenantRepository;
import com.example.demo.repository.TenantToolConfigRepository;
import com.example.demo.service.FlowelleToolClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {
    private static final String API_KEY = "dev-aif-demo-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantToolConfigRepository tenantToolConfigRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @MockBean
    private ModelClient modelClient;

    @MockBean
    private FlowelleToolClient flowelleToolClient;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        tenantToolConfigRepository.deleteAll();
        when(modelClient.generate(anyList())).thenReturn("A grounded wellness answer.");
    }

    @Test
    void chatCreatesSessionAndReturnsModelAnswer() throws Exception {
        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "What can I eat before my period?",
                                  "locale": "en-US",
                                  "scopes": ["wellness"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.answer").value("A grounded wellness answer."))
                .andExpect(jsonPath("$.safetyStatus").value("OK"))
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.toolCalls").isArray());

        ArgumentCaptor<List<ChatPromptMessage>> promptCaptor = ArgumentCaptor.captor();
        verify(modelClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .filteredOn(message -> "user".equals(message.role()) && "What can I eat before my period?".equals(message.content()))
                .hasSize(1);
    }

    @Test
    void wellnessPromptReturnsCitationsAndAddsCitationPromptContext() throws Exception {
        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "What food and hydration can help with PMS?",
                                  "locale": "en-US",
                                  "scopes": ["wellness"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citations[0].title").value("PMS nutrition and hydration basics"))
                .andExpect(jsonPath("$.citations[0].source").value("AI-Friend Curated Wellness Notes"))
                .andExpect(jsonPath("$.citations[0].url").value("https://example.org/wellness/pms-nutrition-hydration"));

        ArgumentCaptor<List<ChatPromptMessage>> promptCaptor = ArgumentCaptor.captor();
        verify(modelClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .anySatisfy(message -> {
                    assertThat(message.role()).isEqualTo("system");
                    assertThat(message.content())
                            .contains("Curated wellness references")
                            .contains("PMS nutrition and hydration basics")
                            .contains("Relevant guidance:");
                });
    }

    @Test
    void chatReusesSessionAndIncludesPriorHistory() throws Exception {
        when(modelClient.generate(anyList())).thenReturn("First answer.", "Second answer.");

        MvcResult firstResult = mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "I feel bloated today"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("First answer."))
                .andReturn();
        String sessionId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("sessionId")
                .asText();

        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "sessionId": "%s",
                                  "message": "What helped last time?"
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.answer").value("Second answer."));

        ArgumentCaptor<List<ChatPromptMessage>> promptCaptor = ArgumentCaptor.captor();
        verify(modelClient, times(2)).generate(promptCaptor.capture());
        List<ChatPromptMessage> secondPrompt = promptCaptor.getAllValues().get(1);
        assertThat(secondPrompt)
                .extracting(ChatPromptMessage::role, ChatPromptMessage::content)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("user", "I feel bloated today"),
                        org.assertj.core.groups.Tuple.tuple("assistant", "First answer."),
                        org.assertj.core.groups.Tuple.tuple("user", "What helped last time?"));
    }

    @Test
    void chatRejectsMissingApiKey() throws Exception {
        mockMvc.perform(post("/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_MISSING_API_KEY"));
    }

    @Test
    void chatRejectsInvalidApiKey() throws Exception {
        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_API_KEY"));
    }

    @Test
    void chatRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void chatRejectsOversizedMessage() throws Exception {
        String oversized = "a".repeat(4001);
        String body = """
                {
                  "externalUserId": "flowelle-user-1",
                  "message": "%s"
                }
                """.formatted(oversized);

        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void redFlagPromptReturnsSafetyEscalationWithoutModelCall() throws Exception {
        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "I have severe pain and heavy bleeding"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safetyStatus").value("RED_FLAG_ESCALATION"))
                .andExpect(jsonPath("$.answer", containsString("urgent medical help")))
                .andExpect(jsonPath("$.citations").isEmpty());

        verify(modelClient, never()).generate(anyList());
    }

    @Test
    void modelFailureReturnsSafeFallback() throws Exception {
        when(modelClient.generate(anyList())).thenThrow(new ModelClientException("down", new RuntimeException("down")));

        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "Suggest light exercise during PMS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safetyStatus").value("MODEL_FALLBACK"))
                .andExpect(jsonPath("$.answer", containsString("trouble reaching the AI service")));
    }

    @Test
    void authorizedHostToolAddsToolCallAndPromptContext() throws Exception {
        seedToolConfig("cycle-summary", Set.of("cycle:read"));
        when(flowelleToolClient.fetchCycleSummary(
                any(Tenant.class),
                any(),
                any(),
                any(TenantToolConfig.class),
                any()))
                .thenReturn(new HostToolResponse(
                        "cycle-summary",
                        "OK",
                        "Predicted next period starts 2026-06-20.",
                        Map.of("nextPeriodStart", "2026-06-20"),
                        "Used your Flowelle cycle summary."));

        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "When is my next period?",
                                  "scopes": ["cycle:read"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolCalls[0].name").value("cycle-summary"))
                .andExpect(jsonPath("$.toolCalls[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.toolCalls[0].summary").value("Used your Flowelle cycle summary."));

        ArgumentCaptor<List<ChatPromptMessage>> promptCaptor = ArgumentCaptor.captor();
        verify(modelClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .anySatisfy(message -> {
                    assertThat(message.role()).isEqualTo("system");
                    assertThat(message.content()).contains("Approved host-app facts", "nextPeriodStart", "2026-06-20");
                });

        assertThat(auditEventRepository.findAll())
                .extracting(AuditEvent::getMetadataJson)
                .anySatisfy(metadata -> assertThat(metadata).contains("cycle-summary:COMPLETED"))
                .noneSatisfy(metadata -> assertThat(metadata).contains("2026-06-20"));
    }

    @Test
    void hostToolWithoutRequiredScopeIsSkipped() throws Exception {
        seedToolConfig("cycle-summary", Set.of("cycle:read"));

        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "When is my next period?",
                                  "scopes": ["wellness"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolCalls[0].name").value("cycle-summary"))
                .andExpect(jsonPath("$.toolCalls[0].status").value("SKIPPED"));

        verify(flowelleToolClient, never()).fetchCycleSummary(
                any(Tenant.class),
                any(),
                any(),
                any(TenantToolConfig.class),
                any());
    }

    @Test
    void hostToolFailureReturnsFailedToolCallAndContinues() throws Exception {
        seedToolConfig("user-preferences", Set.of("preferences:read"));
        when(flowelleToolClient.fetchUserPreferences(
                any(Tenant.class),
                any(),
                any(),
                any(TenantToolConfig.class),
                any()))
                .thenThrow(new HostToolClientException("down", new RuntimeException("down")));

        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "Suggest food and exercise for PMS",
                                  "scopes": ["preferences:read"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("A grounded wellness answer."))
                .andExpect(jsonPath("$.toolCalls[0].name").value("user-preferences"))
                .andExpect(jsonPath("$.toolCalls[0].status").value("FAILED"));

        verify(modelClient).generate(anyList());
    }

    @Test
    void chatAllowsCorsPreflightWithoutApiKey() throws Exception {
        mockMvc.perform(options("/v1/chat/messages")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-aif-tenant-key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    private void seedToolConfig(String name, Set<String> allowedScopes) {
        Tenant tenant = tenantRepository.findBySlug("demo").orElseThrow();
        tenantToolConfigRepository.save(new TenantToolConfig(
                tenant,
                name,
                "https://flowelle.example/aif/tools/" + name,
                "test-secret",
                allowedScopes,
                true));
    }
}
