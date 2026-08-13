package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeatureGateTest {

    @Test
    fun noai_system_only_local_features() {
        val on = Feature.entries.filter { feature ->
            FeatureGate.can(
                feature,
                brainRewrite = false,
                brainCommand = false,
                streamLive = true,
                earPunct = true,
                languages = setOf("en-US"),
                earNeedsNet = false,
                earId = "system",
            )
        }
        assertThat(on).containsExactly(Feature.LIVE_PARTIAL, Feature.EAR_PUNCT)
    }

    @Test
    fun grok_brain_unlocks_high_and_command() {
        assertThat(
            FeatureGate.can(Feature.HIGH_AI, brainRewrite = true, brainCommand = true)
        ).isTrue()
        assertThat(
            FeatureGate.can(Feature.COMMAND, brainRewrite = true, brainCommand = true)
        ).isTrue()
        assertThat(FeatureGate.can(Feature.UNDO_AI, brainRewrite = true)).isTrue()
        assertThat(FeatureGate.can(Feature.AI_STYLE, brainRewrite = true)).isTrue()
        assertThat(FeatureGate.can(Feature.AI_BACKTRACK, brainRewrite = true)).isTrue()
        assertThat(FeatureGate.can(Feature.FIELD_CONTEXT, brainRewrite = true)).isTrue()
        assertThat(FeatureGate.can(Feature.HIGH_AI, brainRewrite = false)).isFalse()
        assertThat(FeatureGate.can(Feature.COMMAND, brainCommand = false)).isFalse()
    }

    @Test
    fun sarvam_ear_unlocks_sarvam_mode() {
        assertThat(FeatureGate.can(Feature.SARVAM_MODE, earId = "sarvam")).isTrue()
        assertThat(FeatureGate.can(Feature.SARVAM_MODE, earId = "system")).isFalse()
    }

    @Test
    fun multilingual_many_langs_or_unknown_net_ear() {
        assertThat(
            FeatureGate.can(Feature.MULTILINGUAL, languages = setOf("en-US", "hi-IN"))
        ).isTrue()
        assertThat(
            FeatureGate.can(
                Feature.MULTILINGUAL,
                languages = emptySet(),
                earNeedsNet = true,
            )
        ).isTrue()
        assertThat(
            FeatureGate.can(Feature.MULTILINGUAL, languages = setOf("en-US"))
        ).isFalse()
    }
}
