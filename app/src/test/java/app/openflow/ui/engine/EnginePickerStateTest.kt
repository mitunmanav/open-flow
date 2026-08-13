package app.openflow.ui.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnginePickerStateTest {

    @Test
    fun brain_none_high_and_command_off() {
        val s = EnginePickerState.of(earId = "system", brainId = "none")
        assertThat(s.rewrite).isFalse()
        assertThat(s.commandMode).isFalse()
        assertThat(s.needsKey).isFalse()
        assertThat(s.highLabel).isEqualTo("High (rules)")
        assertThat(s.commandWhy).isEqualTo("Command Mode — needs a brain")
    }

    @Test
    fun brain_openai_high_command_need_key() {
        val s = EnginePickerState.of(earId = "system", brainId = "openai")
        assertThat(s.rewrite).isTrue()
        assertThat(s.commandMode).isTrue()
        assertThat(s.needsKey).isTrue()
        assertThat(s.highLabel).isEqualTo("High (AI)")
        assertThat(s.commandWhy).isNull()
    }

    @Test
    fun ear_on_phone_audio_stays() {
        val s = EnginePickerState.of(earId = "on_phone", brainId = "none")
        assertThat(s.honesty).isEqualTo("Audio stays on this phone.")
        assertThat(s.needsKey).isFalse()
    }

    @Test
    fun ear_laptop_audio_to_computer() {
        val s = EnginePickerState.of(earId = "laptop", brainId = "none")
        assertThat(s.honesty).isEqualTo("Audio goes to your computer.")
        assertThat(s.needsUrl).isTrue()
    }

    @Test
    fun system_none_may_use_google() {
        val s = EnginePickerState.of(earId = "system", brainId = "none")
        assertThat(s.honesty).isEqualTo("On this phone. Phone STT may still use Google.")
    }

    @Test
    fun grok_is_xai_not_groq() {
        val s = EnginePickerState.of(earId = "system", brainId = "grok")
        assertThat(s.honesty).isEqualTo("Text of this utterance goes to xAI (Grok). Not Groq.")
        assertThat(s.needsKey).isTrue()
        assertThat(s.rewrite).isTrue()
        assertThat(s.honesty).contains("Not Groq")
        assertThat(s.honesty).contains("xAI")
    }

    @Test
    fun openai_ear_voice_to_openai() {
        val s = EnginePickerState.of(earId = "openai", brainId = "none")
        assertThat(s.honesty).isEqualTo("Your voice goes to OpenAI.")
        assertThat(s.needsKey).isTrue()
    }

    @Test
    fun brain_laptop_to_their_computer() {
        val s = EnginePickerState.of(earId = "system", brainId = "laptop")
        assertThat(s.honesty).isEqualTo("Text goes to the computer you set.")
        assertThat(s.needsUrl).isTrue()
        assertThat(s.rewrite).isTrue()
        assertThat(s.commandMode).isTrue()
    }

    @Test
    fun sarvam_ear_shows_mode_chips() {
        val s = EnginePickerState.of(earId = "sarvam", brainId = "none")
        assertThat(s.showSarvamMode).isTrue()
        assertThat(s.needsKey).isTrue()
        assertThat(EnginePickerState.of("system", "none").showSarvamMode).isFalse()
    }

    @Test
    fun custom_brain_needs_url_and_key() {
        val s = EnginePickerState.of(earId = "system", brainId = "custom")
        assertThat(s.needsUrl).isTrue()
        assertThat(s.needsKey).isTrue()
        assertThat(s.rewrite).isTrue()
    }

    @Test
    fun mask_key_shows_last_four_never_full() {
        assertThat(EnginePickerState.maskKey("sk-abcdefghijklmnop")).isEqualTo("••••mnop")
        assertThat(EnginePickerState.maskKey("")).isEmpty()
        assertThat(EnginePickerState.maskKey("ab")).isEqualTo("••••")
        assertThat(EnginePickerState.maskKey("sk-abcdefghijklmnop"))
            .doesNotContain("sk-abcdefghijklmnop")
    }

    @Test
    fun unknown_ids_safe_default() {
        val s = EnginePickerState.of(earId = "nope", brainId = "zzz")
        assertThat(s.rewrite).isFalse()
        assertThat(s.commandMode).isFalse()
        assertThat(s.needsKey).isFalse()
        assertThat(s.honesty).isEqualTo("On this phone. Phone STT may still use Google.")
    }

    @Test
    fun presets_include_spec_ids() {
        val ears = EnginePickerState.ears.map { it.id }
        val brains = EnginePickerState.brains.map { it.id }
        assertThat(ears).containsAtLeast(
            "system", "on_phone", "laptop", "openai", "deepgram", "assemblyai", "sarvam", "custom_stt"
        )
        assertThat(brains).containsAtLeast(
            "none", "on_phone", "laptop", "openai", "grok", "minimax", "deepseek",
            "gemini", "mistral", "together", "fireworks", "openrouter", "sarvam",
            "anthropic", "custom"
        )
        assertThat(brains).doesNotContain("groq")
        assertThat(ears).doesNotContain("groq")
    }
}
