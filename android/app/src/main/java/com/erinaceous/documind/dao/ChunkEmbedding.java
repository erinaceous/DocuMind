package com.erinaceous.documind.dao;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "chunk_embeddings")
public class ChunkEmbedding {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String chunkText;

    // Store as JSON string (for now)
    public String embeddingJson;

    public ChunkEmbedding(String chunkText, String embeddingJson) {
        this.chunkText = chunkText;
        this.embeddingJson = embeddingJson;
    }
}