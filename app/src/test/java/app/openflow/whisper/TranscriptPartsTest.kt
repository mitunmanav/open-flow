package app.openflow.whisper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TranscriptPartsTest {
    @Test
    fun join_skips_blank_and_uses_spaces() {
        val p = TranscriptParts()
        p.add(" hello")
        p.add("")
        p.add("world ")
        assertThat(p.join()).isEqualTo("hello world")
    }

    @Test
    fun join_collapses_fourgram_loop() {
        val p = TranscriptParts()
        p.add("thank you for watching")
        p.add("thank you for watching")
        p.add("thank you for watching")
        assertThat(p.join()).isEqualTo("thank you for watching")
        assertThat(p.last.loopCollapsed).isTrue()
    }

    @Test
    fun join_flags_signature_and_keeps_text() {
        val p = TranscriptParts()
        p.add("the meeting is done thanks for watching")
        assertThat(p.join()).contains("thanks for watching")
        assertThat(p.join()).contains("meeting")
        assertThat(p.last.signatures).contains("thanks for watching")
    }
}
