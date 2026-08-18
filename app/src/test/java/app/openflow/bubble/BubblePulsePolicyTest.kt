package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubblePulsePolicyTest {

    @Test
    fun off_keeps_base_scale() {
        val base = 0.85f
        assertThat(BubblePulsePolicy.scale(listening = true, pulseOn = false, base = base, rms = 10f))
            .isEqualTo(base)
    }

    @Test
    fun on_while_listening_uses_rms() {
        val base = 1f
        assertThat(BubblePulsePolicy.scale(listening = true, pulseOn = true, base = base, rms = 10f))
            .isEqualTo(BubbleRms.pulseScale(10f))
    }

    @Test
    fun idle_never_pulses() {
        assertThat(BubblePulsePolicy.scale(listening = false, pulseOn = true, base = 0.9f, rms = 10f))
            .isEqualTo(0.9f)
    }
}
