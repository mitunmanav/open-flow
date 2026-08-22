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
}
