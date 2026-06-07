package com.example.demo.model;

import java.util.List;

public interface ModelClient {
    String generate(List<ChatPromptMessage> messages);
}
