package app.openflow.ui.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnginePickerStateTest {

    @Test
    fun auto_route_is_exposed_by_picker_state() {
        assertThat(EnginePickerState.of(autoRoute = false).autoRoute).isFalse()
        assertThat(EnginePickerState.of(autoRoute = true).autoRoute).isTrue()
        assertThat(EnginePickerState.manualFallbackHint(autoRoute = false)).isNull()
        assertThat(EnginePickerState.manualFallbackHint(autoRoute = true))
            .isEqualTo(EnginePickerState.OVERRIDE_MANUAL_FALLBACK)
    }

    @Test
    fun brain_none_high_and_command_off() {
        val s = EnginePickerState.of(earId = "system", brainId = "none")
        assertThat(s.rewrite).isFalse()
        assertThat(s.commandMode).isFalse()
        assertThat(s.needsKey).isFalse()
        assertThat(s.highLabel).isEqualTo("High (rules)")
        assertThat(s.commandWhy).isEqualTo("Voice commands need a rewrite brain")
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
        assertThat(s.honesty).isEqualTo("Local. Audio stays on this phone.")
        assertThat(s.needsKey).isFalse()
    }

    @Test
    fun ear_laptop_audio_to_computer() {
        val s = EnginePickerState.of(earId = "laptop", brainId = "none")
        assertThat(s.honesty).isEqualTo("Online. Audio goes to your computer.")
        assertThat(s.needsUrl).isTrue()
    }

    @Test
    fun system_none_may_use_google() {
        val s = EnginePickerState.of(earId = "system", brainId = "none")
        assertThat(s.honesty).isEqualTo("Local. On this phone. Phone speech may still use Google.")
    }

    @Test
    fun grok_is_xai_not_groq() {
        val s = EnginePickerState.of(earId = "system", brainId = "grok")
        assertThat(s.honesty).isEqualTo("Online. Text of this utterance goes to xAI (Grok). Not Groq.")
        assertThat(s.needsKey).isTrue()
        assertThat(s.rewrite).isTrue()
        assertThat(s.honesty).contains("Not Groq")
        assertThat(s.honesty).contains("xAI")
    }

    @Test
    fun openai_ear_voice_to_openai() {
        val s = EnginePickerState.of(earId = "openai", brainId = "none")
        assertThat(s.honesty).isEqualTo("Online. Your voice goes to OpenAI.")
        assertThat(s.needsKey).isTrue()
    }

    @Test
    fun brain_laptop_to_their_computer() {
        val s = EnginePickerState.of(earId = "system", brainId = "laptop")
        assertThat(s.honesty).isEqualTo("Online. Text goes to the computer you set.")
        assertThat(s.needsUrl).isTrue()
        assertThat(s.needsKey).isTrue()
        assertThat(s.rewrite).isTrue()
        assertThat(s.commandMode).isTrue()
    }

    @Test
    fun laptop_brain_shows_key_field() {
        val s = EnginePickerState.of(earId = "system", brainId = "laptop")
        assertThat(s.needsKey).isTrue()
        assertThat(EnginePickerState.of("system", "none").needsKey).isFalse()
    }

    @Test
    fun missing_key_line_only_when_needed_and_empty() {
        assertThat(EnginePickerState.missingKeyLine(needsKey = true, keyMask = ""))
            .isEqualTo("Add an API key below — this choice needs the network.")
        assertThat(EnginePickerState.missingKeyLine(needsKey = true, keyMask = "••••abcd"))
            .isNull()
        assertThat(EnginePickerState.missingKeyLine(needsKey = false, keyMask = ""))
            .isNull()
        assertThat(EnginePickerState.missingKeyLine(needsKey = false, keyMask = "••••abcd"))
            .isNull()
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
        assertThat(s.honesty).isEqualTo("Local. On this phone. Phone speech may still use Google.")
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

    @Test
    fun default_chips_high_command_grey_live_on() {
        val s = EnginePickerState.of(earId = "system", brainId = "none")
        assertThat(s.livePartials).isTrue()
        assertThat(s.chips.map { it.id }).containsExactly(
            "high_ai", "command", "live_partials", "sarvam"
        ).inOrder()
        assertThat(chip(s, "high_ai").lit).isFalse()
        assertThat(chip(s, "command").lit).isFalse()
        assertThat(chip(s, "live_partials").lit).isTrue()
        assertThat(chip(s, "sarvam").lit).isFalse()
        assertThat(chip(s, "high_ai").label).isEqualTo("High (rules)")
    }

    @Test
    fun rewrite_brain_lights_high_and_command() {
        val s = EnginePickerState.of(earId = "system", brainId = "openai")
        assertThat(chip(s, "high_ai").lit).isTrue()
        assertThat(chip(s, "command").lit).isTrue()
        assertThat(chip(s, "high_ai").label).isEqualTo("High AI")
        assertThat(chip(s, "sarvam").lit).isFalse()
    }

    @Test
    fun cloud_ear_lights_live_partials() {
        for (ear in listOf("openai", "deepgram", "assemblyai", "sarvam")) {
            val s = EnginePickerState.of(earId = ear, brainId = "none")
            assertThat(s.livePartials).isTrue()
            assertThat(chip(s, "live_partials").lit).isTrue()
        }
    }

    @Test
    fun sarvam_ear_lights_sarvam_chip() {
        val s = EnginePickerState.of(earId = "sarvam", brainId = "none")
        assertThat(chip(s, "sarvam").lit).isTrue()
        assertThat(chip(s, "sarvam").label).isEqualTo("Sarvam modes")
    }

    @Test
    fun on_phone_brain_does_not_light_rewrite() {
        val s = EnginePickerState.of(earId = "system", brainId = "on_phone")
        assertThat(s.rewrite).isFalse()
        assertThat(s.commandMode).isFalse()
        assertThat(chip(s, "high_ai").lit).isFalse()
        assertThat(chip(s, "command").lit).isFalse()
    }

    @Test
    fun chips_are_indicators_not_toggles() {
        val ids = EnginePickerState.of("system", "none").chips.map { it.id }
        assertThat(ids).doesNotContain("cleanup")
        assertThat(ids).hasSize(4)
    }

    @Test
    fun launch_default_is_local_phone_stt_rules() {
        val s = EnginePickerState.of()
        assertThat(s.earId).isEqualTo("system")
        assertThat(s.brainId).isEqualTo("none")
        assertThat(s.needsKey).isFalse()
        assertThat(s.honesty).contains("Phone speech may still use Google")
    }

    @Test
    fun launch_ears_system_and_cloud_enabled() {
        assertThat(EnginePickerState.earEnabled("system")).isTrue()
        for (id in listOf("openai", "deepgram", "assemblyai", "sarvam")) {
            assertThat(EnginePickerState.earEnabled(id)).isTrue()
            assertThat(EnginePickerState.earDisabledReason(id)).isNull()
        }
        for (id in listOf("on_phone", "laptop", "custom_stt")) {
            assertThat(EnginePickerState.earEnabled(id)).isFalse()
            assertThat(EnginePickerState.earDisabledReason(id))
                .isEqualTo(EnginePickerState.STUB_EAR_REASON)
        }
        assertThat(EnginePickerState.earDisabledReason("system")).isNull()
    }

    @Test
    fun launch_brains_none_and_http_on_phone_off_url_gated() {
        assertThat(EnginePickerState.brainEnabled("none", url = "")).isTrue()
        assertThat(EnginePickerState.brainEnabled("openai", url = "")).isTrue()
        assertThat(EnginePickerState.brainEnabled("grok", url = "")).isTrue()
        assertThat(EnginePickerState.brainEnabled("anthropic", url = "")).isTrue()
        assertThat(EnginePickerState.brainEnabled("on_phone", url = "")).isFalse()
        assertThat(EnginePickerState.brainEnabled("laptop", url = "")).isFalse()
        assertThat(EnginePickerState.brainEnabled("custom", url = "not-a-url")).isFalse()
        assertThat(EnginePickerState.brainEnabled("laptop", url = "https://example.com/v1")).isTrue()
        assertThat(EnginePickerState.brainEnabled("custom", url = "http://192.168.1.1:11434/v1")).isTrue()
        assertThat(EnginePickerState.brainDisabledReason("on_phone", url = "")).isNotEmpty()
        assertThat(EnginePickerState.brainDisabledReason("laptop", url = "")).isNotEmpty()
        assertThat(EnginePickerState.brainDisabledReason("none", url = "")).isNull()
    }

    @Test
    fun settings_screen_wires_gates_no_silent_pick() {
        val src = java.io.File(
            app.openflow.ui.qa.UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/engine/EngineSettingsScreen.kt"
        ).readText()
        assertThat(src).contains("EnginePickerVisibility")
        assertThat(src).contains("visibleEars")
        assertThat(src).contains("visibleBrains")
        assertThat(src).contains("OpenDropdown")
        assertThat(src).contains("ear_dropdown")
        assertThat(src).contains("brain_dropdown")
        assertThat(src).contains("route_local_only")
        assertThat(src).contains("route_local_then_ai")
        assertThat(src).contains("route_ai_first")
        assertThat(src).contains("ai_when_every")
        assertThat(src).contains("ai_when_miss")
        assertThat(src).contains("onRouteMode")
        assertThat(src).contains("onAiWhen")
        assertThat(src).contains("manualFallbackHint")
        assertThat(src).contains("ear_override_hint")
        assertThat(src).contains("brain_override_hint")
        assertThat(src).doesNotContain("EnginePickerState.ears")
        assertThat(src).doesNotContain("EnginePickerState.brains")
    }

    @Test
    fun pathKind_local_vs_online() {
        assertThat(EnginePickerState.of("system", "none").pathKind).isEqualTo("Local")
        assertThat(EnginePickerState.of("openai", "none").pathKind).isEqualTo("Online")
        assertThat(EnginePickerState.of("system", "openai").pathKind).isEqualTo("Online")
        assertThat(EnginePickerState.of("on_phone", "none").pathKind).isEqualTo("Local")
    }

    @Test
    fun ear_sections_local_cloud_later() {
        val secs = EnginePickerState.earSections()
        assertThat(secs.map { it.id }).containsExactly("local", "cloud", "later").inOrder()
        assertThat(secs.map { it.title })
            .containsExactly("On this phone", "Cloud speech", "Coming later")
            .inOrder()
        assertThat(secs[0].items.map { it.id }).containsExactly("system")
        assertThat(secs[0].items.single().label).isEqualTo("Phone speech")
        assertThat(secs[1].items.map { it.id })
            .containsExactly("openai", "deepgram", "assemblyai", "sarvam")
            .inOrder()
        assertThat(secs[2].items.map { it.id })
            .containsExactly("on_phone", "laptop", "custom_stt")
            .inOrder()
    }

    @Test
    fun brain_sections_rules_cloud_later() {
        val secs = EnginePickerState.brainSections()
        assertThat(secs.map { it.id }).containsExactly("rules", "cloud", "later").inOrder()
        assertThat(secs.map { it.title })
            .containsExactly("No AI rewrite", "Cloud rewrite", "Coming later")
            .inOrder()
        assertThat(secs[0].items.map { it.id }).containsExactly("none")
        assertThat(secs[1].items.map { it.id }).contains("openai")
        assertThat(secs[1].items.map { it.id }).contains("sarvam")
        assertThat(secs[2].items.map { it.id })
            .containsExactly("on_phone", "laptop", "custom")
            .inOrder()
    }

    private fun chip(s: EnginePickerState, id: String): FeatureChip =
        s.chips.first { it.id == id }
}
