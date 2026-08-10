package app.openflow.data

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val durationMs: Long,
    val audioPath: String?,
    val transcript: String,
    val languageTag: String,
    val tags: String = "",
    val notes: String = ""
)

@Entity(tableName = "sessions_fts")
@Fts4(contentEntity = SessionEntity::class)
data class SessionFts(
    val title: String,
    val transcript: String,
    val tags: String,
    val notes: String
)
