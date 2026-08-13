package app.openflow.data

import app.openflow.privacy.RetentionPolicy
import app.openflow.text.LearnPair
import app.openflow.text.LearnEngine
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit

class DictationRepository(
    private val db: OpenFlowDb,
    private val dictationDao: DictationDao,
    private val dictionaryDao: DictionaryDao,
    private val snippetDao: SnippetDao,
    private val statsDao: StatsDao
) {
    fun observeDictations(): Flow<List<DictationEntity>> = dictationDao.observeAll()

    fun observeRecentDictations(limit: Int): Flow<List<DictationEntity>> =
        dictationDao.observeRecent(limit.coerceAtLeast(1))

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

    suspend fun learnFromEdit(inserted: String, edited: String): List<LearnPair> {
        val pairs = LearnEngine.pairsFromEdit(inserted, edited)
        if (pairs.isEmpty()) return emptyList()
        val existing = dictionaryMap()
        val kept = ArrayList<LearnPair>(pairs.size)
        for (p in pairs) {
            val reverse = LearnEngine.reverseKey(p.from, p.to, existing)
            if (reverse != null || LearnEngine.wouldCycle(p.from, p.to, existing)) {
                forget(reverse ?: p.to)
                continue
            }
            addWord(p.from, p.to)
            LearnEngine.putAuto(p.from, LearnEngine.sideBag(inserted, p.from))
            kept.add(p)
        }
        return kept
    }

    suspend fun forget(from: String) {
        val key = dictionaryMap().keys.find { it.equals(from, ignoreCase = true) }
        if (key != null) dictionaryDao.delete(key)
        LearnEngine.drop(from)
        if (key != null && !key.equals(from, ignoreCase = true)) LearnEngine.drop(key)
    }

    /** @return false when [id] is missing (no silent no-op). */
    suspend fun updateDictationText(id: String, newText: String): Boolean {
        val e = dictationDao.get(id) ?: return false
        val words = newText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        dictationDao.upsert(e.copy(text = newText, wordCount = words))
        return true
    }

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
        LearnEngine.putManual(w)
    }

    suspend fun deleteWord(id: String) {
        dictionaryDao.delete(id)
        LearnEngine.drop(id)
    }

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
        db.transact {
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
