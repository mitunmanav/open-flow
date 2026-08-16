package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class TextPostProcessorTest {

    @Before
    fun resetLearn() {
        LearnEngine.resetLearn()
    }

    @Test
    fun strips_fillers() {
        val out = TextPostProcessor.process("um hello uh world")
        assertThat(out.lowercase()).doesNotContain("um")
        assertThat(out.lowercase()).doesNotContain("uh")
        assertThat(out.lowercase()).contains("hello")
        assertThat(out.lowercase()).contains("world")
    }

    @Test
    fun capitalizes_start() {
        val out = TextPostProcessor.process("hello there friend this is longer text")
        assertThat(out.first().isUpperCase()).isTrue()
    }

    @Test
    fun dictionary_replacement() {
        val out = TextPostProcessor.applyDictionary(
            "ship openflow today",
            mapOf("openflow" to "Open Flow")
        )
        assertThat(out).contains("Open Flow")
    }

    @Test
    fun snippet_exact_expand() {
        val out = TextPostProcessor.expandSnippets(
            "sig",
            mapOf("sig" to "Best regards,\nMitun")
        )
        assertThat(out).contains("Best regards")
    }

    @Test
    fun snippet_in_sentence_whole_word_expand() {
        val out = TextPostProcessor.expandSnippets(
            "please send sig today",
            mapOf("sig" to "Best regards,\nMitun")
        )
        assertThat(out).contains("please send")
        assertThat(out).contains("Best regards")
        assertThat(out).contains("Mitun")
        assertThat(out).contains("today")
        assertThat(out.lowercase()).doesNotContain("sig")
    }

    @Test
    fun snippet_does_not_expand_partial_word() {
        val out = TextPostProcessor.expandSnippets(
            "the signal is ready",
            mapOf("sig" to "BLOCK")
        )
        assertThat(out).isEqualTo("the signal is ready")
    }

    @Test
    fun snippet_phrase_in_sentence() {
        val out = TextPostProcessor.expandSnippets(
            "please use my address thanks",
            mapOf("my address" to "1 Main St")
        )
        assertThat(out).contains("please use")
        assertThat(out).contains("1 Main St")
        assertThat(out).contains("thanks")
        assertThat(out.lowercase()).doesNotContain("my address")
    }

    @Test
    fun snippet_in_sentence_case_insensitive() {
        val out = TextPostProcessor.expandSnippets(
            "Send SIG please",
            mapOf("sig" to "Best regards")
        )
        assertThat(out).contains("Best regards")
        assertThat(out).doesNotContain("SIG")
    }

    @Test
    fun snippet_longest_phrase_wins() {
        val out = TextPostProcessor.expandSnippets(
            "email sig now",
            mapOf("sig" to "SHORT", "email sig" to "LONG BODY")
        )
        assertThat(out).contains("LONG BODY")
        assertThat(out).doesNotContain("SHORT")
    }

    @Test
    fun polishSessionResult_snippet_in_sentence_before_cleanup() {
        val result = TextPostProcessor.polishSessionResult(
            raw = "um send sig please",
            level = CleanupLevel.LIGHT,
            style = WritingStyle.CASUAL,
            snippets = mapOf("sig" to "Best regards,\nMitun")
        )
        assertThat(result.raw).isEqualTo("um send sig please")
        assertThat(result.clean).contains("Best regards")
        assertThat(result.clean).contains("Mitun")
    }

    @Test
    fun question_mark_for_how() {
        val out = TextPostProcessor.process("how are you doing today")
        assertThat(out).endsWith("?")
    }

    @Test
    fun polish_session_course_corrects_time() {
        val out = TextPostProcessor.polishSession(
            "set a reminder for 4:30 actually 5:30"
        )
        assertThat(out.lowercase()).contains("5:30")
        assertThat(out.lowercase()).doesNotContain("4:30")
    }

    @Test
    fun voice_command_new_line() {
        val out = TextPostProcessor.process("hello new line world")
        assertThat(out).contains("\n")
        assertThat(out.lowercase()).contains("hello")
        assertThat(out.lowercase()).contains("world")
    }

    @Test
    fun numbered_list_inline_dots_to_multiline() {
        // Lists are Medium+ (clarity), not Light
        val out = CleanupPipeline.run(
            "1. Apples 2. Bananas 3. Oranges",
            CleanupLevel.NORMAL
        ).clean
        assertThat(out).contains("1. Apples")
        assertThat(out).contains("2. Bananas")
        assertThat(out).contains("3. Oranges")
        assertThat(out).contains("\n")
        val lines = out.lines().map { it.trim() }.filter { it.isNotEmpty() }
        assertThat(lines[0]).startsWith("1.")
        assertThat(lines[1]).startsWith("2.")
        assertThat(lines[2]).startsWith("3.")
    }

    @Test
    fun numbered_list_spoken_digits() {
        val out = CleanupPipeline.run(
            "1 apples 2 bananas 3 oranges",
            CleanupLevel.NORMAL
        ).clean
        val lines = out.lines().map { it.trim() }.filter { it.isNotEmpty() }
        assertThat(lines).hasSize(3)
        assertThat(lines[0].lowercase()).contains("apples")
        assertThat(lines[1].lowercase()).contains("bananas")
        assertThat(lines[2].lowercase()).contains("oranges")
    }

    @Test
    fun voice_paren_colon_quote() {
        val out = TextPostProcessor.process(
            "hello open paren world close paren colon open quote hi close quote"
        )
        assertThat(out).contains("(")
        assertThat(out).contains(")")
        assertThat(out).contains(":")
        assertThat(out).contains("\"")
        assertThat(out.lowercase()).doesNotContain("open paren")
        assertThat(out.lowercase()).doesNotContain("close paren")
        assertThat(out.lowercase()).doesNotContain("colon")
        assertThat(out.lowercase()).doesNotContain("open quote")
        assertThat(out.lowercase()).doesNotContain("close quote")
    }

    @Test
    fun voice_quote_alone() {
        val out = TextPostProcessor.process("say quote hello")
        assertThat(out).contains("\"")
        assertThat(out.lowercase()).doesNotContain("quote")
    }

    @Test
    fun polishSessionResult_dict_then_snippet_then_cleanup_preserves_raw() {
        val result = TextPostProcessor.polishSessionResult(
            raw = "um openflow",
            level = CleanupLevel.LIGHT,
            style = WritingStyle.CASUAL,
            dictionary = mapOf("openflow" to "Open Flow"),
            snippets = emptyMap()
        )
        assertThat(result.raw).isEqualTo("um openflow")
        assertThat(result.clean.lowercase()).doesNotContain("um")
        assertThat(result.clean).contains("Open Flow")
    }

    @Test
    fun polishSessionResult_snippet_exact_expand_before_cleanup() {
        val result = TextPostProcessor.polishSessionResult(
            raw = "sig",
            level = CleanupLevel.LIGHT,
            style = WritingStyle.CASUAL,
            snippets = mapOf("sig" to "Best regards,\nMitun")
        )
        assertThat(result.raw).isEqualTo("sig")
        assertThat(result.clean).contains("Best regards")
        assertThat(result.clean).contains("Mitun")
    }

    @Test
    fun polishSessionResult_passes_custom_style() {
        val custom = CustomStyleConfig(
            endPunct = EndPunct.BANG,
            caps = CapsMode.SENTENCE,
            expandInformal = false
        )
        val result = TextPostProcessor.polishSessionResult(
            raw = "hello there friend this is a longer custom line",
            level = CleanupLevel.LIGHT,
            style = WritingStyle.CUSTOM,
            custom = custom
        )
        assertThat(result.clean).endsWith("!")
    }

    @Test
    fun polish_empty_in_empty_out() {
        val r = TextPostProcessor.polishSessionResult("")
        assertThat(r.clean).isEmpty()
        assertThat(r.raw).isEmpty()
    }

    @Test
    fun polish_non_empty_does_not_vanish() {
        val r = TextPostProcessor.polishSessionResult(
            raw = "please send the report to finance today",
            level = CleanupLevel.HIGH,
            style = WritingStyle.CASUAL
        )
        assertThat(r.clean).isNotEmpty()
        assertThat(r.clean.lowercase()).contains("send")
        assertThat(r.clean.lowercase()).contains("report")
        assertThat(r.clean.lowercase()).contains("finance")
        assertThat(r.raw).contains("please send")
    }

    @Test
    fun polish_wires_dict_snippet_voice_correct_style() {
        val r = TextPostProcessor.polishSessionResult(
            raw = "um openflow meet at 4:30 actually 5:30 period",
            level = CleanupLevel.NORMAL,
            style = WritingStyle.FORMAL,
            dictionary = mapOf("openflow" to "Open Flow")
        )
        val clean = r.clean
        assertThat(r.raw).contains("openflow")
        assertThat(clean).contains("Open Flow")
        assertThat(clean.lowercase()).contains("5:30")
        assertThat(clean.lowercase()).doesNotContain("4:30")
        assertThat(clean).contains(".")
        assertThat(clean.lowercase()).doesNotContain("um")
        assertThat(clean.lowercase()).doesNotContain("actually")
        assertThat(clean.lowercase()).doesNotContain("period")
        assertThat(clean.first().isUpperCase()).isTrue()
    }

    @Test
    fun dictionary_bare_auto_ambiguous_rewrites() {
        LearnEngine.putAuto("Mike", emptySet())
        val out = TextPostProcessor.applyDictionary(
            "Mike",
            mapOf("Mike" to "Mic"),
            sides = LearnEngine.sideBags(),
            autoKeys = LearnEngine.autoKeys()
        )
        assertThat(out).isEqualTo("Mic")
    }

    @Test
    fun dictionary_extra_content_keeps_auto_ambiguous() {
        LearnEngine.putAuto("Mike", emptySet())
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
    fun dictionary_titlecase_next_keeps_hit() {
        val out = TextPostProcessor.applyDictionary(
            "Mike Smith",
            mapOf("Mike" to "Mic")
        )
        assertThat(out).isEqualTo("Mike Smith")
    }

    @Test
    fun dictionary_manual_rewrites_in_sentence() {
        val out = TextPostProcessor.applyDictionary(
            "ask Mike tomorrow",
            mapOf("Mike" to "Mic")
        )
        assertThat(out).contains("Mic")
        assertThat(out).doesNotContain("Mike")
    }

    @Test
    fun dictionary_longest_key_first() {
        val out = TextPostProcessor.applyDictionary(
            "openflow app",
            mapOf("open" to "X", "openflow" to "Open Flow")
        )
        assertThat(out).contains("Open Flow")
        assertThat(out).doesNotContain("Xflow")
    }

    @Test
    fun polish_uses_live_auto_hints() {
        LearnEngine.putAuto("Mike", emptySet())
        val r = TextPostProcessor.polishSessionResult(
            raw = "ask Mike tomorrow",
            level = CleanupLevel.RAW,
            dictionary = mapOf("Mike" to "Mic")
        )
        assertThat(r.clean).contains("Mike")
        assertThat(r.clean).doesNotContain("Mic")
    }

    @Test
    fun contractions_preserved_in_polish() {
        val out = TextPostProcessor.process("don't worry it's fine we'll see you there")
        assertThat(out).contains("Don't")
        assertThat(out).contains("it's")
        assertThat(out).contains("we'll")
    }

    @Test
    fun numbers_and_decimals_preserved() {
        val out = TextPostProcessor.process("the price is 45.99 and version 2.0 is out")
        assertThat(out).contains("45.99")
        assertThat(out).contains("2.0")
    }

    @Test
    fun empty_and_blank_strings_handled_safely() {
        assertThat(TextPostProcessor.process("")).isEmpty()
        assertThat(TextPostProcessor.process("   ")).isEmpty()
        assertThat(TextPostProcessor.polishSession("").trim()).isEmpty()
        assertThat(TextPostProcessor.polishSession("   ").trim()).isEmpty()
    }
}
