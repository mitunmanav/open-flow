package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionTextTest {

    @Test
    fun finals_only() {
        assertThat(SessionText.commitRaw("hello world", "")).isEqualTo("hello world")
    }

    @Test
    fun partial_only_when_no_final() {
        assertThat(SessionText.commitRaw("", "hello there")).isEqualTo("hello there")
    }

    @Test
    fun empty_both() {
        assertThat(SessionText.commitRaw("", "")).isEqualTo("")
        assertThat(SessionText.commitRaw("  ", "  ")).isEqualTo("")
    }

    @Test
    fun append_partial_after_finals() {
        assertThat(SessionText.commitRaw("hello", "world")).isEqualTo("hello world")
    }

    @Test
    fun skip_partial_if_already_suffix_of_finals() {
        assertThat(SessionText.commitRaw("hello world", "world")).isEqualTo("hello world")
        assertThat(SessionText.commitRaw("hello world", "hello world")).isEqualTo("hello world")
    }

    @Test
    fun prefer_longer_partial_when_it_extends_finals() {
        // Engine sometimes re-emits growing hypothesis after a final chunk
        assertThat(SessionText.commitRaw("hello", "hello world")).isEqualTo("hello world")
    }

    @Test
    fun trim_and_collapse() {
        assertThat(SessionText.commitRaw("  hi  ", "  there  ")).isEqualTo("hi there")
    }

    @Test
    fun no_double_space_before_punct_partial() {
        assertThat(SessionText.commitRaw("Hi", ".")).isEqualTo("Hi.")
    }
}
