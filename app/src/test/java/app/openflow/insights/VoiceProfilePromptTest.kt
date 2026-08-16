package app.openflow.insights

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoiceProfilePromptTest {
    @Test
    fun build_mentions_json_keys_and_payload() {
        val msg = VoiceProfilePrompt.buildUserMessage(
            mapOf("totalWords" to 2000L, "topWords" to listOf("flow"))
        )
        assertThat(msg.lowercase()).contains("archetype")
        assertThat(msg).contains("2000")
        assertThat(msg).contains("flow")
    }

    @Test
    fun parse_plain_and_fenced() {
        assertThat(
            VoiceProfilePrompt.parseFlavor(
                """{"archetype":"Coder","catchphrase":"ship it","headline":"Night owl"}"""
            )
        ).isEqualTo(VoiceFlavor("Coder", "ship it", "Night owl"))
        assertThat(
            VoiceProfilePrompt.parseFlavor(
                "```json\n{\"archetype\":\"A\",\"catchphrase\":\"B\",\"headline\":\"C\"}\n```"
            )
        ).isEqualTo(VoiceFlavor("A", "B", "C"))
        assertThat(VoiceProfilePrompt.parseFlavor("nope")).isNull()
    }
}
