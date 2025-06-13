package com.erinaceous.documind.chat;

public class ChatMessage {
    public enum Sender { USER, AI }

    private String message;
    private Sender sender;

    public ChatMessage(String message, Sender sender) {
        this.message = message;
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public Sender getSender() {
        return sender;
    }
}
