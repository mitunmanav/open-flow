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
            .isEqualTo("Listening 3s")
    }

    @Test
    fun final_shows_check_and_text() {
        assertThat(BubbleLabelFormatter.finalChunk("done here"))
            .isEqualTo("✓ done here")
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
        assertThat(BubbleLabelFormatter.idle()).isEqualTo("🎙 Tap to talk")
    }
}
