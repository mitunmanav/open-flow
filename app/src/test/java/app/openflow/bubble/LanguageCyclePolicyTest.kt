package app.openflow.bubble

import app.openflow.stt.LanguagePolicy
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LanguageCyclePolicyTest {
    private val tags = listOf("en-US", "hi-IN", "es-ES")

    @Test
    fun badge_is_uppercase_prefix() {
        assertThat(LanguageCyclePolicy.badge("en-US")).isEqualTo("EN")
        assertThat(LanguageCyclePolicy.badge("hi-IN")).isEqualTo("HI")
        assertThat(LanguageCyclePolicy.badge("pt-BR")).isEqualTo("PT")
    }

    @Test
    fun badge_blank_falls_back_to_en() {
        assertThat(LanguageCyclePolicy.badge("")).isEqualTo("EN")
        assertThat(LanguageCyclePolicy.badge(null)).isEqualTo("EN")
    }

    @Test
    fun next_moves_forward_in_list() {
        assertThat(LanguageCyclePolicy.next("en-US", tags)).isEqualTo("hi-IN")
        assertThat(LanguageCyclePolicy.next("hi-IN", tags)).isEqualTo("es-ES")
    }

    @Test
    fun next_wraps_around() {
        assertThat(LanguageCyclePolicy.next("es-ES", tags)).isEqualTo("en-US")
    }

    @Test
    fun next_unknown_current_starts_at_first() {
        assertThat(LanguageCyclePolicy.next("fr-FR", tags)).isEqualTo("en-US")
        assertThat(LanguageCyclePolicy.next(null, tags)).isEqualTo("en-US")
    }

    @Test
    fun next_single_language_is_stable() {
        assertThat(LanguageCyclePolicy.next("en-US", listOf("en-US"))).isEqualTo("en-US")
    }

    @Test
    fun cycle_matches_supported_languages_catalog() {
        val catalog = LanguagePolicy.SUPPORTED_LANGUAGES.map { it.tag }
        var cur: String? = "en-US"
        repeat(catalog.size) {
            cur = LanguageCyclePolicy.next(cur!!, catalog)
        }
        assertThat(cur).isEqualTo("en-US")
    }
}
