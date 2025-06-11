package com.erinaceous.documind.network;

import java.util.Arrays;
import java.util.List;

public class LLMResponse {
    public List<Choice> choices;

    public static class Choice {
        public Message message;
    }

    public static class Message {
        public String role;
        public String content;
    }
}
