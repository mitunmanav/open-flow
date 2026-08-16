package app.openflow.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictations",
    indices = [Index("createdAtEpochMs")]
)
data class DictationEntity(
    @PrimaryKey val id: String,
    /** Clean / polished transcript (UI shows this). */
    val text: String,
    /** Original STT before cleanup. Empty for pre-v4 rows. */
    val rawText: String = "",
    val createdAtEpochMs: Long,
    val durationMs: Long,
    val languageTag: String,
    val wordCount: Int,
    /** Focused app package at insert time; empty for pre-v7 rows. */
    val packageName: String = "",
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

/** Cached BYOK Voice flavor (aggregates-only refresh). */
@Entity(tableName = "voice_profile")
data class VoiceProfileEntity(
    @PrimaryKey val id: Int = 1,
    val archetype: String = "",
    val catchphrase: String = "",
    val headline: String = "",
    val generatedAtEpochMs: Long = 0L,
    val provider: String = "",
    val model: String = "",
)
