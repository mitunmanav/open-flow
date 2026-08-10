package app.openflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        """
        SELECT s.* FROM sessions s
        JOIN sessions_fts fts ON s.rowid = fts.rowid
        WHERE sessions_fts MATCH :query
        ORDER BY s.createdAtEpochMs DESC
        """
    )
    suspend fun searchFts(query: String): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun count(): Int
}
