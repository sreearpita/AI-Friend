package com.example.demo.controller;

import com.example.demo.dto.ConversationResponse;
import com.example.demo.model.AppUser;
import com.example.demo.model.Conversation;
import com.example.demo.service.ConversationService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ConversationService conversationService;

    @Test
    void createConversationReturnsConversationId() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        AppUser user = new AppUser(userId);
        Conversation conv = new Conversation();
        conv.setId(convId);

        when(userService.getOrCreate(userId)).thenReturn(user);
        when(conversationService.create(user)).thenReturn(conv);

        mockMvc.perform(post("/api/conversations")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(convId.toString()));
    }

    @Test
    void createConversationMissingHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/conversations"))
                .andExpect(status().isBadRequest());
    }
}
