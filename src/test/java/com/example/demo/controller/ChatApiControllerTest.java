package com.example.demo.controller;

import com.example.demo.dto.ChatRequest;
import com.example.demo.service.ChatApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatApiController.class)
class ChatApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatApiService chatApiService;

    @Test
    void chatReturnsReply() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        when(chatApiService.chat(eq(userId), eq(convId), eq("Hello")))
                .thenReturn("Hey there!");

        ChatRequest request = new ChatRequest();
        request.setConversationId(convId.toString());
        request.setMessage("Hello");

        mockMvc.perform(post("/api/chat")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hey there!"));
    }

    @Test
    void chatMissingHeaderReturnsBadRequest() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setConversationId(UUID.randomUUID().toString());
        request.setMessage("Hello");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatInvalidConversationIdReturnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();

        ChatRequest request = new ChatRequest();
        request.setConversationId("not-a-uuid");
        request.setMessage("Hello");

        mockMvc.perform(post("/api/chat")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatConversationNotFoundReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        when(chatApiService.chat(any(), eq(convId), any()))
                .thenThrow(new IllegalArgumentException("Conversation not found"));

        ChatRequest request = new ChatRequest();
        request.setConversationId(convId.toString());
        request.setMessage("Hello");

        mockMvc.perform(post("/api/chat")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void chatWrongUserReturns403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        when(chatApiService.chat(any(), eq(convId), any()))
                .thenThrow(new SecurityException("Conversation does not belong to this user"));

        ChatRequest request = new ChatRequest();
        request.setConversationId(convId.toString());
        request.setMessage("Hello");

        mockMvc.perform(post("/api/chat")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void chatBlankMessageReturnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();

        ChatRequest request = new ChatRequest();
        request.setConversationId(UUID.randomUUID().toString());
        request.setMessage("   ");

        mockMvc.perform(post("/api/chat")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
