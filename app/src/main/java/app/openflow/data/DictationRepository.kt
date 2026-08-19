package app.openflow.data

import app.openflow.privacy.RetentionPolicy
import app.openflow.text.LearnPair
import app.openflow.text.LearnEngine
import app.openflow.text.PairImport
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit

class DictationRepository(
    private val db: OpenFlowDb,
    private val dictationDao: DictationDao,
    private val ftsDao: DictationFtsDao,
    private val dictionaryDao: DictionaryDao,
    private val snippetDao: SnippetDao,
    private val statsDao: StatsDao,
    private val voiceProfileDao: VoiceProfileDao,
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
     * FTS search over clean + raw text. Blank / noise query → full ordered list.
     * Local only — no network.
     */
    suspend fun searchDictations(query: String): List<DictationEntity> {
        val match = FtsQuery.sanitize(query) ?: return dictationDao.allOrdered()
        return dictationDao.searchFts(match)
    }

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
        retentionPolicy: String = "keep",
        packageName: String = "",
        createdAtEpochMs: Long = System.currentTimeMillis(),
        processStatus: String = ProcessStatus.OK,
        id: String = UUID.randomUUID().toString(),
    ): DictationEntity? {
        if (!RetentionPolicy.shouldPersist(retentionPolicy)) return null
        return db.transact {
            RetentionPolicy.cutoffEpochMs(System.currentTimeMillis(), retentionPolicy)?.let {
                dictationDao.deleteOlderThan(it)
                ftsDao.deleteOrphans()
            }
            val words = cleanText.trim().split(WORD_SPLIT).filter { it.isNotEmpty() }.size
            val e = DictationEntity(
                id = id,
                text = cleanText,
                rawText = rawText,
                createdAtEpochMs = createdAtEpochMs,
                durationMs = durationMs,
                languageTag = languageTag,
                wordCount = words,
                packageName = packageName.trim(),
                processStatus = ProcessStatus.normalize(processStatus),
            )
            dictationDao.upsert(e)
            indexFts(e)
            if (ProcessStatus.isFailed(e.processStatus).not() && words > 0) {
                bumpStatsInternal(words)
            }
            e
        }
    }

    /** Mark a failed session as successfully processed after bubble retry. */
    suspend fun markDictationOk(id: String, rawText: String, cleanText: String): Boolean {
        val existing = dictationDao.get(id) ?: return false
        val words = cleanText.trim().split(WORD_SPLIT).filter { it.isNotEmpty() }.size
        val updated = existing.copy(
            text = cleanText,
            rawText = rawText,
            wordCount = words,
            processStatus = ProcessStatus.OK,
        )
        db.transact {
            dictationDao.upsert(updated)
            indexFts(updated)
            if (words > 0 && ProcessStatus.isFailed(existing.processStatus)) {
                bumpStatsInternal(words)
            }
        }
        return true
    }

    suspend fun purgeOnLaunch(retentionPolicy: String, nowEpochMs: Long = System.currentTimeMillis()) {
        val cut = RetentionPolicy.cutoffEpochMs(nowEpochMs, retentionPolicy) ?: return
        db.transact {
            dictationDao.deleteOlderThan(cut)
            ftsDao.deleteOrphans()
        }
    }

    /** Compat: single string → both raw and clean (pre-pipeline callers). */
    suspend fun saveDictation(text: String, durationMs: Long, languageTag: String): DictationEntity? =
        saveDictation(rawText = text, cleanText = text, durationMs = durationMs, languageTag = languageTag)

    suspend fun deleteDictation(id: String) {
        db.transact {
            dictationDao.delete(id)
            ftsDao.deleteBySessionId(id)
        }
    }

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
            val bag = LearnEngine.sideBag(inserted, p.from)
            val confirm = LearnEngine.noteAutoCandidate(p.from, p.to, bag)
            if (!confirm.persisted) continue
            if (!upsertDictRow(p.from, p.to)) {
                LearnEngine.drop(p.from)
                continue
            }
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
        return db.transact {
            val e = dictationDao.get(id) ?: return@transact false
            val words = newText.trim().split(WORD_SPLIT).filter { it.isNotEmpty() }.size
            val updated = e.copy(text = newText, wordCount = words)
            dictationDao.upsert(updated)
            indexFts(updated)
            true
        }
    }

    /** @return false when [word] is already a snippet trigger. */
    suspend fun addWord(word: String, replacement: String = word): Boolean {
        val w = word.trim()
        if (w.isEmpty()) return false
        if (!upsertDictRow(w, replacement)) return false
        LearnEngine.putManual(w)
        return true
    }

    private suspend fun upsertDictRow(word: String, replacement: String): Boolean {
        val w = word.trim()
        if (w.isEmpty()) return false
        val snip = snippetMap().keys
        if (PairImport.decide(w, dictionaryMap().keys, snip, PairImport.Kind.DICT) ==
            PairImport.Decision.CONFLICT
        ) {
            return false
        }
        dictionaryDao.upsert(
            DictionaryWordEntity(
                id = w,
                word = w,
                replacement = replacement.trim().ifBlank { w },
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun importDictionary(text: String): PairImport.Outcome =
        importPairs(text, PairImport.Kind.DICT)

    suspend fun importSnippets(text: String): PairImport.Outcome =
        importPairs(text, PairImport.Kind.SNIPPET)

    private suspend fun importPairs(text: String, kind: PairImport.Kind): PairImport.Outcome {
        val parsed = PairImport.parse(text)
        var added = 0
        var skipped = parsed.skipped
        var conflicts = 0
        val dict = dictionaryMap().keys.toMutableSet()
        val snip = snippetMap().keys.toMutableSet()
        for (row in parsed.rows) {
            when (PairImport.decide(row.from, dict, snip, kind)) {
                PairImport.Decision.ADD -> {
                    if (kind == PairImport.Kind.DICT) {
                        addWord(row.from, row.to)
                        dict += row.from
                    } else {
                        addSnippet(row.from, row.to)
                        snip += row.from
                    }
                    added++
                }
                PairImport.Decision.SKIP_DUP -> skipped++
                PairImport.Decision.CONFLICT -> conflicts++
            }
        }
        return PairImport.Outcome(added, skipped, conflicts)
    }

    suspend fun deleteWord(id: String) {
        dictionaryDao.delete(id)
        LearnEngine.drop(id)
    }

    suspend fun clearLearned() {
        dictionaryDao.deleteAll()
        LearnEngine.clearAll()
    }

    /** @return false when [trigger] is already a dictionary word. */
    suspend fun addSnippet(trigger: String, body: String): Boolean {
        val t = trigger.trim()
        if (t.isEmpty() || body.isBlank()) return false
        if (PairImport.decide(
                t,
                dictionaryMap().keys,
                snippetMap().keys,
                PairImport.Kind.SNIPPET
            ) == PairImport.Decision.CONFLICT
        ) {
            return false
        }
        snippetDao.upsert(
            SnippetEntity(
                id = t,
                trigger = t,
                body = body,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun deleteSnippet(id: String) = snippetDao.delete(id)

    suspend fun stats(): AppStatsEntity = statsDao.get() ?: AppStatsEntity()

    suspend fun allForInsights(): List<DictationEntity> = dictationDao.allOrdered()

    suspend fun voiceProfile(): VoiceProfileEntity? = voiceProfileDao.get()

    suspend fun saveVoiceProfile(e: VoiceProfileEntity) {
        voiceProfileDao.upsert(e.copy(id = 1))
    }

    private suspend fun indexFts(e: DictationEntity) {
        ftsDao.deleteBySessionId(e.id)
        ftsDao.upsert(
            DictationFtsEntity(
                sessionId = e.id,
                text = e.text,
                rawText = e.rawText
            )
        )
    }

    private suspend fun bumpStatsInternal(words: Int) {
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

    private suspend fun bumpStats(words: Int) {
        db.transact {
            bumpStatsInternal(words)
        }
    }

    companion object {
        private val WORD_SPLIT = Regex("\\s+")
    }
}
