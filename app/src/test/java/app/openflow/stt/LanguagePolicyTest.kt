package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LanguagePolicyTest {

    @Test
    fun force_maps_everything_to_en_us() {
        assertThat(LanguagePolicy.force(null)).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("")).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("fr-FR")).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("hi-IN")).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("es-ES")).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("en-GB")).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("en")).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("en-US")).isEqualTo("en-US")
    }

    @Test
    fun isAllowed_only_en_and_en_us() {
        assertThat(LanguagePolicy.isAllowed("en")).isTrue()
        assertThat(LanguagePolicy.isAllowed("en-US")).isTrue()
        assertThat(LanguagePolicy.isAllowed("EN-us")).isTrue()
        assertThat(LanguagePolicy.isAllowed("en-GB")).isFalse()
        assertThat(LanguagePolicy.isAllowed("fr-FR")).isFalse()
        assertThat(LanguagePolicy.isAllowed("es-ES")).isFalse()
        assertThat(LanguagePolicy.isAllowed("")).isFalse()
        assertThat(LanguagePolicy.isAllowed(null)).isFalse()
    }
}
