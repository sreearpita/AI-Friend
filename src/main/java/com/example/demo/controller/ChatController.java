package com.example.demo.controller;

import com.example.demo.service.OllamaService;
import com.example.demo.service.OllamaService.OllamaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Value("${app.persona}")
    private String systemPrompt;

    private final OllamaService ollamaService;

    public ChatController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        logger.info("Received legacy chat request");
        List<OllamaMessage> messages = List.of(
                new OllamaMessage("system", systemPrompt),
                new OllamaMessage("user", message)
        );
        return ollamaService.chat(messages);
    }
}
