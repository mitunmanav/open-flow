package app.openflow.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DictationRepositoryTest {

    @Test
    fun destructive_fallback_forbidden_and_version_unbumped() {
        assertThat(RoomOpenPolicy.ALLOW_DESTRUCTIVE_FALLBACK).isFalse()
        assertThat(RoomOpenPolicy.VERSION).isEqualTo(4)
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
    fun save_bumps_stats() = runTest {
        val f = fakes()
        f.repo.saveDictation("a b", "a b", 1L, "en-US", "keep")
        val s = f.repo.stats()
        assertThat(s.totalSessions).isEqualTo(1)
        assertThat(s.totalWords).isEqualTo(2)
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
        val words = FakeDictionaryDao()
        val snips = FakeSnippetDao()
        val stats = FakeStatsDao()
        val repo = DictationRepository(
            db = PassthroughDb,
            dictationDao = dict,
            dictionaryDao = words,
            snippetDao = snips,
            statsDao = stats
        )
        return Harness(repo, dict)
    }

    private data class Harness(val repo: DictationRepository, val dict: FakeDictationDao)
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
