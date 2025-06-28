package com.erinaceous.documind;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.erinaceous.documind.chat.ChatAgent;
import com.erinaceous.documind.chat.ChatMessage;
import com.erinaceous.documind.dao.ChunkEmbedding;
import com.erinaceous.documind.file.FileAgent;
import com.erinaceous.documind.network.EmbeddingFetcher;
import com.erinaceous.documind.network.QwenV1Api;
import com.erinaceous.documind.network.QwenV1Api.QwenPlusResponse;
import com.google.gson.Gson;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import org.apache.commons.compress.utils.Lists;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 1001;
    private final FileAgent fileAgent = new FileAgent(this);
    private final ChatAgent chatAgent = new ChatAgent(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PDFBoxResourceLoader.init(getApplicationContext());

        // File select button
        fileAgent.setSelectedFileNameTextView(findViewById(R.id.selectedFileName));
        Button selectFileButton = findViewById(R.id.selectFileButton);

        selectFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
            startActivityForResult(Intent.createChooser(intent, "Choose File"), FILE_SELECT_CODE);
        });

        chatAgent.setChatRecyclerView(findViewById(R.id.chatRecyclerView));
        fileAgent.setChatAgent(chatAgent);

        EditText editTextMessage = findViewById(R.id.editTextMessage);
        Button sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> {

            if (fileAgent.getSelectedFileUri() == null) {
                Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
                return;
            }

            String question = editTextMessage.getText().toString().trim();

            if (question.isEmpty()) {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> questionList = Lists.newArrayList();
            questionList.add(question);
            List<Float> questionEmbeddingVector = Lists.newArrayList();
            new EmbeddingFetcher(this, chatAgent) {
                @Override
                protected void deal(Response<QwenV1Api.QwenEmbeddingResponse> response) {
                    List<Float> localQuestionEmbeddingVector = response.body().data.get(0).embedding;
                    questionEmbeddingVector.addAll(localQuestionEmbeddingVector);
                }
            }.run(questionList);

            chatAgent.instantMessage(new ChatMessage(question, ChatMessage.Sender.USER));
            editTextMessage.setText("");

            List<ChunkEmbedding> allChunks = Manager.getInstance(this).getAppDatabase().chunkEmbeddingDao().getAll();
            List<ChunkEmbedding> topChunks = fileAgent.getTopNRelevantChunks(allChunks, questionEmbeddingVector, 3);


            StringBuilder contextBuilder = new StringBuilder("");
            for (int i = 0; i < topChunks.size(); i++) {
                contextBuilder.append("Chunk ").append(i + 1).append(": ").append(topChunks.get(i).chunkText).append("\n");
            }

            // Call Qwen API here, then add the response
//            QwenV1Api.QwenPlusRequest request = new QwenV1Api.QwenPlusRequest(selectedFileContentText, question);
            //TODO
            //TODO
            QwenV1Api.QwenPlusRequest qwenPlusRequest = new QwenV1Api.QwenPlusRequest(contextBuilder.toString(), question);

            Manager.getInstance(this).getQwenV1Api().getAnswer(qwenPlusRequest).enqueue(new Callback<QwenPlusResponse>() {
                @Override
                public void onResponse(Call<QwenPlusResponse> call, Response<QwenPlusResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String answer = response.body().choices.get(0).message.content;
                        chatAgent.instantMessage(new ChatMessage(answer, ChatMessage.Sender.AI));
                    } else {
                        chatAgent.instantMessage(new ChatMessage("Failed to connect to Qwen API", ChatMessage.Sender.AI));
                    }
                }

                @Override
                public void onFailure(Call<QwenPlusResponse> call, Throwable t) {
                    chatAgent.instantMessage(new ChatMessage("Error: " + t.getMessage(), ChatMessage.Sender.AI));
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
                fileAgent.setSelectedFileUri(selectedFileUri);
                chatAgent.instantMessage(new ChatMessage("📄 " + fileAgent.getFileName() + " loaded.", ChatMessage.Sender.USER));
                chatAgent.instantMessage(new ChatMessage(fileAgent.getFullText(), ChatMessage.Sender.AI));
            }
        }
    }
    // Get filename from URI


}
