package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearnPromptTest {

    @Test
    fun blank_when_no_hits() {
        assertThat(LearnPrompt.utteranceHints("hello world", mapOf("mike" to "Mic"))).isEmpty()
        assertThat(LearnPrompt.utteranceHints("meet mike", emptyMap())).isEmpty()
    }

    @Test
    fun only_pairs_in_this_utterance() {
        val dict = mapOf("mike" to "Mic", "acme" to "Acme Corp", "zzz" to "Z")
        val out = LearnPrompt.utteranceHints("meet mike at lunch", dict)
        assertThat(out).contains("mike→Mic")
        assertThat(out).doesNotContain("acme")
        assertThat(out).doesNotContain("zzz")
    }

    @Test
    fun ignore_case_hit() {
        val out = LearnPrompt.utteranceHints("Meet MIKE later", mapOf("mike" to "Mic"))
        assertThat(out).isEqualTo("mike→Mic")
    }
}
