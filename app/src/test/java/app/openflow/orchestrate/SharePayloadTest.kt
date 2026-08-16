package app.openflow.orchestrate

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SharePayloadTest {

    @Test
    fun forRow_prefersStoredText() {
        assertThat(SharePayload.forRow(text = "stored", rawText = "raw"))
            .isEqualTo("stored")
    }

    @Test
    fun forRow_fallsBackToRawWhenTextEmpty() {
        assertThat(SharePayload.forRow(text = "", rawText = "raw fallback"))
            .isEqualTo("raw fallback")
    }

    @Test
    fun forRow_trimsBoth() {
        assertThat(SharePayload.forRow(text = "  best  ", rawText = "  raw  "))
            .isEqualTo("best")
    }

    @Test
    fun forRow_whitespaceOnlyText_fallsBackToRaw() {
        assertThat(SharePayload.forRow(text = "   ", rawText = "raw"))
            .isEqualTo("raw")
    }

    @Test
    fun forArtifact_delegatesToBestAvailable() {
        val a = SessionArtifact(raw = "raw", cleaned = "cleaned", ai = "ai")
        assertThat(SharePayload.forArtifact(a)).isEqualTo("ai")
    }
}
