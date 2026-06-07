package com.example.demo.service;

import java.util.List;

import com.example.demo.model.ChatPromptMessage;
import com.example.demo.model.ModelClient;
import com.example.demo.model.ModelClientException;

import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.stereotype.Service;

@Service
public class OllamaModelClient implements ModelClient {
    private final OllamaChatClient chatClient;

    public OllamaModelClient(OllamaChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String generate(List<ChatPromptMessage> messages) {
        try {
            List<Message> promptMessages = messages.stream()
                    .map(this::toSpringAiMessage)
                    .toList();
            ChatResponse response = chatClient.call(new Prompt(promptMessages));
            return response.getResult().getOutput().getContent();
        } catch (Exception exception) {
            throw new ModelClientException("Model provider call failed", exception);
        }
    }

    private Message toSpringAiMessage(ChatPromptMessage message) {
        if ("system".equalsIgnoreCase(message.role())) {
            return new SystemMessage(message.content());
        }
        return new UserMessage(message.content());
    }
}
