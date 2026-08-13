package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SentenceFormatTest {

    @Test
    fun capitalizes_each_sentence_start() {
        val out = SentenceFormat.capitalizeSentences("hello there. how are you")
        assertThat(out).isEqualTo("Hello there. How are you")
    }

    @Test
    fun keeps_existing_acronym_caps() {
        val out = SentenceFormat.capitalizeSentences("API is ready")
        assertThat(out).contains("API")
        assertThat(out).isEqualTo("API is ready")
    }

    @Test
    fun empty_stays_empty() {
        assertThat(SentenceFormat.capitalizeSentences("")).isEmpty()
    }

    @Test
    fun capitalizes_after_bang_and_question() {
        val out = SentenceFormat.capitalizeSentences("wait. go now! are you sure?")
        assertThat(out).isEqualTo("Wait. Go now! Are you sure?")
    }

    @Test
    fun does_not_strip_fillers() {
        val out = SentenceFormat.capitalizeSentences("um hello. uh yes")
        assertThat(out.lowercase()).contains("um")
        assertThat(out.lowercase()).contains("uh")
        assertThat(out).isEqualTo("Um hello. Uh yes")
    }
}
