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

    @Test
    fun bias_extras_from_api_33() {
        assertThat(SttIntentPolicy.includeBiasing(32)).isFalse()
        assertThat(SttIntentPolicy.includeBiasing(33)).isTrue()
        assertThat(SttIntentPolicy.includeBiasing(36)).isTrue()
    }

    @Test
    fun language_extra_uses_catalog() {
        assertThat(SttIntentPolicy.languageTag(null)).isEqualTo(LanguagePolicy.DEFAULT_LANGUAGE)
        assertThat(SttIntentPolicy.languageTag("en-IN")).isEqualTo("en-IN")
        assertThat(SttIntentPolicy.languageTag("nope")).isEqualTo(LanguagePolicy.DEFAULT_LANGUAGE)
    }

    @Test
    fun listen_extras_pack_language_and_bias() {
        val dict = mapOf("acme corp" to "Acme Corp")
        val bias = SttBias.strings(dict, SttBias.fieldTokens("hello Acme"))
        val extras = SttIntentPolicy.listenExtras("en-IN", api = 33, biasing = bias)
        assertThat(extras.language).isEqualTo("en-IN")
        assertThat(extras.putBiasing).isTrue()
        assertThat(extras.biasing).contains("Acme Corp")
    }

    @Test
    fun listen_extras_skip_bias_before_api_33() {
        val extras = SttIntentPolicy.listenExtras("en-US", api = 32, biasing = listOf("Acme"))
        assertThat(extras.language).isEqualTo("en-US")
        assertThat(extras.putBiasing).isFalse()
        assertThat(extras.biasing).isEmpty()
    }
}
