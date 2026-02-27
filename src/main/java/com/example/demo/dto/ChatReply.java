package com.example.demo.dto;

public class ChatReply {
    private String reply;

    public ChatReply() {}

    public ChatReply(String reply) {
        this.reply = reply;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
}
