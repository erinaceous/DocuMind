package com.erinaceous.documind;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.erinaceous.documind.chat.ChatAdapter;
import com.erinaceous.documind.chat.ChatMessage;
import com.erinaceous.documind.network.LLMApi;
import com.erinaceous.documind.network.LLMRequest;
import com.erinaceous.documind.network.LLMResponse;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;
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
    LLMApi api;
    private Uri selectedFileUri;
    private TextView selectedFileName;
    private String selectedFileContentText;
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter adapter;

    private LLMApi createApi() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder builder = original.newBuilder()
                            .header("Authorization", "Bearer YOU_API_KEY");
                    return chain.proceed(builder.build());
                }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(LLMApi.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PDFBoxResourceLoader.init(getApplicationContext());

        api = createApi();

        // File select button
        selectedFileName = findViewById(R.id.selectedFileName);
        Button selectFileButton = findViewById(R.id.selectFileButton);

        selectFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
            startActivityForResult(Intent.createChooser(intent, "Choose File"), FILE_SELECT_CODE);
        });


        RecyclerView chatRecyclerView = findViewById(R.id.chatRecyclerView);
        EditText editTextMessage = findViewById(R.id.editTextMessage);
        Button sendButton = findViewById(R.id.sendButton);

        adapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);

        sendButton.setOnClickListener(v -> {

            if (selectedFileUri == null) {
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
            LLMRequest request = new LLMRequest(selectedFileContentText, question);

            api.getAnswer(request).enqueue(new Callback<LLMResponse>() {
                @Override
                public void onResponse(Call<LLMResponse> call, Response<LLMResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String answer = response.body().choices.get(0).message.content;
                        chatMessages.add(new ChatMessage(answer, ChatMessage.Sender.AI));
                    } else {
                        chatMessages.add(new ChatMessage("Failed to connect to Qwen API", ChatMessage.Sender.AI));
                    }
                    adapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
//                    adapter.notifyDataSetChanged();

                }

                @Override
                public void onFailure(Call<LLMResponse> call, Throwable t) {
                    chatMessages.add(new ChatMessage("Error: " + t.getMessage(), ChatMessage.Sender.AI));
                    adapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
//                    adapter.notifyDataSetChanged();

                }
            });
        });

    }

    // Handle file selection result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                String fileName = getFileNameFromUri(selectedFileUri);
                selectedFileName.setText(fileName);
                selectedFileContentText = extractFileText(selectedFileUri);
                chatMessages.add(new ChatMessage("📄 " + fileName + " loaded.", ChatMessage.Sender.USER));
                chatMessages.add(new ChatMessage(selectedFileContentText, ChatMessage.Sender.AI));
                adapter.notifyDataSetChanged();

            }
        }
    }

    // Get filename from URI
    private String getFileNameFromUri(Uri uri) {
        String result = "Unknown";
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        return result;
    }

    private String extractFileText(Uri uri) {
        String type = getContentResolver().getType(uri);
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (type != null && type.contains("pdf")) {
                PDDocument document = PDDocument.load(inputStream);
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                document.close();
                return text;
            } else if (type != null && type.contains("officedocument")) {
                XWPFDocument doc = new XWPFDocument(inputStream);
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                String text = extractor.getText();
                extractor.close();
                return text;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}