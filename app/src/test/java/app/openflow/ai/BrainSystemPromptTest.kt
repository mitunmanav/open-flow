package app.openflow.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BrainSystemPromptTest {

    @Test
    fun cleanup_without_hints_is_clean_constant() {
        assertThat(BrainSystemPrompt.cleanup("cleanup")).isEqualTo(BrainSystemPrompt.CLEAN)
        assertThat(BrainSystemPrompt.cleanup("cleanup")).contains("do not invent facts")
    }

    @Test
    fun cleanup_appends_utterance_spell_hints() {
        val out = BrainSystemPrompt.cleanup("cleanup spell: mike→Mic")
        assertThat(out).startsWith(BrainSystemPrompt.CLEAN)
        assertThat(out).contains("Spell: mike→Mic")
    }
}
