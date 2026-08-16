package app.openflow.ai.providers.cloud

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AllBrainsTest {

    private class RecordingHttp(
        var responseContent: String = "polished test text"
    ) : CloudHttp {
        var lastUrl: String? = null
        var lastHeaders: Map<String, String> = emptyMap()
        var lastBody: String? = null
        var callCount = 0

        override fun post(url: String, headers: Map<String, String>, json: String): String {
            callCount++
            lastUrl = url
            lastHeaders = headers
            lastBody = json
            return if (url.contains("anthropic.com")) {
                """{"content":[{"type":"text","text":"$responseContent"}]}"""
            } else {
                """{"choices":[{"message":{"content":"$responseContent"}}]}"""
            }
        }
    }

    private val allCloudBrainIds = listOf(
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
    fun test_all_openai_compatible_brains_post_valid_payload_and_headers() = runTest {
        for (id in allCloudBrainIds) {
            val http = RecordingHttp()
            val brain = CloudProviders.brain(
                id = id,
                apiKey = { "test-key-$id" },
                model = "test-model-$id",
                baseUrl = null,
                http = http,
            )

            assertThat(brain).isNotNull()
            val result = brain!!.enhance("um hello world", "cleanup")
            assertThat(result).isEqualTo("polished test text")
            assertThat(http.callCount).isEqualTo(1)
            assertThat(http.lastUrl).startsWith("https://")
            assertThat(http.lastUrl).endsWith("/chat/completions")
            assertThat(http.lastHeaders["Content-Type"]).isEqualTo("application/json")
            assertThat(http.lastHeaders["Authorization"]).isEqualTo("Bearer test-key-$id")
            assertThat(http.lastBody).contains("\"temperature\":0.1")
            assertThat(http.lastBody).contains("Clean dictation")

            if (id == "sarvam") {
                assertThat(http.lastHeaders["api-subscription-key"]).isEqualTo("test-key-sarvam")
            }
        }
    }

    @Test
    fun test_anthropic_brain_posts_messages_and_x_api_key() = runTest {
        val http = RecordingHttp()
        val brain = CloudProviders.brain(
            id = "anthropic",
            apiKey = { "sk-ant-test" },
            model = "claude-3-5-sonnet-20241022",
            baseUrl = null,
            http = http,
        )

        assertThat(brain).isNotNull()
        val result = brain!!.enhance("um test dictation", "cleanup")
        assertThat(result).isEqualTo("polished test text")
        assertThat(http.lastUrl).isEqualTo("https://api.anthropic.com/v1/messages")
        assertThat(http.lastHeaders["x-api-key"]).isEqualTo("sk-ant-test")
        assertThat(http.lastHeaders["anthropic-version"]).isEqualTo("2023-06-01")
        assertThat(http.lastBody).contains("max_tokens")
    }

    @Test
    fun test_custom_brain_uses_custom_base_url() = runTest {
        val http = RecordingHttp()
        val brain = CloudProviders.brain(
            id = "custom",
            apiKey = { "custom-token" },
            model = "custom-llm",
            baseUrl = "https://my-custom-llm.com/v1",
            http = http,
        )

        assertThat(brain).isNotNull()
        val result = brain!!.enhance("clean this", "cleanup")
        assertThat(result).isEqualTo("polished test text")
        assertThat(http.lastUrl).isEqualTo("https://my-custom-llm.com/v1/chat/completions")
        assertThat(http.lastHeaders["Authorization"]).isEqualTo("Bearer custom-token")
    }
}
