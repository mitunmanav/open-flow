package app.openflow.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit

class DictationRepository(
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
     * [text] on the entity stays clean (UI-compatible). [wordCount] from clean only.
     */
    suspend fun saveDictation(
        rawText: String,
        cleanText: String,
        durationMs: Long,
        languageTag: String
    ): DictationEntity {
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

    /** Compat: single string → both raw and clean (pre-pipeline callers). */
    suspend fun saveDictation(text: String, durationMs: Long, languageTag: String): DictationEntity =
        saveDictation(rawText = text, cleanText = text, durationMs = durationMs, languageTag = languageTag)

    suspend fun deleteDictation(id: String) = dictationDao.delete(id)

    suspend fun latestText(): String? = dictationDao.latest()?.text

    suspend fun addWord(word: String, replacement: String = word) {
        dictionaryDao.upsert(
            DictionaryWordEntity(
                id = UUID.randomUUID().toString(),
                word = word.trim(),
                replacement = replacement.trim().ifBlank { word.trim() },
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteWord(id: String) = dictionaryDao.delete(id)

    suspend fun addSnippet(trigger: String, body: String) {
        snippetDao.upsert(
            SnippetEntity(
                id = UUID.randomUUID().toString(),
                trigger = trigger.trim(),
                body = body,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSnippet(id: String) = snippetDao.delete(id)

    suspend fun stats(): AppStatsEntity = statsDao.get() ?: AppStatsEntity()

    private suspend fun bumpStats(words: Int) {
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
