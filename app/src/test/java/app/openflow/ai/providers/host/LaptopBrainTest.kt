package app.openflow.ai.providers.host

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LaptopBrainTest {

    @Test
    fun name_is_laptop() {
        assertThat(LaptopBrain(baseUrl = "http://192.168.1.5:11434/v1").name).isEqualTo("laptop")
    }

    @Test
    fun missing_url_returns_identity() = runTest {
        val brain = LaptopBrain(baseUrl = null)
        assertThat(brain.enhance("keep me")).isEqualTo("keep me")
        assertThat(LaptopBrain(baseUrl = "").enhance("x")).isEqualTo("x")
        assertThat(LaptopBrain(baseUrl = "file:///tmp").enhance("x")).isEqualTo("x")
        assertThat(LaptopBrain(baseUrl = "http://example.com/v1").enhance("x")).isEqualTo("x")
    }

    @Test
    fun lan_url_posts_chat_completions() = runTest {
        var seenUrl = ""
        var seenJson = ""
        val brain = LaptopBrain(
            baseUrl = "http://192.168.1.5:11434/v1",
            model = "llama3.2",
            post = { url, _, json ->
                seenUrl = url
                seenJson = json
                """{"choices":[{"message":{"content":"cleaned"}}]}"""
            },
        )
        assertThat(brain.enhance("raw talk", "cleanup")).isEqualTo("cleaned")
        assertThat(seenUrl).isEqualTo("http://192.168.1.5:11434/v1/chat/completions")
        assertThat(seenJson).contains("llama3.2")
        assertThat(seenJson).contains("raw talk")
        assertThat(seenJson).contains("Do not invent facts")
    }

    @Test
    fun command_mode_sends_user_text() = runTest {
        var seenJson = ""
        val brain = LaptopBrain(
            baseUrl = "http://10.0.0.2:8080/v1",
            post = { _, _, json ->
                seenJson = json
                """{"choices":[{"message":{"content":"bullets"}}]}"""
            },
        )
        assertThat(brain.enhance("make this shorter", "command")).isEqualTo("bullets")
        assertThat(seenJson).contains("make this shorter")
    }

    @Test
    fun post_failure_returns_identity() = runTest {
        val brain = LaptopBrain(
            baseUrl = "http://127.0.0.1:11434/v1",
            post = { _, _, _ -> error("down") },
        )
        assertThat(brain.enhance("raw")).isEqualTo("raw")
    }

    @Test
    fun laptop_caps_match_plan() {
        val brain = LaptopBrain(baseUrl = "https://tailscale-box.example/v1")
        assertThat(brain.rewrite).isTrue()
        assertThat(brain.commandMode).isTrue()
        assertThat(brain.needsNet).isTrue()
        assertThat(brain.audioLeavesDevice).isTrue()
    }
}
