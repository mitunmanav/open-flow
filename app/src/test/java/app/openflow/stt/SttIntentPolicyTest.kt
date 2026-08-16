package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SttIntentPolicyTest {

    @Test
    fun formatting_on_from_api_33() {
        assertThat(SttIntentPolicy.preferFormatted(32)).isFalse()
        assertThat(SttIntentPolicy.preferFormatted(33)).isTrue()
        assertThat(SttIntentPolicy.preferFormatted(36)).isTrue()
    }

    @Test
    fun quality_vs_latency_mode() {
        assertThat(SttIntentPolicy.formattingMode(preferQuality = true))
            .isEqualTo(SttIntentPolicy.QUALITY)
        assertThat(SttIntentPolicy.formattingMode(preferQuality = false))
            .isEqualTo(SttIntentPolicy.LATENCY)
    }
}
