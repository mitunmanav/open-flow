package app.openflow.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderIdTest {
    @Test
    fun parse_ear_system() {
        assertThat(ProviderId.parseEar("system")).isEqualTo(EarId.SYSTEM)
    }

    @Test
    fun parse_ear_unknown_defaults_system() {
        assertThat(ProviderId.parseEar("nope")).isEqualTo(EarId.SYSTEM)
        assertThat(ProviderId.parseEar("")).isEqualTo(EarId.SYSTEM)
        assertThat(ProviderId.parseEar("GROQ")).isEqualTo(EarId.SYSTEM)
    }

    @Test
    fun parse_ear_named_ids() {
        assertThat(ProviderId.parseEar("on_phone")).isEqualTo(EarId.ON_PHONE)
        assertThat(ProviderId.parseEar("laptop")).isEqualTo(EarId.LAPTOP)
        assertThat(ProviderId.parseEar("openai")).isEqualTo(EarId.OPENAI)
        assertThat(ProviderId.parseEar("deepgram")).isEqualTo(EarId.DEEPGRAM)
        assertThat(ProviderId.parseEar("assemblyai")).isEqualTo(EarId.ASSEMBLYAI)
        assertThat(ProviderId.parseEar("sarvam")).isEqualTo(EarId.SARVAM)
        assertThat(ProviderId.parseEar("custom_stt")).isEqualTo(EarId.CUSTOM_STT)
    }

    @Test
    fun parse_brain_none() {
        assertThat(ProviderId.parseBrain("none")).isEqualTo(BrainId.NONE)
    }

    @Test
    fun parse_brain_unknown_defaults_none() {
        assertThat(ProviderId.parseBrain("nope")).isEqualTo(BrainId.NONE)
        assertThat(ProviderId.parseBrain("")).isEqualTo(BrainId.NONE)
        assertThat(ProviderId.parseBrain("groq")).isEqualTo(BrainId.NONE)
    }

    @Test
    fun parse_brain_named_ids() {
        assertThat(ProviderId.parseBrain("on_phone")).isEqualTo(BrainId.ON_PHONE)
        assertThat(ProviderId.parseBrain("laptop")).isEqualTo(BrainId.LAPTOP)
        assertThat(ProviderId.parseBrain("openai")).isEqualTo(BrainId.OPENAI)
        assertThat(ProviderId.parseBrain("grok")).isEqualTo(BrainId.GROK)
        assertThat(ProviderId.parseBrain("minimax")).isEqualTo(BrainId.MINIMAX)
        assertThat(ProviderId.parseBrain("deepseek")).isEqualTo(BrainId.DEEPSEEK)
        assertThat(ProviderId.parseBrain("gemini")).isEqualTo(BrainId.GEMINI)
        assertThat(ProviderId.parseBrain("mistral")).isEqualTo(BrainId.MISTRAL)
        assertThat(ProviderId.parseBrain("together")).isEqualTo(BrainId.TOGETHER)
        assertThat(ProviderId.parseBrain("fireworks")).isEqualTo(BrainId.FIREWORKS)
        assertThat(ProviderId.parseBrain("openrouter")).isEqualTo(BrainId.OPENROUTER)
        assertThat(ProviderId.parseBrain("sarvam")).isEqualTo(BrainId.SARVAM)
        assertThat(ProviderId.parseBrain("anthropic")).isEqualTo(BrainId.ANTHROPIC)
        assertThat(ProviderId.parseBrain("custom")).isEqualTo(BrainId.CUSTOM)
    }

    @Test
    fun no_groq_named_preset() {
        val brains = BrainId.entries.map { it.name }
        val ears = EarId.entries.map { it.name }
        assertThat(brains).doesNotContain("GROQ")
        assertThat(ears).doesNotContain("GROQ")
        assertThat(brains).contains("GROK")
    }
}
