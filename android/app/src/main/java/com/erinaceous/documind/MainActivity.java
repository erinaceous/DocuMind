package com.erinaceous.documind;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.erinaceous.documind.network.LLMApi;
import com.erinaceous.documind.network.LLMRequest;
import com.erinaceous.documind.network.LLMResponse;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 1001;
    private Uri selectedFileUri;

    Button btnSelectFile, btnAsk;
    TextView tvFileName, tvAnswer;
    EditText etQuestion;

    LLMApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PDFBoxResourceLoader.init(getApplicationContext());

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder()
                            .header("Authorization", "Bearer YOUR_API_KEY");
                    return chain.proceed(builder.build());
                }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1/")
//        https://dashscope.aliyuncs.com/api/v1/
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(LLMApi.class);

        setContentView(R.layout.activity_main);

        // Bind views
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnAsk = findViewById(R.id.btnAsk);
        tvFileName = findViewById(R.id.tvFileName);
        tvAnswer = findViewById(R.id.tvAnswer);
        etQuestion = findViewById(R.id.etQuestion);

        // File select button
        btnSelectFile.setOnClickListener(v -> openFilePicker());

        // Ask button
        btnAsk.setOnClickListener(v -> {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
                return;
            }
            String question = etQuestion.getText().toString().trim();
            if (question.isEmpty()) {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show();
                return;
            }

            String content = extractFileText(selectedFileUri);


            LLMRequest request = new LLMRequest(content, question);

            api.getAnswer(request).enqueue(new Callback<LLMResponse>() {
                @Override
                public void onResponse(Call<LLMResponse> call, Response<LLMResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String answer = response.body().choices.get(0).message.content;
                        tvAnswer.setText(answer);
                    } else {
                        tvAnswer.setText("Failed to get answer.");
                    }
                }

                @Override
                public void onFailure(Call<LLMResponse> call, Throwable t) {

                    tvAnswer.setText("Error: " + t.getMessage());
                }
            });


        });
    }

    private String askAI(String content, String question) {
        // Simplified example

        return "abcd";
//        return QwenEngine.ask(content, question);
    }

    private void openFilePicker() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
//        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        String[] mimeTypes = {
//                "application/pdf",
//                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
//        };
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
//        startActivityForResult(Intent.createChooser(intent, "Select Document"), FILE_SELECT_CODE);
        startActivityForResult(intent, FILE_SELECT_CODE);
    }

    // Handle file selection result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            tvFileName.setText(getFileName(selectedFileUri));
        }
    }

    // Get filename from URI
    private String getFileName(Uri uri) {
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