package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleLabelFormatterTest {

    @Test
    fun partial_shows_text() {
        assertThat(BubbleLabelFormatter.partial("hello world"))
            .isEqualTo("hello world")
    }

    @Test
    fun partial_blank_falls_back_to_listening() {
        assertThat(BubbleLabelFormatter.partial("  ", elapsedSec = 3))
            .isEqualTo("Hearing 3s")
    }

    @Test
    fun final_shows_text_no_checkmark() {
        assertThat(BubbleLabelFormatter.finalChunk("done here"))
            .isEqualTo("done here")
        assertThat(BubbleLabelFormatter.finalChunk("done here")).doesNotContain("✓")
    }

    @Test
    fun listening_own_words() {
        assertThat(BubbleLabelFormatter.listening(0)).isEqualTo("Hearing…")
        assertThat(BubbleLabelFormatter.listening(3)).isEqualTo("Hearing 3s")
    }

    @Test
    fun need_mic_own_words() {
        assertThat(BubbleLabelFormatter.needMic()).isEqualTo("Mic off")
    }

    @Test
    fun soft_cap_long_partial_keeps_tail() {
        val long = "a".repeat(100)
        val out = BubbleLabelFormatter.partial(long, maxChars = 40)
        assertThat(out.length).isAtMost(41) // ellipsis
        assertThat(out.endsWith("…") || out.length <= 40).isTrue()
    }

    @Test
    fun idle_default() {
        assertThat(BubbleLabelFormatter.idle()).isEqualTo("Tap")
    }
}
