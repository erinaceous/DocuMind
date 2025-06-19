package com.erinaceous.documind.tools;

import java.util.ArrayList;
import java.util.List;

public class TextTools {

    public static List<String> splitTextIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        // Check if it's mostly Chinese (heuristic: count Chinese characters)
        int chineseCount = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseCount++;
            }
        }
        boolean isChinese = ((double) chineseCount / text.length()) > 0.3;

        if (isChinese) {
            // Chinese — fixed-length character chunking
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + chunkSize, text.length());
                chunks.add(text.substring(start, end));
                start += (chunkSize - overlap);
            }
        } else {
            // English — word-based chunking
            String[] words = text.split("\\s+");
            int start = 0;
            while (start < words.length) {
                int end = Math.min(start + chunkSize, words.length);
                StringBuilder chunk = new StringBuilder();
                for (int i = start; i < end; i++) {
                    chunk.append(words[i]).append(" ");
                }
                chunks.add(chunk.toString().trim());
                start += (chunkSize - overlap);
            }
        }

        return chunks;
    }
}
