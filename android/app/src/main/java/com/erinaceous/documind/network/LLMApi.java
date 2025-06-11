package com.erinaceous.documind.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LLMApi {
    @POST("chat/completions")
    Call<LLMResponse> getAnswer(@Body LLMRequest request);
}
