package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LanguagePolicyTest {

    @Test
    fun force_keeps_catalog_langs() {
        assertThat(LanguagePolicy.force(null)).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("")).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("hi-IN")).isEqualTo("hi-IN")
        assertThat(LanguagePolicy.force("en-IN")).isEqualTo("en-IN")
        assertThat(LanguagePolicy.force("en-GB")).isEqualTo("en-GB")
        assertThat(LanguagePolicy.force("fr-FR")).isEqualTo("fr-FR")
        assertThat(LanguagePolicy.force("en")).isEqualTo("en-US")
        assertThat(LanguagePolicy.force("nope")).isEqualTo("en-US")
    }

    @Test
    fun iso639_from_tag() {
        assertThat(LanguagePolicy.iso639("hi-IN")).isEqualTo("hi")
        assertThat(LanguagePolicy.iso639("en-IN")).isEqualTo("en")
        assertThat(LanguagePolicy.iso639("zh-CN")).isEqualTo("zh")
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
