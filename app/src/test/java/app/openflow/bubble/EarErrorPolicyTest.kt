package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EarErrorPolicyTest {

    @Test
    fun mic_beats_fatal() {
        assertThat(EarErrorPolicy.classify("Allow mic please", fatal = true))
            .isEqualTo(EarErrorPolicy.Kind.MIC)
        assertThat(EarErrorPolicy.classify("Microphone busy", fatal = false))
            .isEqualTo(EarErrorPolicy.Kind.MIC)
    }

    @Test
    fun soft_only_when_not_fatal() {
        listOf("Silence", "No match", "Busy", "No recognition", "Retrying").forEach { m ->
            assertThat(EarErrorPolicy.classify(m, fatal = false))
                .isEqualTo(EarErrorPolicy.Kind.SOFT)
            assertThat(EarErrorPolicy.classify(m, fatal = true))
                .isEqualTo(EarErrorPolicy.Kind.SHOW)
        }
    }

    @Test
    fun other_is_show() {
        assertThat(EarErrorPolicy.classify("Server exploded", fatal = false))
            .isEqualTo(EarErrorPolicy.Kind.SHOW)
    }
}
