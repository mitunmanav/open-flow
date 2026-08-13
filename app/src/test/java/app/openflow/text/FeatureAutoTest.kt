package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeatureAutoTest {

    private val aiOn = setOf(
        Feature.HIGH_AI,
        Feature.UNDO_AI,
        Feature.COMMAND,
        Feature.AI_STYLE,
        Feature.AI_BACKTRACK,
        Feature.FIELD_CONTEXT,
    )

    @Test
    fun phone_stt_rules_brain_high_is_rules() {
        val on = FeatureAuto.of("system", "none")
        assertThat(on).containsNoneIn(aiOn)
        assertThat(on).doesNotContain(Feature.LIVE_PARTIAL)
        assertThat(on).doesNotContain(Feature.SARVAM_MODE)
        assertThat(on).doesNotContain(Feature.MULTILINGUAL)
        assertThat(FeatureAuto.earNeedsNet("system")).isFalse()
    }

    @Test
    fun on_phone_brain_no_command_no_high_ai() {
        val on = FeatureAuto.of("system", "on_phone")
        assertThat(on).doesNotContain(Feature.HIGH_AI)
        assertThat(on).doesNotContain(Feature.COMMAND)
        assertThat(on).containsNoneIn(aiOn)
        assertThat(FeatureAuto.earNeedsNet("on_phone")).isFalse()
    }

    @Test
    fun any_other_brain_lights_ai_features() {
        for (brain in listOf(
            "openai", "grok", "laptop", "anthropic", "custom", "minimax",
            "deepseek", "gemini", "mistral", "together", "fireworks",
            "openrouter", "sarvam",
        )) {
            val on = FeatureAuto.of("system", brain)
            assertThat(on).containsAtLeastElementsIn(aiOn)
        }
    }

    @Test
    fun cloud_ears_live_partial_and_need_net() {
        for (ear in listOf("openai", "deepgram", "assemblyai", "sarvam", "custom_stt")) {
            val on = FeatureAuto.of(ear, "none")
            assertThat(on).contains(Feature.LIVE_PARTIAL)
            assertThat(on).contains(Feature.MULTILINGUAL)
            assertThat(on).doesNotContain(Feature.HIGH_AI)
            assertThat(on).doesNotContain(Feature.COMMAND)
            assertThat(FeatureAuto.earNeedsNet(ear)).isTrue()
        }
    }

    @Test
    fun laptop_ear_not_cloud() {
        val on = FeatureAuto.of("laptop", "none")
        assertThat(on).doesNotContain(Feature.LIVE_PARTIAL)
        assertThat(FeatureAuto.earNeedsNet("laptop")).isFalse()
    }

    @Test
    fun sarvam_ear_unlocks_mode() {
        assertThat(FeatureAuto.of("sarvam", "none")).contains(Feature.SARVAM_MODE)
        assertThat(FeatureAuto.of("openai", "none")).doesNotContain(Feature.SARVAM_MODE)
        assertThat(FeatureAuto.of("system", "none")).doesNotContain(Feature.SARVAM_MODE)
    }

    @Test
    fun multilingual_two_langs_or_cloud_ear() {
        assertThat(FeatureAuto.of("system", "none", setOf("en-US", "hi-IN")))
            .contains(Feature.MULTILINGUAL)
        assertThat(FeatureAuto.of("system", "none", setOf("en-US")))
            .doesNotContain(Feature.MULTILINGUAL)
        assertThat(FeatureAuto.of("deepgram", "none", setOf("en-US")))
            .contains(Feature.MULTILINGUAL)
    }

    @Test
    fun ids_case_insensitive() {
        val on = FeatureAuto.of("SARVAM", "GROK")
        assertThat(on).contains(Feature.SARVAM_MODE)
        assertThat(on).contains(Feature.HIGH_AI)
        assertThat(on).contains(Feature.COMMAND)
        assertThat(on).contains(Feature.LIVE_PARTIAL)
        assertThat(FeatureAuto.earNeedsNet("OpenAI")).isTrue()
    }

    @Test
    fun cloud_ear_plus_brain_unions() {
        val on = FeatureAuto.of("openai", "openai")
        assertThat(on).containsAtLeastElementsIn(aiOn)
        assertThat(on).contains(Feature.LIVE_PARTIAL)
        assertThat(on).contains(Feature.MULTILINGUAL)
        assertThat(on).doesNotContain(Feature.SARVAM_MODE)
        assertThat(on).doesNotContain(Feature.EAR_PUNCT)
    }
}
