package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextPostProcessorTest {

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
        val out = TextPostProcessor.process("1. Apples 2. Bananas 3. Oranges")
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
        val out = TextPostProcessor.process("1 apples 2 bananas 3 oranges")
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
}
