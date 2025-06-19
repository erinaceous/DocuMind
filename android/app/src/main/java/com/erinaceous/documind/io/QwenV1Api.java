package com.erinaceous.documind.io;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface QwenV1Api {
    @POST("chat/completions")
    Call<QwenPlusResponse> getAnswer(@Body QwenPlusRequest request);
    Call<QwenEmbeddingResponse> getEmbedding(@Body QwenEmbeddingRequest request);

    class QwenPlusRequest {
        private final String model;
        private final List<Message> messages;

        public QwenPlusRequest(String content, String question) {
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

    class QwenPlusResponse {
        public List<Choice> choices;

        public static class Choice {
            public Message message;
        }

        public static class Message {
            public String role;
            public String content;
        }
    }

    class QwenEmbeddingRequest {
        private final String model;
        private final String input;

        public QwenEmbeddingRequest(String input) {
            this.model = "text-embedding-v1"; // or your model name
            this.input = input;
        }
    }

    class QwenEmbeddingResponse {

        public List<Data> data;

        public static class Data {
            public List<Double> embedding;
            public Integer index;
            public String object;
        }

    }

}


