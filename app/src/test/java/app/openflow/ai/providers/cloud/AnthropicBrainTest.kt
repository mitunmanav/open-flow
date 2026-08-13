package app.openflow.ai.providers.cloud

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AnthropicBrainTest {

    private class FakeHttp : CloudHttp {
        var calls = 0
        var url: String? = null
        var headers: Map<String, String> = emptyMap()
        var json: String? = null
        var response: String = """{"content":[{"type":"text","text":"polished"}]}"""

        override fun post(url: String, headers: Map<String, String>, json: String): String {
            calls++
            this.url = url
            this.headers = headers
            this.json = json
            return response
        }
    }

    @Test
    fun empty_key_returns_original() = runTest {
        val http = FakeHttp()
        val out = AnthropicBrain(apiKey = { "" }, model = "claude-sonnet-4-20250514", http = http)
            .enhance("keep", "cleanup")
        assertThat(out).isEqualTo("keep")
        assertThat(http.calls).isEqualTo(0)
    }

    @Test
    fun posts_messages_with_x_api_key() = runTest {
        val http = FakeHttp()
        val out = AnthropicBrain(apiKey = { "ant-key" }, model = "claude-sonnet-4-20250514", http = http)
            .enhance("hello", "cleanup")
        assertThat(out).isEqualTo("polished")
        assertThat(http.url).isEqualTo("https://api.anthropic.com/v1/messages")
        assertThat(http.headers["x-api-key"]).isEqualTo("ant-key")
        assertThat(http.headers["anthropic-version"]).isNotEmpty()
        assertThat(http.headers["Authorization"]).isNull()
        assertThat(http.json).contains("\"model\":\"claude-sonnet-4-20250514\"")
        assertThat(http.json).contains("Clean dictation")
        assertThat(http.json).contains("hello")
    }

    @Test
    fun command_uses_instruction() = runTest {
        val http = FakeHttp()
        AnthropicBrain(apiKey = { "k" }, model = "claude-sonnet-4-20250514", http = http)
            .enhance("bullet this", "command")
        assertThat(http.json).contains("bullet this")
        assertThat(http.json).contains("\"role\":\"user\"")
    }

    @Test
    fun capability_is_cloud_brain() {
        val b = AnthropicBrain(apiKey = { "k" }, model = "claude-sonnet-4-20250514", http = { _, _, _ -> "" })
        assertThat(b.rewrite).isTrue()
        assertThat(b.commandMode).isTrue()
        assertThat(b.needsNet).isTrue()
        assertThat(b.name).isEqualTo("anthropic")
    }
}
