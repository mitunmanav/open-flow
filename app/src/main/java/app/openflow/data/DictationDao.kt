package app.openflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DictationDao {
    @Query("SELECT * FROM dictations ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<DictationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(d: DictationEntity)

    @Query("DELETE FROM dictations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM dictations WHERE createdAtEpochMs < :beforeEpochMs")
    suspend fun deleteOlderThan(beforeEpochMs: Long)

    @Query("SELECT * FROM dictations ORDER BY createdAtEpochMs DESC LIMIT 1")
    suspend fun latest(): DictationEntity?

    @Query("SELECT * FROM dictations WHERE id = :id")
    suspend fun get(id: String): DictationEntity?
}

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary_words ORDER BY word COLLATE NOCASE")
    fun observeAll(): Flow<List<DictionaryWordEntity>>

    @Query("SELECT * FROM dictionary_words")
    suspend fun all(): List<DictionaryWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(w: DictionaryWordEntity)

    @Query("DELETE FROM dictionary_words WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets ORDER BY trigger COLLATE NOCASE")
    fun observeAll(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets")
    suspend fun all(): List<SnippetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: SnippetEntity)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM app_stats WHERE id = 1")
    suspend fun get(): AppStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: AppStatsEntity)
}
