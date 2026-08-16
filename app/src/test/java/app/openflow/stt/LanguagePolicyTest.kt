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
    fun normalize_handles_supported_languages() {
        assertThat(LanguagePolicy.normalize(null)).isEqualTo("en-US")
        assertThat(LanguagePolicy.normalize("")).isEqualTo("en-US")
        assertThat(LanguagePolicy.normalize("fr-fr")).isEqualTo("fr-FR")
        assertThat(LanguagePolicy.normalize("hi-in")).isEqualTo("hi-IN")
        assertThat(LanguagePolicy.normalize("es-es")).isEqualTo("es-ES")
        assertThat(LanguagePolicy.normalize("en-gb")).isEqualTo("en-GB")
        assertThat(LanguagePolicy.normalize("en")).isEqualTo("en-US")
        assertThat(LanguagePolicy.normalize("en-US")).isEqualTo("en-US")
        assertThat(LanguagePolicy.normalize("ja-jp")).isEqualTo("ja-JP")
    }

    @Test
    fun isAllowed_validates_catalog() {
        assertThat(LanguagePolicy.isAllowed("en")).isTrue()
        assertThat(LanguagePolicy.isAllowed("en-US")).isTrue()
        assertThat(LanguagePolicy.isAllowed("EN-us")).isTrue()
        assertThat(LanguagePolicy.isAllowed("en-GB")).isTrue()
        assertThat(LanguagePolicy.isAllowed("fr-FR")).isTrue()
        assertThat(LanguagePolicy.isAllowed("es-ES")).isTrue()
        assertThat(LanguagePolicy.isAllowed("hi-IN")).isTrue()
        assertThat(LanguagePolicy.isAllowed("unknown-locale")).isFalse()
        assertThat(LanguagePolicy.isAllowed("")).isFalse()
        assertThat(LanguagePolicy.isAllowed(null)).isFalse()
    }
}
