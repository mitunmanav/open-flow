package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File

class LearnEngineTest {

    @Before
    fun reset() {
        LearnEngine.resetLearn()
    }

    @Test
    fun mitton_to_mitun_one_word() {
        val pairs = LearnEngine.pairsFromEdit("Mitton", "Mitun")
        assertThat(pairs).containsExactly(LearnPair("Mitton", "Mitun"))
    }

    @Test
    fun clearAll_wipes_sides_keeps_hook() {
        var encoded = "unset"
        LearnEngine.persistHook = { encoded = it }
        LearnEngine.putAuto("wisper", setOf("a", "b"))
        assertThat(LearnEngine.autoKeys()).contains("wisper")
        LearnEngine.clearAll()
        assertThat(LearnEngine.autoKeys()).isEmpty()
        assertThat(LearnEngine.sideBags()).isEmpty()
        assertThat(encoded).isEqualTo("")
        assertThat(LearnEngine.persistHook).isNotNull()
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

    @Test
    fun mike_to_mic_learns() {
        val pairs = LearnEngine.pairsFromEdit("Mike", "Mic")
        assertThat(pairs).containsExactly(LearnPair("Mike", "Mic"))
        assertThat(LearnEngine.isAmbiguous("Mike", "Mic")).isTrue()
    }

    @Test
    fun bare_mike_becomes_mic() {
        val out = TextPostProcessor.applyDictionary(
            "Mike",
            mapOf("Mike" to "Mic"),
            sides = mapOf("mike" to emptySet()),
            autoKeys = setOf("mike")
        )
        assertThat(out).isEqualTo("Mic")
    }

    @Test
    fun the_mike_is_bare_side() {
        val out = TextPostProcessor.applyDictionary(
            "the Mike",
            mapOf("Mike" to "Mic"),
            sides = mapOf("mike" to emptySet()),
            autoKeys = setOf("mike")
        )
        assertThat(out).isEqualTo("the Mic")
    }

    @Test
    fun sentence_with_extra_content_keeps_mike() {
        val out = TextPostProcessor.applyDictionary(
            "ask Mike tomorrow",
            mapOf("Mike" to "Mic"),
            sides = mapOf("mike" to emptySet()),
            autoKeys = setOf("mike")
        )
        assertThat(out).contains("Mike")
        assertThat(out).doesNotContain("Mic")
    }

    @Test
    fun same_stored_bag_rewrites() {
        val bag = LearnEngine.sideBag("turn on Mike", "Mike")
        assertThat(bag).containsExactly("turn")
        val out = TextPostProcessor.applyDictionary(
            "turn on Mike",
            mapOf("Mike" to "Mic"),
            sides = mapOf("mike" to bag),
            autoKeys = setOf("mike")
        )
        assertThat(out).isEqualTo("turn on Mic")
    }

    @Test
    fun titlecase_next_keeps_mike() {
        val out = TextPostProcessor.applyDictionary(
            "Mike Smith",
            mapOf("Mike" to "Mic"),
            sides = mapOf("mike" to emptySet()),
            autoKeys = setOf("mike")
        )
        assertThat(out).isEqualTo("Mike Smith")
    }

    @Test
    fun titlecase_next_skips_manual_too() {
        val out = TextPostProcessor.applyDictionary(
            "Mike Smith",
            mapOf("Mike" to "Mic")
        )
        assertThat(out).isEqualTo("Mike Smith")
    }

    @Test
    fun mitton_to_mitun_always_rewrites_in_sentence() {
        assertThat(LearnEngine.isAmbiguous("Mitton", "Mitun")).isFalse()
        val out = TextPostProcessor.applyDictionary(
            "hello Mitton today",
            mapOf("Mitton" to "Mitun"),
            autoKeys = setOf("mitton")
        )
        assertThat(out).isEqualTo("hello Mitun today")
    }

    @Test
    fun one_to_many_no_learn() {
        assertThat(LearnEngine.pairsFromEdit("hello", "hello world")).isEmpty()
        assertThat(LearnEngine.pairsFromEdit("foo", "bar baz")).isEmpty()
    }

    @Test
    fun reverse_mic_to_mike_forgets() {
        val existing = mapOf("Mike" to "Mic")
        assertThat(LearnEngine.reverseKey("Mic", "Mike", existing)).isEqualTo("Mike")
    }

    @Test
    fun no_cycle() {
        val existing = mapOf("Mike" to "Mic")
        assertThat(LearnEngine.wouldCycle("Mic", "Mike", existing)).isTrue()
        assertThat(LearnEngine.wouldCycle("Mitton", "Mitun", existing)).isFalse()
    }

    @Test
    fun short_close_class_not_one_word() {
        assertThat(LearnEngine.isAmbiguous("Jon", "John")).isTrue()
        assertThat(LearnEngine.isAmbiguous("Chris", "Kris")).isTrue()
        val out = TextPostProcessor.applyDictionary(
            "ask Jon tomorrow",
            mapOf("Jon" to "John"),
            sides = mapOf("jon" to emptySet()),
            autoKeys = setOf("jon")
        )
        assertThat(out).contains("Jon")
        assertThat(out).doesNotContain("John")
    }

    @Test
    fun no_friend_or_named_cue_in_source() {
        val src = learnEngineSrc().lowercase()
        assertThat(src).doesNotContain("friend")
        assertThat(src).doesNotContain("named")
    }

    private fun learnEngineSrc(): String {
        val candidates = listOf(
            File("src/main/java/app/openflow/text/LearnEngine.kt"),
            File("app/src/main/java/app/openflow/text/LearnEngine.kt")
        )
        return candidates.first { it.exists() }.readText()
    }

    @Test
    fun applyPairs_transfers_case_from_source() {
        val map = mapOf("Mitton" to "Mitun")
        // lowercase source → lowercase replacement
        assertThat(LearnEngine.applyPairs("hello mitton", map)).isEqualTo("hello mitun")
        // UPPERCASE source → UPPERCASE replacement
        assertThat(LearnEngine.applyPairs("hello MITTON", map)).isEqualTo("hello MITUN")
        // Title case (matches map key exactly) → keeps map value
        assertThat(LearnEngine.applyPairs("hello Mitton", map)).isEqualTo("hello Mitun")
    }

    @Test
    fun applyPairs_handles_plurals() {
        val map = mapOf("Mitton" to "Mitun")
        // plural s
        assertThat(LearnEngine.applyPairs("hello Mittons", map)).isEqualTo("hello Mituns")
        // possessive 's
        assertThat(LearnEngine.applyPairs("Mitton's house", map)).isEqualTo("Mitun's house")
    }

    @Test
    fun encode_decode_roundtrip_idempotent() {
        LearnEngine.putAuto("wisper", setOf("voice", "app"))
        LearnEngine.putAuto("mic", setOf("turn"))
        LearnEngine.putManual("botton")
        val encoded1 = LearnEngine.encodeSides()
        LearnEngine.loadSides(encoded1)
        val encoded2 = LearnEngine.encodeSides()
        assertThat(encoded2).isEqualTo(encoded1)
    }

    @Test
    fun middle_word_change_learns_pair() {
        val pairs = LearnEngine.pairsFromEdit("send the report", "send the document")
        assertThat(pairs).containsExactly(LearnPair("report", "document"))
    }

    @Test
    fun levenshtein_known_values() {
        assertThat(LearnEngine.levenshtein("kitten", "sitting")).isEqualTo(3)
        assertThat(LearnEngine.levenshtein("", "abc")).isEqualTo(3)
        assertThat(LearnEngine.levenshtein("abc", "abc")).isEqualTo(0)
    }

    @Test
    fun one_edit_does_not_persist_auto() {
        LearnEngine.resetLearn()
        val c1 = LearnEngine.noteAutoCandidate("mike", "mic", setOf("turn"))
        assertThat(c1.persisted).isFalse()
        assertThat(LearnEngine.autoKeys()).doesNotContain("mike")
    }

    @Test
    fun two_same_bag_persists() {
        LearnEngine.resetLearn()
        LearnEngine.noteAutoCandidate("mike", "mic", setOf("turn"))
        val c2 = LearnEngine.noteAutoCandidate("mike", "mic", setOf("turn"))
        assertThat(c2.persisted).isTrue()
        assertThat(LearnEngine.autoKeys()).contains("mike")
    }

    @Test
    fun apply_skips_title_mike_and_mike_ii() {
        val text = LearnEngine.applyPairs(
            "Meet Mike II tomorrow",
            mapOf("mike" to "mic"),
            sides = mapOf("mike" to setOf("turn")),
            autoKeys = setOf("mike"),
        )
        assertThat(text).contains("Mike")
        assertThat(text).contains("II")
        assertThat(text.lowercase()).doesNotContain("mic")
    }

    @Test
    fun reverse_surface_drops() {
        LearnEngine.resetLearn()
        LearnEngine.noteAutoCandidate("mike", "mic", setOf("turn"))
        LearnEngine.noteAutoCandidate("mike", "mic", setOf("turn"))
        LearnEngine.noteAutoCandidate("mic", "mike", setOf("turn"))
        assertThat(LearnEngine.autoKeys()).doesNotContain("mike")
    }
}
