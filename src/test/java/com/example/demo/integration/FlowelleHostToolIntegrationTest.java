package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import com.example.demo.model.ChatPromptMessage;
import com.example.demo.model.ModelClient;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.repository.TenantRepository;
import com.example.demo.repository.TenantToolConfigRepository;
import com.example.demo.security.HostToolSigner;
import com.example.demo.service.FlowelleToolClient;
import com.example.demo.service.RestTemplateHostToolClient;
import com.example.demo.support.ContractFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class FlowelleHostToolIntegrationTest {
    private static final String API_KEY = "dev-aif-demo-key";
    private static final String CYCLE_SUMMARY_URL = "https://flowelle.test/api/aif/tools/cycle-summary";
    private static final String USER_PREFERENCES_URL = "https://flowelle.test/aif/tools/user-preferences";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantToolConfigRepository tenantToolConfigRepository;

    @Autowired
    private RestTemplateHostToolClient restTemplateHostToolClient;

    @Autowired
    private HostToolSigner hostToolSigner;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModelClient modelClient;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        tenantToolConfigRepository.deleteAll();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(restTemplateHostToolClient, "restTemplate");
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        when(modelClient.generate(anyList())).thenReturn("A grounded wellness answer.");
        seedToolConfigs();
    }

    @Test
    void signedFlowelleCallbacksReturnCompletedToolCallsAndCitations() throws Exception {
        expectSignedCallback(
                CYCLE_SUMMARY_URL,
                ContractFixtures.read("/contracts/flowelle/cycle-summary-response.json"));
        expectSignedCallback(
                USER_PREFERENCES_URL,
                ContractFixtures.read("/contracts/flowelle/user-preferences-response.json"));

        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "When is my next period and what food and exercise help with PMS?",
                                  "locale": "en-US",
                                  "scopes": ["cycle:read", "preferences:read", "wellness"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolCalls.length()").value(2))
                .andExpect(jsonPath("$.toolCalls[0].name").value("cycle-summary"))
                .andExpect(jsonPath("$.toolCalls[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.toolCalls[1].name").value("user-preferences"))
                .andExpect(jsonPath("$.toolCalls[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$.citations[0].title").exists());

        ArgumentCaptor<java.util.List<ChatPromptMessage>> promptCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(modelClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .anySatisfy(message -> assertThat(message.content()).contains("Approved host-app facts"))
                .anySatisfy(message -> assertThat(message.content()).contains("Curated wellness references"));

        mockServer.verify();
    }

    @Test
    void redFlagPromptSkipsSignedCallbacksRetrievalAndModel() throws Exception {
        mockMvc.perform(post("/v1/chat/messages")
                        .header("X-AIF-Tenant-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalUserId": "flowelle-user-1",
                                  "message": "I have severe pain and heavy bleeding and need food advice",
                                  "scopes": ["cycle:read", "preferences:read"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safetyStatus").value("RED_FLAG_ESCALATION"))
                .andExpect(jsonPath("$.citations").isEmpty())
                .andExpect(jsonPath("$.toolCalls").isEmpty());

        verify(modelClient, never()).generate(anyList());
        mockServer.verify();
    }

    private void expectSignedCallback(String url, String responseBody) {
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HostToolSigner.TENANT_HEADER, "demo"))
                .andExpect(header(HostToolSigner.KEY_ID_HEADER, "dev-v1"))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    String timestamp = request.getHeaders().getFirst(HostToolSigner.TIMESTAMP_HEADER);
                    String signature = request.getHeaders().getFirst(HostToolSigner.SIGNATURE_HEADER);
                    assertThat(timestamp).isNotBlank();
                    assertThat(signature).isEqualTo(hostToolSigner.sign(timestamp, body, "test-secret"));
                    assertThat(body).contains("\"tenantSlug\":\"demo\"");
                })
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private void seedToolConfigs() {
        Tenant tenant = tenantRepository.findBySlug("demo").orElseThrow();
        tenantToolConfigRepository.save(new TenantToolConfig(
                tenant,
                FlowelleToolClient.CYCLE_SUMMARY_TOOL,
                CYCLE_SUMMARY_URL,
                "test-secret",
                Set.of("cycle:read"),
                true));
        tenantToolConfigRepository.save(new TenantToolConfig(
                tenant,
                FlowelleToolClient.USER_PREFERENCES_TOOL,
                USER_PREFERENCES_URL,
                "test-secret",
                Set.of("preferences:read"),
                true));
    }
}
