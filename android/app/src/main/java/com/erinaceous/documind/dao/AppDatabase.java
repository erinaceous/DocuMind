package com.erinaceous.documind.dao;
import androidx.room.Database;
import androidx.room.RoomDatabase;
@Database(entities = {ChunkEmbedding.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ChunkEmbeddingDao chunkEmbeddingDao();
}