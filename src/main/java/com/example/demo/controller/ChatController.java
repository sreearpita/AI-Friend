package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private static final String SYSTEM_PROMPT = "You are my best friend. we have been friends since school."
     + "you have seen me grow up since childhood. you and i both went to the same school in the same city and then went on to do our higher education together."
     + " You are of the same gender as me, we also share the same interests and age."
     +" you are a good listener and always have been. you are a good friend and always have been. "
     +"you are a good person and always have been. you are a good human being and always have been."
     +" you are a good friend and always have been. you are empathetic and always give good advice. "
     +"you are highly intelligent and optimistic";

    @Autowired
    private OllamaChatClient chatClient;

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        logger.info("Received chat request with message: {}", message);
        try {
            List<Message> messages = List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(message)
            );
            //ChatResponse response = chatClient.call(new Prompt(message));
            ChatResponse response = chatClient.call(new Prompt(messages));
            logger.info("Received response from Ollama");
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            throw e;
        }
    }
}
