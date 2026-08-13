package app.openflow.ai.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NamedCloudTest {

    private val named = listOf(
        "openai",
        "grok",
        "minimax",
        "deepseek",
        "gemini",
        "mistral",
        "together",
        "fireworks",
        "openrouter",
        "sarvam",
    )

    @Test
    fun every_named_brain_is_https() {
        for (id in named) {
            val url = NamedCloud.brainBaseUrl(id)
            assertThat(url).isNotNull()
            assertThat(url!!).startsWith("https://")
        }
    }

    @Test
    fun grok_is_xai_not_groq() {
        assertThat(NamedCloud.brainBaseUrl("grok")).isEqualTo("https://api.x.ai/v1")
        assertThat(NamedCloud.brainBaseUrl("groq")).isNull()
    }

    @Test
    fun custom_and_unknown_are_null() {
        assertThat(NamedCloud.brainBaseUrl("custom")).isNull()
        assertThat(NamedCloud.brainBaseUrl("anthropic")).isNull()
        assertThat(NamedCloud.brainBaseUrl("")).isNull()
    }

    @Test
    fun spec_urls() {
        assertThat(NamedCloud.brainBaseUrl("openai")).isEqualTo("https://api.openai.com/v1")
        assertThat(NamedCloud.brainBaseUrl("minimax")).isEqualTo("https://api.minimax.io/v1")
        assertThat(NamedCloud.brainBaseUrl("deepseek")).isEqualTo("https://api.deepseek.com")
        assertThat(NamedCloud.brainBaseUrl("gemini"))
            .isEqualTo("https://generativelanguage.googleapis.com/v1beta/openai/")
        assertThat(NamedCloud.brainBaseUrl("mistral")).isEqualTo("https://api.mistral.ai/v1")
        assertThat(NamedCloud.brainBaseUrl("together")).isEqualTo("https://api.together.xyz/v1")
        assertThat(NamedCloud.brainBaseUrl("fireworks"))
            .isEqualTo("https://api.fireworks.ai/inference/v1")
        assertThat(NamedCloud.brainBaseUrl("openrouter")).isEqualTo("https://openrouter.ai/api/v1")
        assertThat(NamedCloud.brainBaseUrl("sarvam")).isEqualTo("https://api.sarvam.ai/v1")
    }
}
