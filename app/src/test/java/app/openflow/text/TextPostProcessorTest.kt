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
}
