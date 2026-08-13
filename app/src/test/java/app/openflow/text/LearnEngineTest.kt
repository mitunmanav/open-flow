package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearnEngineTest {

    @Test
    fun mitton_to_mitun_one_word() {
        val pairs = LearnEngine.pairsFromEdit("Mitton", "Mitun")
        assertThat(pairs).containsExactly(LearnPair("Mitton", "Mitun"))
    }

    @Test
    fun identical_empty() {
        assertThat(LearnEngine.pairsFromEdit("hello", "hello")).isEmpty()
        assertThat(LearnEngine.pairsFromEdit("  hello  ", "hello")).isEmpty()
        assertThat(LearnEngine.pairsFromEdit("", "")).isEmpty()
    }

    @Test
    fun common_english_empty() {
        assertThat(LearnEngine.pairsFromEdit("the", "a")).isEmpty()
        assertThat(LearnEngine.pairsFromEdit("hello the world", "hello a world")).isEmpty()
        assertThat(LearnEngine.pairsFromEdit("to", "from")).isEmpty()
    }

    @Test
    fun huge_rewrite_empty() {
        val from = "one two three four five six seven eight"
        val to = "aaa bbb ccc ddd eee fff ggg hhh"
        assertThat(LearnEngine.pairsFromEdit(from, to)).isEmpty()
    }

    @Test
    fun john_to_mitun() {
        val pairs = LearnEngine.pairsFromEdit("john", "Mitun")
        assertThat(pairs).containsExactly(LearnPair("john", "Mitun"))
    }

    @Test
    fun shouldLearn_rejects_short_same() {
        assertThat(LearnEngine.shouldLearn("a", "bb")).isFalse()
        assertThat(LearnEngine.shouldLearn("ab", "x")).isFalse()
        assertThat(LearnEngine.shouldLearn("Hello", "hello")).isFalse()
        assertThat(LearnEngine.shouldLearn("Mitton", "Mitun")).isTrue()
    }

    @Test
    fun rewriteMap_plus_applyDictionary() {
        val map = LearnEngine.rewriteMap(listOf(LearnPair("Mitton", "Mitun")))
        assertThat(map).containsEntry("Mitton", "Mitun")
        val out = TextPostProcessor.applyDictionary("hi Mitton", map)
        assertThat(out).isEqualTo("hi Mitun")
    }

    @Test
    fun shouldWatch_window() {
        val inserted = 1_000L
        assertThat(LearnEngine.shouldWatch(1_000L, 0L)).isFalse()
        assertThat(LearnEngine.shouldWatch(inserted, inserted)).isFalse()
        assertThat(LearnEngine.shouldWatch(inserted + 1, inserted)).isTrue()
        assertThat(LearnEngine.shouldWatch(inserted + 45_000L, inserted)).isTrue()
        assertThat(LearnEngine.shouldWatch(inserted + 45_001L, inserted)).isFalse()
        assertThat(LearnEngine.shouldWatch(500L, inserted)).isFalse()
    }

    @Test
    fun isOwnSet() {
        assertThat(LearnEngine.isOwnSet("hello", "hello")).isTrue()
        assertThat(LearnEngine.isOwnSet(" hello ", "hello")).isTrue()
        assertThat(LearnEngine.isOwnSet("hello", "world")).isFalse()
    }

    @Test
    fun sentence_index_align_one_fix() {
        val pairs = LearnEngine.pairsFromEdit("Meet Mitton today", "Meet Mitun today")
        assertThat(pairs).containsExactly(LearnPair("Mitton", "Mitun"))
    }
}
