package com.erinaceous.documind.file;

import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.TextView;

import com.erinaceous.documind.Manager;
import com.erinaceous.documind.chat.ChatAgent;
import com.erinaceous.documind.chat.ChatMessage;
import com.erinaceous.documind.dao.ChunkEmbedding;
import com.erinaceous.documind.network.QwenV1Api;
import com.erinaceous.documind.tools.TextTools;
import com.google.gson.Gson;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileAgent {
    private final ContextWrapper contextWrapper;
    private String fullText;
    private Uri selectedFileUri;
    private TextView selectedFileNameTextView;
    private String fileName;
    private List<String> selectedFileContentChunks;
    private ChatAgent chatAgent = null;

    public FileAgent(ContextWrapper contextWrapper) {
        this.contextWrapper = contextWrapper;
    }

    private String getFileNameFromUri(Uri uri) {
        String result = "Unknown";
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = contextWrapper.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        return result;
    }

    private List<String> extractFileChunks(Uri uri) {
        String type = contextWrapper.getContentResolver().getType(uri);
        List<String> chunks = new ArrayList<>();
        try (InputStream inputStream = contextWrapper.getContentResolver().openInputStream(uri)) {
            String fullText = "";
            if (type != null && type.contains("pdf")) {
                PDDocument document = PDDocument.load(inputStream);
                PDFTextStripper stripper = new PDFTextStripper();
                this.fullText = stripper.getText(document);
                document.close();
            } else if (type != null && type.contains("officedocument")) {
                XWPFDocument doc = new XWPFDocument(inputStream);
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                this.fullText = extractor.getText();
                extractor.close();
            }

            if (!this.fullText.isEmpty()) {
                chunks = TextTools.splitTextIntoChunks(fullText, 200, 50); // You can adjust sizes
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return chunks;
    }

    public String getFullText() {
        return fullText;
    }

    public Uri getSelectedFileUri() {
        return selectedFileUri;
    }

    public FileAgent setSelectedFileUri(Uri selectedFileUri) {
        this.selectedFileUri = selectedFileUri;
        this.fileName = getFileNameFromUri(selectedFileUri);
        selectedFileNameTextView.setText(fileName);
        selectedFileContentChunks = extractFileChunks(selectedFileUri);
        return this;
    }

    private void getEmbeddings(List<String> chunkList) {
        QwenV1Api.QwenEmbeddingRequest qwenEmbeddingRequest = new QwenV1Api.QwenEmbeddingRequest(chunkList);

        Manager.getInstance(contextWrapper).getQwenV1Api().getEmbedding(qwenEmbeddingRequest).enqueue(new Callback<QwenV1Api.QwenEmbeddingResponse>() {
            @Override
            public void onResponse(Call<QwenV1Api.QwenEmbeddingResponse> call, Response<QwenV1Api.QwenEmbeddingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<ChunkEmbedding> chunkEmbeddingList = new ArrayList<>();

                    for (int i = 0; i < response.body().data.size(); i++) {
                        String json = new Gson().toJson(response.body().data.get(i));
                        chunkEmbeddingList.add(new ChunkEmbedding(chunkList.get(i), json));
                    }

                    Manager.getInstance(contextWrapper).getAppDatabase().chunkEmbeddingDao().insertAll(chunkEmbeddingList);
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

    public TextView getSelectedFileNameTextView() {
        return selectedFileNameTextView;
    }

    public FileAgent setSelectedFileNameTextView(TextView selectedFileNameTextView) {
        this.selectedFileNameTextView = selectedFileNameTextView;
        return this;
    }

    public List<String> getSelectedFileContentChunks() {
        return selectedFileContentChunks;
    }

    public String getFileName() {
        return fileName;
    }

    public void setChatAgent(ChatAgent chatAgent) {
        this.chatAgent = chatAgent;
    }
}
