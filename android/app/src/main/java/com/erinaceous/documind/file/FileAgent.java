package com.erinaceous.documind.file;

import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Pair;
import android.widget.TextView;

import com.erinaceous.documind.Manager;
import com.erinaceous.documind.chat.ChatAgent;
import com.erinaceous.documind.chat.ChatMessage;
import com.erinaceous.documind.dao.ChunkEmbedding;
import com.erinaceous.documind.network.EmbeddingFetcher;
import com.erinaceous.documind.network.QwenV1Api;
import com.erinaceous.documind.tools.TextTools;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

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
        saveEmbeddings(selectedFileContentChunks);
        return this;
    }

    private void saveEmbeddings(List<String> chunkList) {
        new EmbeddingFetcher(contextWrapper, chatAgent) {
            @Override
            protected void deal(Response<QwenV1Api.QwenEmbeddingResponse> response) {
                List<ChunkEmbedding> chunkEmbeddingList = new ArrayList<>();

                for (int i = 0; i < response.body().data.size(); i++) {
                    String json = new Gson().toJson(response.body().data.get(i).embedding);
                    chunkEmbeddingList.add(new ChunkEmbedding(chunkList.get(i), json));
                }
                Executors.newSingleThreadExecutor().execute(() -> {
                    Manager.getInstance(contextWrapper).getAppDatabase().chunkEmbeddingDao().deleteAll();
                    Manager.getInstance(contextWrapper).getAppDatabase().chunkEmbeddingDao().insertAll(chunkEmbeddingList);
                });

            }
        }.run(chunkList);

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

    public String matchMostSimilaryChunk(List<Float> questionEmbeddingVector){

        for(String chunk : selectedFileContentChunks){

        }
        return "";
    }

    public List<ChunkEmbedding> getTopNRelevantChunks(List<ChunkEmbedding> chunks, List<Float> questionEmbedding, int topN) {
        List<Pair<ChunkEmbedding, Float>> scoredChunks = new ArrayList<>();

        for (ChunkEmbedding chunk : chunks) {
            List<Float> chunkVector = parseEmbeddingJson(chunk.embeddingJson);
            float score = cosineSimilarity(questionEmbedding, chunkVector);
            scoredChunks.add(new Pair<>(chunk, score));
        }

        // Sort by similarity (descending)
        scoredChunks.sort((a, b) -> Float.compare(b.second, a.second));

        // Collect top-N chunks
        List<ChunkEmbedding> topChunks = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, scoredChunks.size()); i++) {
            topChunks.add(scoredChunks.get(i).first);
        }

        return topChunks;
    }

    private List<Float> parseEmbeddingJson(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<Float>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private float cosineSimilarity(List<Float> a, List<Float> b) {
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += Math.pow(a.get(i), 2);
            normB += Math.pow(b.get(i), 2);
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
