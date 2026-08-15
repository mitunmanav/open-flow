package app.openflow.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EarGateTest {

    @Test
    fun system_and_cloud_ears_are_live() {
        assertThat(EarGate.live("system")).isTrue()
        assertThat(EarGate.live("SYSTEM")).isTrue()
        for (id in listOf("openai", "deepgram", "assemblyai", "sarvam")) {
            assertThat(EarGate.live(id)).isTrue()
        }
    }

    @Test
    fun stub_ears_not_live() {
        for (id in listOf("on_phone", "laptop", "custom_stt")) {
            assertThat(EarGate.live(id)).isFalse()
        }
    }

    @Test
    fun resolve_keeps_cloud_ear() {
        assertThat(EarGate.resolve("openai")).isEqualTo("openai")
        assertThat(EarGate.resolve("deepgram")).isEqualTo("deepgram")
        assertThat(EarGate.resolve("system")).isEqualTo("system")
        assertThat(EarGate.resolve("on_phone")).isEqualTo("system")
        assertThat(EarGate.resolve("")).isEqualTo("system")
    }

    @Test
    fun on_phone_brain_resolves_to_none() {
        assertThat(EarGate.resolveBrain("on_phone")).isEqualTo("none")
        assertThat(EarGate.resolveBrain("openai")).isEqualTo("openai")
        assertThat(EarGate.resolveBrain("none")).isEqualTo("none")
    }
}
