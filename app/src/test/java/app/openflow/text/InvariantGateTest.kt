package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InvariantGateTest {

    @Test
    fun substring_words_pass() {
        assertThat(InvariantGate.ok("meeting tomorrow", "Meeting tomorrow.")).isTrue()
        // "meet" ⊂ "meeting" — parla-clean substring rule.
        assertThat(InvariantGate.ok("meeting", "meet")).isTrue()
    }

    @Test
    fun invented_word_fails() {
        val v = InvariantGate.check("send the report", "send the report ASAP please")
        assertThat(v.ok).isFalse()
        assertThat(v.invented).containsExactly("ASAP", "please")
    }

    @Test
    fun punctuation_only_tokens_pass() {
        assertThat(InvariantGate.ok("hello world", "Hello, world.")).isTrue()
        assertThat(InvariantGate.ok("wait for it", "wait... for it!")).isTrue()
        assertThat(InvariantGate.ok("bullet item next", "• item • next")).isTrue()
    }

    @Test
    fun itn_renders_pass() {
        assertThat(InvariantGate.ok("it costs forty two dollars", "It costs $42.")).isTrue()
        assertThat(InvariantGate.ok("meet at seven thirty pm", "Meet at 7:30 pm.")).isTrue()
        assertThat(InvariantGate.ok("may fifth", "May 5th.")).isTrue()
        assertThat(InvariantGate.ok("fifty percent", "50%")).isTrue()
    }

    @Test
    fun formal_expansion_allowlist_passes() {
        assertThat(InvariantGate.ok("i gonna do it", "I am going to do it.")).isTrue()
        assertThat(InvariantGate.ok("i don't know", "I do not know.")).isTrue()
        assertThat(
            InvariantGate.ok("we can't make it", "We cannot make it.")
        ).isTrue()
    }

    @Test
    fun atomic_sentinels_and_urls_pass() {
        assertThat(
            InvariantGate.ok("see https://example.com/x?a=1 now", "See https://example.com/x?a=1 now.")
        ).isTrue()
    }

    @Test
    fun i2_question_and_bang_must_survive() {
        assertThat(InvariantGate.terminalPunctOk("Ready?", "Ready?")).isTrue()
        assertThat(InvariantGate.terminalPunctOk("Go!", "Go!")).isTrue()
        assertThat(InvariantGate.ok("Ready?", "Ready")).isFalse()
        assertThat(InvariantGate.ok("Go!", "Go")).isFalse()
    }

    @Test
    fun i2_period_may_strip() {
        assertThat(InvariantGate.terminalPunctOk("Sounds good.", "Sounds good")).isTrue()
        assertThat(InvariantGate.ok("Sounds good.", "Sounds good")).isTrue()
    }

    @Test
    fun i2_filler_tag_may_drop_bang() {
        assertThat(InvariantGate.terminalPunctOk("hello um!", "Hello")).isTrue()
    }

    @Test
    fun empty_and_blank_pass() {
        assertThat(InvariantGate.check("", "").ok).isTrue()
        assertThat(InvariantGate.check("um uh", "...").ok).isTrue()
    }
}
