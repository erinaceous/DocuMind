package com.erinaceous.documind.network;

import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface QwenV1Api {



    @POST("chat/completions")
    Call<QwenPlusResponse> getAnswer(@Body QwenPlusRequest request);

    class QwenPlusRequest {
        private final String model;
        private final List<Message> messages;

        public QwenPlusRequest(String content, String question) {
            this.model = "qwen-plus"; // or your model name
            this.messages = Arrays.asList(
                    new Message("system", "You are a helpful assistant."),
                    new Message("user", "Content: \\n\\n" + content + "\n\nQuestion: " + question)
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

    @POST("embeddings")
    Call<QwenEmbeddingResponse> getEmbedding(@Body QwenEmbeddingRequest request);

    class QwenEmbeddingRequest {
        private final String model;
        private final List<String> inputList;

        public QwenEmbeddingRequest(List<String> inputList) {
            this.model = "text-embedding-v1"; // or your model name
            this.inputList = inputList;
        }
    }

    class QwenEmbeddingResponse {

        public List<Data> data;

        public static class Data {
            public List<Float> embedding;
            public Integer index;
            public String object;
        }

    }


    public static QwenV1Api createApi() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder builder = original.newBuilder()
                            .header("Authorization", "Bearer YOUR_API_ID");
                    return chain.proceed(builder.build());
                }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(QwenV1Api.class);
    }

}


