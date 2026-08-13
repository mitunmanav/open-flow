package app.openflow.ai.providers.cloud

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OpenAiCompatBrainTest {

    private class FakeHttp : CloudHttp {
        var calls = 0
        var url: String? = null
        var headers: Map<String, String> = emptyMap()
        var json: String? = null
        var response: String = """{"choices":[{"message":{"content":"cleaned"}}]}"""
        var throwOnPost = false

        override fun post(url: String, headers: Map<String, String>, json: String): String {
            if (throwOnPost) error("net down")
            calls++
            this.url = url
            this.headers = headers
            this.json = json
            return response
        }
    }

    private fun brain(
        key: String = "sk-test",
        id: String = "openai",
        url: String = "https://api.openai.com/v1",
        http: FakeHttp = FakeHttp(),
    ) = OpenAiCompatBrain(
        id = id,
        apiKey = { key },
        model = "gpt-4o-mini",
        baseUrl = url,
        http = http,
    )

    @Test
    fun empty_key_returns_original_no_http() = runTest {
        val http = FakeHttp()
        val out = brain(key = "", http = http).enhance("keep me", "cleanup")
        assertThat(out).isEqualTo("keep me")
        assertThat(http.calls).isEqualTo(0)
    }

    @Test
    fun cleanup_posts_chat_completions_with_bearer() = runTest {
        val http = FakeHttp()
        val out = brain(http = http).enhance("hello world", "cleanup")
        assertThat(out).isEqualTo("cleaned")
        assertThat(http.url).isEqualTo("https://api.openai.com/v1/chat/completions")
        assertThat(http.headers["Authorization"]).isEqualTo("Bearer sk-test")
        assertThat(http.headers["Content-Type"]).isEqualTo("application/json")
        assertThat(http.json).contains("\"model\":\"gpt-4o-mini\"")
        assertThat(http.json).contains("Clean dictation")
        assertThat(http.json).contains("do not invent facts")
        assertThat(http.json).contains("hello world")
        assertThat(http.json!!.lowercase()).doesNotContain("groq")
    }

    @Test
    fun command_puts_instruction_in_system_and_user() = runTest {
        val http = FakeHttp()
        brain(http = http).enhance("make this shorter", "command")
        assertThat(http.json).contains("\"role\":\"system\"")
        assertThat(http.json).contains("\"role\":\"user\"")
        assertThat(http.json).contains("make this shorter")
        val sys = http.json!!.indexOf("\"role\":\"system\"")
        val user = http.json!!.indexOf("\"role\":\"user\"")
        assertThat(http.json!!.indexOf("make this shorter", sys)).isGreaterThan(sys)
        assertThat(http.json!!.indexOf("make this shorter", user)).isGreaterThan(user)
    }

    @Test
    fun grok_hits_xai_not_groq() = runTest {
        val http = FakeHttp()
        val url = NamedCloud.brainBaseUrl("grok")!!
        brain(id = "grok", url = url, http = http).enhance("hi", "cleanup")
        assertThat(http.url).isEqualTo("https://api.x.ai/v1/chat/completions")
        assertThat(http.url).doesNotContain("groq")
    }

    @Test
    fun strips_email_and_phone_before_post() = runTest {
        val http = FakeHttp()
        brain(http = http).enhance("mail me at a@b.com or 9876543210 thanks", "cleanup")
        assertThat(http.json).doesNotContain("a@b.com")
        assertThat(http.json).doesNotContain("9876543210")
        assertThat(http.json).contains("mail me at")
        assertThat(http.json).contains("thanks")
    }

    @Test
    fun http_fail_returns_original() = runTest {
        val http = FakeHttp().also { it.throwOnPost = true }
        val out = brain(http = http).enhance("safe", "cleanup")
        assertThat(out).isEqualTo("safe")
    }

    @Test
    fun capability_is_cloud_brain() {
        val b = brain()
        assertThat(b.rewrite).isTrue()
        assertThat(b.commandMode).isTrue()
        assertThat(b.needsNet).isTrue()
        assertThat(b.audioLeavesDevice).isTrue()
        assertThat(b.name).isEqualTo("openai")
    }
}
