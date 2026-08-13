package app.openflow.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderNoticeTest {
    @Test
    fun system_plus_none() {
        assertThat(ProviderNotice.line("system", "none"))
            .isEqualTo("On this phone. Phone STT may still use Google.")
    }

    @Test
    fun on_phone_plus_none() {
        assertThat(ProviderNotice.line("on_phone", "none"))
            .isEqualTo("Audio stays on this phone.")
    }

    @Test
    fun grok_is_xai_not_groq() {
        assertThat(ProviderNotice.line("system", "grok"))
            .isEqualTo("Text of this utterance goes to xAI (Grok). Not Groq.")
    }

    @Test
    fun openai_ear() {
        assertThat(ProviderNotice.line("openai", "none"))
            .isEqualTo("Your voice goes to OpenAI.")
    }

    @Test
    fun laptop() {
        assertThat(ProviderNotice.line("laptop", "none"))
            .isEqualTo("Audio/text goes to the computer you set.")
        assertThat(ProviderNotice.line("system", "laptop"))
            .isEqualTo("Audio/text goes to the computer you set.")
    }

    @Test
    fun no_fake_anonymous_claims() {
        val lines = listOf(
            ProviderNotice.line("system", "none"),
            ProviderNotice.line("on_phone", "none"),
            ProviderNotice.line("system", "grok"),
            ProviderNotice.line("openai", "none"),
            ProviderNotice.line("laptop", "none"),
            ProviderNotice.line("deepgram", "openai"),
        )
        for (line in lines) {
            val lower = line.lowercase()
            assertThat(lower).doesNotContain("anonymous")
            assertThat(lower).doesNotContain("anonymize")
            assertThat(lower).doesNotContain("they cannot")
            assertThat(lower).doesNotContain("private")
        }
    }
}
