package com.erinaceous.documind.network;

import android.content.ContextWrapper;

import com.erinaceous.documind.Manager;
import com.erinaceous.documind.chat.ChatAgent;
import com.erinaceous.documind.chat.ChatMessage;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class EmbeddingFetcher {
    private final ContextWrapper contextWrapper;
    private final ChatAgent chatAgent;

    public EmbeddingFetcher(ContextWrapper contextWrapper, ChatAgent chatAgent) {
        this.contextWrapper = contextWrapper;
        this.chatAgent = chatAgent;
    }

    protected abstract void deal(Response<QwenV1Api.QwenEmbeddingResponse> response);

    public void run(List<String> textList) {
        QwenV1Api.QwenEmbeddingRequest qwenEmbeddingRequest = new QwenV1Api.QwenEmbeddingRequest(textList);

        Manager.getInstance(contextWrapper).getQwenV1Api().getEmbedding(qwenEmbeddingRequest).enqueue(new Callback<QwenV1Api.QwenEmbeddingResponse>() {
            @Override
            public void onResponse(Call<QwenV1Api.QwenEmbeddingResponse> call, Response<QwenV1Api.QwenEmbeddingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    deal(response);
                }
            }

            @Override
            public void onFailure(Call<QwenV1Api.QwenEmbeddingResponse> call, Throwable t) {
                if (null == chatAgent) {
                    return;
                }

                chatAgent.instantMessage(new ChatMessage("Error: " + t.getMessage(), ChatMessage.Sender.AI));
            }
        });
    }
}
