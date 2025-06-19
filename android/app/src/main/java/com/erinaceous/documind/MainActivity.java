package com.erinaceous.documind;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.erinaceous.documind.chat.ChatAdapter;
import com.erinaceous.documind.chat.ChatMessage;
import com.erinaceous.documind.file.FileManager;
import com.erinaceous.documind.io.QwenV1Api;
import com.erinaceous.documind.io.QwenV1Api.QwenPlusResponse;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 1001;
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    QwenV1Api api;
    private ChatAdapter adapter;
    private RecyclerView chatRecyclerView;
    private final FileManager fileManager = new FileManager(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PDFBoxResourceLoader.init(getApplicationContext());

        api = createApi();

        // File select button
        fileManager.setSelectedFileNameTextView(findViewById(R.id.selectedFileName));
        Button selectFileButton = findViewById(R.id.selectFileButton);

        selectFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
            startActivityForResult(Intent.createChooser(intent, "Choose File"), FILE_SELECT_CODE);
        });


        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        EditText editTextMessage = findViewById(R.id.editTextMessage);
        Button sendButton = findViewById(R.id.sendButton);

        adapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);

        sendButton.setOnClickListener(v -> {

            if (fileManager.getSelectedFileUri() == null) {
                Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
                return;
            }

            String question = editTextMessage.getText().toString().trim();

            if (question.isEmpty()) {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show();
                return;
            }

            chatMessages.add(new ChatMessage(question, ChatMessage.Sender.USER));
            adapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            editTextMessage.setText("");

            // Call Qwen API here, then add the response
//            QwenV1Api.QwenPlusRequest request = new QwenV1Api.QwenPlusRequest(selectedFileContentText, question);
            QwenV1Api.QwenPlusRequest request = new QwenV1Api.QwenPlusRequest("", question);

            api.getAnswer(request).enqueue(new Callback<QwenPlusResponse>() {
                @Override
                public void onResponse(Call<QwenPlusResponse> call, Response<QwenPlusResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String answer = response.body().choices.get(0).message.content;
                        chatMessages.add(new ChatMessage(answer, ChatMessage.Sender.AI));
                    } else {
                        chatMessages.add(new ChatMessage("Failed to connect to Qwen API", ChatMessage.Sender.AI));
                    }
                    adapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                }

                @Override
                public void onFailure(Call<QwenPlusResponse> call, Throwable t) {
                    chatMessages.add(new ChatMessage("Error: " + t.getMessage(), ChatMessage.Sender.AI));
                    adapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                }
            });
        });

    }

    // Handle file selection result

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                fileManager.setSelectedFileUri(selectedFileUri);

                chatMessages.add(new ChatMessage("📄 " + fileManager.getFileName() + " loaded.", ChatMessage.Sender.USER));
                chatMessages.add(new ChatMessage(fileManager.getFullText(), ChatMessage.Sender.AI));
                adapter.notifyItemInserted(chatMessages.size() - 1);
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

            }
        }
    }
    // Get filename from URI

    private QwenV1Api createApi() {
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
