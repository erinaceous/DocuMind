package com.erinaceous.documind.network;

import java.util.Arrays;
import java.util.List;

public class LLMRequest {
    private String model;
    private List<Message> messages;

    public LLMRequest(String content, String question) {
        this.model = "qwen-plus"; // or your model name
        this.messages = Arrays.asList(
                new Message("system", "You are a helpful assistant."),
                new Message("user", "Content: " + content + "\n\nQuestion: " + question)
        );
    }

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
