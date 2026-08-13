package app.openflow.data

import androidx.room.withTransaction
import app.openflow.privacy.RetentionPolicy
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit

class DictationRepository(
    private val db: OpenFlowDatabase,
    private val dictationDao: DictationDao,
    private val dictionaryDao: DictionaryDao,
    private val snippetDao: SnippetDao,
    private val statsDao: StatsDao
) {
    fun observeDictations(): Flow<List<DictationEntity>> = dictationDao.observeAll()
    fun observeDictionary(): Flow<List<DictionaryWordEntity>> = dictionaryDao.observeAll()
    fun observeSnippets(): Flow<List<SnippetEntity>> = snippetDao.observeAll()

    suspend fun dictionaryMap(): Map<String, String> =
        dictionaryDao.all().associate { it.word to it.replacement.ifBlank { it.word } }

    suspend fun snippetMap(): Map<String, String> =
        snippetDao.all().associate { it.trigger to it.body }

    /**
     * Persist a dictation with raw STT + clean text.
     * [retentionPolicy]: keep | wipe_24h | never_store
     * Returns null when never_store (history skipped).
     */
    suspend fun saveDictation(
        rawText: String,
        cleanText: String,
        durationMs: Long,
        languageTag: String,
        retentionPolicy: String = "keep"
    ): DictationEntity? {
        if (!RetentionPolicy.shouldPersist(retentionPolicy)) return null
        RetentionPolicy.cutoffEpochMs(System.currentTimeMillis(), retentionPolicy)?.let {
            dictationDao.deleteOlderThan(it)
        }
        val words = cleanText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        val e = DictationEntity(
            id = UUID.randomUUID().toString(),
            text = cleanText,
            rawText = rawText,
            createdAtEpochMs = System.currentTimeMillis(),
            durationMs = durationMs,
            languageTag = languageTag,
            wordCount = words
        )
        dictationDao.upsert(e)
        bumpStats(words)
        return e
    }

    suspend fun purgeOnLaunch(retentionPolicy: String, nowEpochMs: Long = System.currentTimeMillis()) {
        val cut = RetentionPolicy.cutoffEpochMs(nowEpochMs, retentionPolicy) ?: return
        dictationDao.deleteOlderThan(cut)
    }

    /** Compat: single string → both raw and clean (pre-pipeline callers). */
    suspend fun saveDictation(text: String, durationMs: Long, languageTag: String): DictationEntity? =
        saveDictation(rawText = text, cleanText = text, durationMs = durationMs, languageTag = languageTag)

    suspend fun deleteDictation(id: String) = dictationDao.delete(id)

    suspend fun latestText(): String? = dictationDao.latest()?.text

    suspend fun addWord(word: String, replacement: String = word) {
        val w = word.trim()
        dictionaryDao.upsert(
            DictionaryWordEntity(
                id = w,
                word = w,
                replacement = replacement.trim().ifBlank { w },
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteWord(id: String) = dictionaryDao.delete(id)

    suspend fun addSnippet(trigger: String, body: String) {
        val t = trigger.trim()
        snippetDao.upsert(
            SnippetEntity(
                id = t,
                trigger = t,
                body = body,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSnippet(id: String) = snippetDao.delete(id)

    suspend fun stats(): AppStatsEntity = statsDao.get() ?: AppStatsEntity()

    private suspend fun bumpStats(words: Int) {
        db.withTransaction {
            val cur = statsDao.get() ?: AppStatsEntity()
            val day = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
            val streak = when {
                cur.lastDayEpoch == 0L -> 1
                day == cur.lastDayEpoch -> cur.streakDays
                day == cur.lastDayEpoch + 1 -> cur.streakDays + 1
                else -> 1
            }
            statsDao.upsert(
                cur.copy(
                    totalWords = cur.totalWords + words,
                    totalSessions = cur.totalSessions + 1,
                    lastDayEpoch = day,
                    streakDays = streak
                )
            )
        }
    }
}
