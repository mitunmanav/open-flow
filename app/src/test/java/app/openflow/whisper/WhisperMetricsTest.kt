package app.openflow.whisper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WhisperMetricsTest {
    @Test
    fun rtf_one_when_transcribe_matches_audio() {
        val m = WhisperMetrics(loadMs = 0, transcribeMs = 1000, audioMs = 1000)
        assertThat(m.rtf).isEqualTo(1.0)
    }

    @Test
    fun audio_ms_from_samples() {
        assertThat(WhisperMetrics.audioMs(16_000)).isEqualTo(1000L)
    }
}
