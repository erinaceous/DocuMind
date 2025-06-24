package com.erinaceous.documind.dao;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChunkEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChunkEmbedding> embeddings);

    @Query("SELECT * FROM chunk_embeddings")
    List<ChunkEmbedding> getAll();
}