package app.openflow.orchestrate

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionArtifactTest {

    @Test
    fun bestAvailable_prefersAi() {
        val a = SessionArtifact(raw = "raw", cleaned = "cleaned", ai = "ai out")
        assertThat(a.bestAvailable()).isEqualTo("ai out")
    }

    @Test
    fun bestAvailable_usesCleanedWhenAiBlank() {
        val a = SessionArtifact(raw = "raw", cleaned = "cleaned", ai = "")
        assertThat(a.bestAvailable()).isEqualTo("cleaned")
    }

    @Test
    fun bestAvailable_usesRawWhenCleanedAndAiBlank() {
        val a = SessionArtifact(raw = "raw only", cleaned = "", ai = "")
        assertThat(a.bestAvailable()).isEqualTo("raw only")
    }

    @Test
    fun bestAvailable_emptyAll_returnsEmpty() {
        val a = SessionArtifact()
        assertThat(a.bestAvailable()).isEmpty()
    }

    @Test
    fun bestAvailable_brainFail_keepsCleaned() {
        val a = SessionArtifact(raw = "uh hello", cleaned = "Hello", ai = "")
        assertThat(a.bestAvailable()).isEqualTo("Hello")
    }

    @Test
    fun bestAvailable_trimsWhitespace() {
        val a = SessionArtifact(raw = "  raw  ", cleaned = "  cleaned  ", ai = "  ai  ")
        assertThat(a.bestAvailable()).isEqualTo("ai")
    }

    @Test
    fun bestAvailable_whitespaceOnlyAi_fallsThroughToCleaned() {
        val a = SessionArtifact(raw = "raw", cleaned = "cleaned", ai = "   ")
        assertThat(a.bestAvailable()).isEqualTo("cleaned")
    }
}
