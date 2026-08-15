package app.openflow.ui.copy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UiCopyTest {

    @Test
    fun sentenceLabel_lowercases_internal_words() {
        assertThat(UiCopy.sentenceLabel("Save Word")).isEqualTo("Save word")
        assertThat(UiCopy.sentenceLabel("Bubble Settings")).isEqualTo("Bubble settings")
        assertThat(UiCopy.sentenceLabel("Clear All Learned")).isEqualTo("Clear all learned")
    }

    @Test
    fun sentenceLabel_keeps_known_proper_nouns() {
        assertThat(UiCopy.sentenceLabel("Speech + AI")).isEqualTo("Speech + AI")
        assertThat(UiCopy.sentenceLabel("Open Flow")).isEqualTo("Open Flow")
        assertThat(UiCopy.sentenceLabel("OpenAI Realtime")).isEqualTo("OpenAI realtime")
    }

    @Test
    fun sentenceLabel_blank_safe() {
        assertThat(UiCopy.sentenceLabel("")).isEqualTo("")
        assertThat(UiCopy.sentenceLabel("  ")).isEqualTo("")
    }
}
