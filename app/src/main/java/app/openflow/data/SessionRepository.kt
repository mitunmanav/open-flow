package app.openflow.data

import app.openflow.search.SearchHit
import app.openflow.search.TranscriptSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class SessionRepository(private val dao: SessionDao) {

    fun observeSessions(): Flow<List<SessionEntity>> = dao.observeAll()

    suspend fun saveSession(
        transcript: String,
        audioPath: String?,
        durationMs: Long,
        languageTag: String,
        title: String? = null
    ): SessionEntity {
        val now = System.currentTimeMillis()
        val body = transcript.trim()
        val entity = SessionEntity(
            id = UUID.randomUUID().toString(),
            title = title?.ifBlank { null } ?: defaultTitle(body, now),
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            durationMs = durationMs,
            audioPath = audioPath,
            transcript = body,
            languageTag = languageTag
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun updateTranscript(id: String, transcript: String) {
        val existing = dao.getById(id) ?: return
        dao.upsert(
            existing.copy(
                transcript = transcript,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun search(query: String): List<SessionEntity> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val all = dao.observeAll().first()
        return try {
            val fts = dao.searchFts(sanitizeFts(q))
            if (fts.isNotEmpty()) fts else memoryFilter(all, q)
        } catch (_: Exception) {
            memoryFilter(all, q)
        }
    }

    fun observeSearchHits(): Flow<List<SearchHit>> =
        dao.observeAll().map { list ->
            list.map {
                SearchHit(it.id, it.title, it.transcript, it.createdAtEpochMs)
            }
        }

    private fun memoryFilter(all: List<SessionEntity>, q: String): List<SessionEntity> {
        val hits = TranscriptSearch.filter(
            all.map { SearchHit(it.id, it.title, it.transcript, it.createdAtEpochMs) },
            q
        ).map { it.id }.toSet()
        return all.filter { it.id in hits }
    }

    private fun sanitizeFts(q: String): String {
        val cleaned = q.replace(Regex("[^\"\\w\\s-]"), " ").trim()
        if (cleaned.isEmpty()) return "\"\""
        return cleaned.split(Regex("\\s+")).joinToString(" ") { token ->
            "\"$token*\""
        }
    }

    private fun defaultTitle(transcript: String, now: Long): String {
        val first = transcript.lineSequence().firstOrNull()?.take(48)?.trim().orEmpty()
        return first.ifBlank { "Session $now" }
    }
}
