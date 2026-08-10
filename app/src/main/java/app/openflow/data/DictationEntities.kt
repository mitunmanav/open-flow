package app.openflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictations")
data class DictationEntity(
    @PrimaryKey val id: String,
    val text: String,
    val createdAtEpochMs: Long,
    val durationMs: Long,
    val languageTag: String,
    val wordCount: Int
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
