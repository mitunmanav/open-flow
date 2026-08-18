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
    fun final_shows_text_no_checkmark() {
        assertThat(BubbleLabelFormatter.finalChunk("done here"))
            .isEqualTo("done here")
        assertThat(BubbleLabelFormatter.finalChunk("done here")).doesNotContain("✓")
    }

    @Test
    fun listening_own_words() {
        assertThat(BubbleLabelFormatter.listening(0)).isEqualTo("Listening")
        assertThat(BubbleLabelFormatter.listening(3)).isEqualTo("Listening 3s")
    }

    @Test
    fun need_mic_own_words() {
        assertThat(BubbleLabelFormatter.needMic()).isEqualTo("Mic off")
    }

    @Test
    fun ear_error_maps_403_to_key_hint() {
        assertThat(BubbleLabelFormatter.earError("cloud socket failed (403)"))
            .isEqualTo("API key rejected — check Speech + AI")
        assertThat(BubbleLabelFormatter.earError("cloud socket failed (401)"))
            .isEqualTo("API key missing — open Speech + AI")
        assertThat(BubbleLabelFormatter.earError("Silence timeout"))
            .isEqualTo("Silence timeout")
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

    @Test
    fun transcribe_fail_saved_in_app() {
        assertThat(BubbleLabelFormatter.transcribeFail())
            .isEqualTo("Could not transcribe. Saved in the app.")
    }
}
