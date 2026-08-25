package app.openflow.ui.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SarvamModeSuggestTest {

    @Test
    fun hindi_tag_suggests_codemix() {
        assertThat(SarvamModeSuggest.suggest("hi-IN", "transcribe")).isEqualTo("codemix")
    }

    @Test
    fun indian_english_tag_suggests_codemix() {
        assertThat(SarvamModeSuggest.suggest("en-IN", "transcribe")).isEqualTo("codemix")
    }

    @Test
    fun already_codemix_suggests_nothing() {
        assertThat(SarvamModeSuggest.suggest("hi-IN", "codemix")).isNull()
    }

    @Test
    fun other_locales_left_alone() {
        assertThat(SarvamModeSuggest.suggest("en-US", "transcribe")).isNull()
        assertThat(SarvamModeSuggest.suggest("ta-IN", "verbatim")).isNull()
        assertThat(SarvamModeSuggest.suggest(null, "transcribe")).isNull()
        assertThat(SarvamModeSuggest.suggest("", "transcribe")).isNull()
    }

    @Test
    fun case_and_space_tolerant() {
        assertThat(SarvamModeSuggest.suggest(" HI-in ", "translit")).isEqualTo("codemix")
    }
}
