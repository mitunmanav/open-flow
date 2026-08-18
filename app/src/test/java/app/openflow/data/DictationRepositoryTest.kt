package app.openflow.data

import app.openflow.text.LearnEngine
import app.openflow.text.TextPostProcessor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DictationRepositoryTest {

    @Before
    fun resetLearn() {
        LearnEngine.resetLearn()
    }

    @Test
    fun destructive_fallback_forbidden_and_version_matches() {
        assertThat(RoomOpenPolicy.ALLOW_DESTRUCTIVE_FALLBACK).isFalse()
        assertThat(RoomOpenPolicy.VERSION).isEqualTo(8)
        assertThat(OpenFlowMigrations.MIGRATION_5_6.startVersion).isEqualTo(5)
        assertThat(OpenFlowMigrations.MIGRATION_5_6.endVersion).isEqualTo(6)
        assertThat(OpenFlowMigrations.MIGRATION_6_7.startVersion).isEqualTo(6)
        assertThat(OpenFlowMigrations.MIGRATION_6_7.endVersion).isEqualTo(7)
        assertThat(OpenFlowMigrations.MIGRATION_7_8.startVersion).isEqualTo(7)
        assertThat(OpenFlowMigrations.MIGRATION_7_8.endVersion).isEqualTo(8)
    }

    @Test
    fun observeRecent_limits_observeAll_keeps_full() = runTest {
        val f = fakes()
        val now = 2_000L
        f.dict.upsert(row("a", "one", now - 30))
        f.dict.upsert(row("b", "two", now - 20))
        f.dict.upsert(row("c", "three", now - 10))

        val recent = f.repo.observeRecentDictations(2).first()
        assertThat(recent.map { it.id }).containsExactly("c", "b").inOrder()

        val all = f.repo.observeDictations().first()
        assertThat(all.map { it.id }).containsExactly("c", "b", "a").inOrder()
    }

    @Test
    fun updateDictationText_false_on_miss_true_on_hit() = runTest {
        val f = fakes()
        f.dict.upsert(row("hit", "old", 1L))

        assertThat(f.repo.updateDictationText("missing", "nope")).isFalse()
        assertThat(f.dict.get("missing")).isNull()

        assertThat(f.repo.updateDictationText("hit", "new words here")).isTrue()
        val e = f.dict.get("hit")!!
        assertThat(e.text).isEqualTo("new words here")
        assertThat(e.wordCount).isEqualTo(3)
    }

    @Test
    fun never_store_skips_history() = runTest {
        val f = fakes()
        val saved = f.repo.saveDictation(
            rawText = "raw",
            cleanText = "clean",
            durationMs = 10L,
            languageTag = "en-US",
            retentionPolicy = "never_store"
        )
        assertThat(saved).isNull()
        assertThat(f.repo.observeDictations().first()).isEmpty()
    }

    @Test
    fun keep_saves_and_does_not_purge() = runTest {
        val f = fakes()
        val old = System.currentTimeMillis() - 48L * 60L * 60L * 1000L
        f.dict.upsert(row("old", "ancient", old))

        val saved = f.repo.saveDictation(
            rawText = "raw hi",
            cleanText = "hi",
            durationMs = 5L,
            languageTag = "en-US",
            retentionPolicy = "keep"
        )
        assertThat(saved).isNotNull()
        assertThat(saved!!.text).isEqualTo("hi")
        assertThat(saved.rawText).isEqualTo("raw hi")
        assertThat(f.repo.observeDictations().first().map { it.id })
            .containsAtLeast("old", saved.id)
    }

    @Test
    fun wipe_24h_drops_old_on_save_and_purge() = runTest {
        val f = fakes()
        val now = System.currentTimeMillis()
        val day = 24L * 60L * 60L * 1000L
        f.dict.upsert(row("old", "gone", now - day - 5_000L))
        f.dict.upsert(row("fresh", "stay", now - 1_000L))

        f.repo.purgeOnLaunch("wipe_24h", now)
        assertThat(f.repo.observeDictations().first().map { it.id }).containsExactly("fresh")

        f.dict.upsert(row("stale", "drop", now - day - 10_000L))
        val saved = f.repo.saveDictation(
            rawText = "x",
            cleanText = "x",
            durationMs = 1L,
            languageTag = "en-US",
            retentionPolicy = "wipe_24h"
        )
        assertThat(saved).isNotNull()
        val ids = f.repo.observeDictations().first().map { it.id }
        assertThat(ids).contains(saved!!.id)
        assertThat(ids).contains("fresh")
        assertThat(ids).doesNotContain("stale")
    }

    @Test
    fun learnFromEdit_calls_engine_and_stores_word() = runTest {
        val f = fakes()
        val pairs = f.repo.learnFromEdit("Meet Mitton", "Meet Mitun")
        assertThat(pairs.map { it.from to it.to }).containsExactly("Mitton" to "Mitun")
        val words = f.repo.observeDictionary().first()
        assertThat(words.single { it.word == "Mitton" }.replacement).isEqualTo("Mitun")
    }

    @Test
    fun clearLearned_wipes_dict_and_sides() = runTest {
        val f = fakes()
        f.repo.addWord("foo", "bar")
        f.repo.learnFromEdit("Meet Mitton", "Meet Mitun")
        assertThat(f.repo.dictionaryMap()).isNotEmpty()
        assertThat(LearnEngine.autoKeys()).isNotEmpty()
        f.repo.clearLearned()
        assertThat(f.repo.dictionaryMap()).isEmpty()
        assertThat(LearnEngine.autoKeys()).isEmpty()
        assertThat(LearnEngine.sideBags()).isEmpty()
    }

    @Test
    fun add_delete_word_snippet_and_dictation() = runTest {
        val f = fakes()
        f.repo.addWord("foo", "bar")
        f.repo.addSnippet("/sig", "thanks")
        val d = f.repo.saveDictation("raw", "clean text", 2L, "en-US", "keep")!!

        assertThat(f.repo.dictionaryMap()).containsEntry("foo", "bar")
        assertThat(f.repo.snippetMap()).containsEntry("/sig", "thanks")
        assertThat(f.repo.latestText()).isEqualTo("clean text")

        f.repo.deleteWord("foo")
        f.repo.deleteSnippet("/sig")
        f.repo.deleteDictation(d.id)

        assertThat(f.repo.dictionaryMap()).isEmpty()
        assertThat(f.repo.snippetMap()).isEmpty()
        assertThat(f.repo.observeDictations().first()).isEmpty()
    }

    @Test
    fun reverse_mic_to_mike_forgets() = runTest {
        val f = fakes()
        f.repo.learnFromEdit("Mike", "Mic")
        assertThat(f.repo.dictionaryMap()).containsEntry("Mike", "Mic")
        val pairs = f.repo.learnFromEdit("Mic", "Mike")
        assertThat(pairs).isEmpty()
        assertThat(f.repo.dictionaryMap()).isEmpty()
        assertThat(LearnEngine.autoKeys()).doesNotContain("mike")
    }

    @Test
    fun no_cycle() = runTest {
        val f = fakes()
        f.repo.learnFromEdit("Mike", "Mic")
        f.repo.learnFromEdit("Mic", "Mike")
        val map = f.repo.dictionaryMap()
        assertThat(map).doesNotContainKey("Mic")
        assertThat(map).doesNotContainKey("Mike")
    }

    @Test
    fun learnFromEdit_stores_side_bag() = runTest {
        val f = fakes()
        f.repo.learnFromEdit("turn on Mike", "turn on Mic")
        assertThat(LearnEngine.sideBags()["mike"]).containsExactly("turn")
        assertThat(LearnEngine.autoKeys()).contains("mike")
        val out = TextPostProcessor.applyDictionary(
            "turn on Mike",
            f.repo.dictionaryMap(),
            sides = LearnEngine.sideBags(),
            autoKeys = LearnEngine.autoKeys()
        )
        assertThat(out).isEqualTo("turn on Mic")
    }

    @Test
    fun addWord_manual_is_bold() = runTest {
        val f = fakes()
        f.repo.addWord("Mike", "Mic")
        assertThat(LearnEngine.autoKeys()).doesNotContain("mike")
        val out = TextPostProcessor.applyDictionary(
            "ask Mike tomorrow",
            f.repo.dictionaryMap(),
            sides = LearnEngine.sideBags(),
            autoKeys = LearnEngine.autoKeys()
        )
        assertThat(out).contains("Mic")
    }

    @Test
    fun forget_drops_row_and_bag() = runTest {
        val f = fakes()
        f.repo.learnFromEdit("Mike", "Mic")
        f.repo.forget("Mike")
        assertThat(f.repo.dictionaryMap()).isEmpty()
        assertThat(LearnEngine.autoKeys()).doesNotContain("mike")
        assertThat(LearnEngine.sideBags()).doesNotContainKey("mike")
    }

    @Test
    fun save_bumps_stats() = runTest {
        val f = fakes()
        f.repo.saveDictation("a b", "a b", 1L, "en-US", "keep")
        val s = f.repo.stats()
        assertThat(s.totalSessions).isEqualTo(1)
        assertThat(s.totalWords).isEqualTo(2)
    }

    @Test
    fun search_blank_returns_all_ordered() = runTest {
        val f = fakes()
        f.dict.upsert(row("a", "alpha note", 10L))
        f.dict.upsert(row("b", "beta note", 20L))
        val hits = f.repo.searchDictations("")
        assertThat(hits.map { it.id }).containsExactly("b", "a").inOrder()
    }

    @Test
    fun search_matches_clean_and_raw_via_fts() = runTest {
        val f = fakes()
        f.repo.saveDictation("raw zebra sound", "clean tiger", 1L, "en-US", "keep")
        f.repo.saveDictation("other", "nope", 1L, "en-US", "keep")
        val byClean = f.repo.searchDictations("tiger")
        assertThat(byClean).hasSize(1)
        assertThat(byClean.first().text).isEqualTo("clean tiger")
        val byRaw = f.repo.searchDictations("zebra")
        assertThat(byRaw).hasSize(1)
        assertThat(byRaw.first().rawText).contains("zebra")
    }

    @Test
    fun search_strips_dangerous_fts_ops() = runTest {
        val f = fakes()
        f.repo.saveDictation("x", "safe token here", 1L, "en-US", "keep")
        // Must not throw; operators stripped → still finds token.
        val hits = f.repo.searchDictations("safe* AND \"token\"")
        assertThat(hits).hasSize(1)
    }

    @Test
    fun wipe_purges_fts_with_rows() = runTest {
        val f = fakes()
        val now = 1_000_000L
        val day = 24L * 60L * 60L * 1000L
        f.dict.upsert(row("old", "old clean", now - day - 5_000L).copy(rawText = "old raw"))
        f.fts.upsert(DictationFtsEntity(sessionId = "old", text = "old clean", rawText = "old raw"))
        f.repo.purgeOnLaunch("wipe_24h", now)
        assertThat(f.repo.observeDictations().first()).isEmpty()
        assertThat(f.fts.sessionIds()).isEmpty()
        assertThat(f.repo.searchDictations("old")).isEmpty()
    }

    @Test
    fun import_dictionary_skips_snippet_conflict() = runTest {
        val f = fakes()
        f.repo.addSnippet("sig", "Best regards")
        val out = f.repo.importDictionary("sig,signature\nwisper,Wispr\n")
        assertThat(out.added).isEqualTo(1)
        assertThat(out.conflicts).isEqualTo(1)
        assertThat(f.repo.dictionaryMap()).containsEntry("wisper", "Wispr")
        assertThat(f.repo.dictionaryMap()).doesNotContainKey("sig")
        assertThat(f.repo.snippetMap()).containsEntry("sig", "Best regards")
    }

    @Test
    fun import_snippets_skips_dict_conflict() = runTest {
        val f = fakes()
        f.repo.addWord("addr", "address")
        val out = f.repo.importSnippets("addr,123 Main\nsig,Best regards\n")
        assertThat(out.added).isEqualTo(1)
        assertThat(out.conflicts).isEqualTo(1)
        assertThat(f.repo.snippetMap()).containsEntry("sig", "Best regards")
        assertThat(f.repo.snippetMap()).doesNotContainKey("addr")
    }

    @Test
    fun addWord_false_when_snippet_owns_word() = runTest {
        val f = fakes()
        f.repo.addSnippet("foo", "block")
        assertThat(f.repo.addWord("foo", "bar")).isFalse()
        assertThat(f.repo.dictionaryMap()).isEmpty()
    }

    @Test
    fun operations_execute_inside_transactions() = runTest {
        var txCount = 0
        val trackingDb = object : OpenFlowDb {
            override suspend fun <R> transact(block: suspend () -> R): R {
                txCount++
                return block()
            }
        }
        val dict = FakeDictationDao()
        val fts = FakeDictationFtsDao { dict.ids() }
        val words = FakeDictionaryDao()
        val snips = FakeSnippetDao()
        val stats = FakeStatsDao()
        val voice = FakeVoiceProfileDao()
        val repo = DictationRepository(
            db = trackingDb,
            dictationDao = dict,
            ftsDao = fts,
            dictionaryDao = words,
            snippetDao = snips,
            statsDao = stats,
            voiceProfileDao = voice,
        )

        val saved = repo.saveDictation("raw", "clean", 100L, "en-US")
        assertThat(saved).isNotNull()
        assertThat(txCount).isAtLeast(1)

        val countBeforeUpdate = txCount
        repo.updateDictationText(saved!!.id, "updated clean")
        assertThat(txCount).isGreaterThan(countBeforeUpdate)

        val countBeforeDelete = txCount
        repo.deleteDictation(saved.id)
        assertThat(txCount).isGreaterThan(countBeforeDelete)

        val countBeforePurge = txCount
        repo.purgeOnLaunch("wipe_24h")
        assertThat(txCount).isGreaterThan(countBeforePurge)
    }

    private fun row(id: String, text: String, createdAt: Long) = DictationEntity(
        id = id,
        text = text,
        rawText = text,
        createdAtEpochMs = createdAt,
        durationMs = 1L,
        languageTag = "en-US",
        wordCount = text.split(Regex("\\s+")).size
    )

    private fun fakes(): Harness {
        val dict = FakeDictationDao()
        val fts = FakeDictationFtsDao { dict.ids() }
        val words = FakeDictionaryDao()
        val snips = FakeSnippetDao()
        val stats = FakeStatsDao()
        val voice = FakeVoiceProfileDao()
        val repo = DictationRepository(
            db = PassthroughDb,
            dictationDao = dict,
            ftsDao = fts,
            dictionaryDao = words,
            snippetDao = snips,
            statsDao = stats,
            voiceProfileDao = voice,
        )
        return Harness(repo, dict, fts)
    }

    private data class Harness(
        val repo: DictationRepository,
        val dict: FakeDictationDao,
        val fts: FakeDictationFtsDao,
    )
}

private object PassthroughDb : OpenFlowDb {
    override suspend fun <R> transact(block: suspend () -> R): R = block()
}

private class FakeDictationDao : DictationDao {
    private val items = LinkedHashMap<String, DictationEntity>()
    private val flow = MutableStateFlow<List<DictationEntity>>(emptyList())

    private fun publish() {
        flow.value = items.values.sortedByDescending { it.createdAtEpochMs }
    }

    override fun observeAll(): Flow<List<DictationEntity>> = flow

    override fun observeRecent(limit: Int): Flow<List<DictationEntity>> =
        flow.map { it.take(limit.coerceAtLeast(0)) }

    override suspend fun upsert(d: DictationEntity) {
        items[d.id] = d
        publish()
    }

    override suspend fun delete(id: String) {
        items.remove(id)
        publish()
    }

    override suspend fun deleteOlderThan(beforeEpochMs: Long) {
        items.values.removeAll { it.createdAtEpochMs < beforeEpochMs }
        publish()
    }

    override suspend fun latest(): DictationEntity? = flow.value.firstOrNull()

    override suspend fun get(id: String): DictationEntity? = items[id]

    fun ids(): Set<String> = items.keys.toSet()

    override suspend fun searchFts(query: String): List<DictationEntity> {
        val tokens = query.replace("*", "").lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return flow.value
        return flow.value.filter { row ->
            val hay = (row.text + " " + row.rawText).lowercase()
            tokens.all { hay.contains(it) }
        }
    }

    override suspend fun allOrdered(): List<DictationEntity> = flow.value
}

private class FakeDictationFtsDao(
    private val liveSessionIds: () -> Set<String>,
) : DictationFtsDao {
    private val bySession = LinkedHashMap<String, DictationFtsEntity>()

    override suspend fun upsert(row: DictationFtsEntity) {
        bySession[row.sessionId] = row
    }

    override suspend fun deleteBySessionId(sessionId: String) {
        bySession.remove(sessionId)
    }

    override suspend fun deleteOrphans() {
        bySession.keys.filter { it !in liveSessionIds() }.forEach { bySession.remove(it) }
    }

    override suspend fun clearAll() {
        bySession.clear()
    }

    fun sessionIds(): Set<String> = bySession.keys.toSet()
}

private class FakeDictionaryDao : DictionaryDao {
    private val items = LinkedHashMap<String, DictionaryWordEntity>()
    private val flow = MutableStateFlow<List<DictionaryWordEntity>>(emptyList())

    private fun publish() {
        flow.value = items.values.sortedBy { it.word.lowercase() }
    }

    override fun observeAll(): Flow<List<DictionaryWordEntity>> = flow

    override suspend fun all(): List<DictionaryWordEntity> = items.values.toList()

    override suspend fun upsert(w: DictionaryWordEntity) {
        items[w.id] = w
        publish()
    }

    override suspend fun delete(id: String) {
        items.remove(id)
        publish()
    }

    override suspend fun deleteAll() {
        items.clear()
        publish()
    }
}

private class FakeSnippetDao : SnippetDao {
    private val items = LinkedHashMap<String, SnippetEntity>()
    private val flow = MutableStateFlow<List<SnippetEntity>>(emptyList())

    private fun publish() {
        flow.value = items.values.sortedBy { it.trigger.lowercase() }
    }

    override fun observeAll(): Flow<List<SnippetEntity>> = flow

    override suspend fun all(): List<SnippetEntity> = items.values.toList()

    override suspend fun upsert(s: SnippetEntity) {
        items[s.id] = s
        publish()
    }

    override suspend fun delete(id: String) {
        items.remove(id)
        publish()
    }
}

private class FakeStatsDao : StatsDao {
    private var row: AppStatsEntity? = null

    override suspend fun get(): AppStatsEntity? = row

    override suspend fun upsert(s: AppStatsEntity) {
        row = s
    }
}

private class FakeVoiceProfileDao : VoiceProfileDao {
    private var row: VoiceProfileEntity? = null

    override suspend fun get(): VoiceProfileEntity? = row

    override suspend fun upsert(row: VoiceProfileEntity) {
        this.row = row
    }
}
