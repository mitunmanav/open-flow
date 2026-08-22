package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EarMicPolicyTest {
    @Test
    fun on_phone_ear_owns_mic() {
        assertThat(EarMicPolicy.bubbleCapturesWav("on_phone")).isFalse()
        assertThat(EarMicPolicy.bubbleCapturesWav("system")).isTrue()
        assertThat(EarMicPolicy.bubbleCapturesWav("openai")).isTrue()
    }
}
