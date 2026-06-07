package com.example.demo.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.model.ModelClient;
import com.example.demo.model.ModelClientException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {
    private static final String API_KEY = "dev-aif-demo-key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModelClient modelClient;

    @BeforeEach
    void setUp() {
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
                .andExpect(jsonPath("$.answer", containsString("urgent medical help")));

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
}
