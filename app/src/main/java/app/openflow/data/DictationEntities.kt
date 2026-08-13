package app.openflow.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "dictations")
data class DictationEntity(
    @PrimaryKey val id: String,
    /** Clean / polished transcript (UI shows this). */
    val text: String,
    /** Original STT before cleanup. Empty for pre-v4 rows. */
    val rawText: String = "",
    val createdAtEpochMs: Long,
    val durationMs: Long,
    val languageTag: String,
    val wordCount: Int
)

/**
 * Standalone FTS4 index (UUID PK on [DictationEntity] blocks contentEntity mode).
 * Synced by [DictationRepository] on save / delete / purge.
 */
@Fts4
@Entity(tableName = "dictations_fts")
data class DictationFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int = 0,
    val sessionId: String,
    val text: String,
    val rawText: String = ""
)

@Entity(tableName = "dictionary_words")
data class DictionaryWordEntity(
    @PrimaryKey val id: String,
    val word: String,
    val replacement: String,
    val createdAtEpochMs: Long
)

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey val id: String,
    val trigger: String,
    val body: String,
    val createdAtEpochMs: Long
)

@Entity(tableName = "app_stats")
data class AppStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalWords: Long = 0,
    val totalSessions: Long = 0,
    val lastDayEpoch: Long = 0,
    val streakDays: Int = 0
)
