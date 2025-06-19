package com.erinaceous.documind.file;

import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.TextView;

import com.erinaceous.documind.tools.TextTools;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private final ContextWrapper contextWrapper;
    private String fullText;
    private Uri selectedFileUri;
    private TextView selectedFileNameTextView;
    private String fileName;
    private List<String> selectedFileContentChunks;

    public FileManager(ContextWrapper contextWrapper) {
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

    public FileManager setSelectedFileUri(Uri selectedFileUri) {
        this.selectedFileUri = selectedFileUri;
        this.fileName = getFileNameFromUri(selectedFileUri);
        selectedFileNameTextView.setText(fileName);
        selectedFileContentChunks = extractFileChunks(selectedFileUri);
        return this;
    }

    public TextView getSelectedFileNameTextView() {
        return selectedFileNameTextView;
    }

    public FileManager setSelectedFileNameTextView(TextView selectedFileNameTextView) {
        this.selectedFileNameTextView = selectedFileNameTextView;
        return this;
    }

    public List<String> getSelectedFileContentChunks() {
        return selectedFileContentChunks;
    }

    public String getFileName() {
        return fileName;
    }
}
