package app.openflow.ai

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class NoAITest {

    @Test
    fun name_is_none() {
        assertThat(NoAI.name).isEqualTo("none")
    }

    @Test
    fun enhance_returns_text_unchanged() = runTest {
        assertThat(NoAI.enhance("hello world")).isEqualTo("hello world")
        assertThat(NoAI.enhance("", mode = "cleanup")).isEqualTo("")
        assertThat(NoAI.enhance("  spaced  ", mode = "formal")).isEqualTo("  spaced  ")
    }

    @Test
    fun no_internet_permission_in_manifest() {
        val manifest = locate("src/main/AndroidManifest.xml").readText()
        assertThat(manifest).doesNotContain("android.permission.INTERNET")
    }

    @Test
    fun ai_package_has_no_network_and_only_noai() {
        val dir = locate("src/main/java/app/openflow/ai")
        val sources = dir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertThat(sources).isNotEmpty()
        val text = sources.joinToString("\n") { it.readText() }
        val banned = listOf(
            "HttpURLConnection",
            "OkHttp",
            "okhttp",
            "Retrofit",
            "retrofit",
            "openai",
            "OpenAI",
            "gemini",
            "anthropic",
            "java.net.URL",
            "android.permission.INTERNET",
            "ktor",
            "Ktor"
        )
        for (b in banned) {
            assertWithMessage("ai/ must not contain $b").that(text).doesNotContain(b)
        }
        val impls = Regex("""(?:object|class)\s+(\w+)\s*:\s*TextAIProvider""")
            .findAll(text)
            .map { it.groupValues[1] }
            .toList()
        assertThat(impls).containsExactly("NoAI")
    }

    private fun locate(rel: String): File {
        var dir = File(".").canonicalFile
        repeat(6) {
            val hit = File(dir, rel)
            if (hit.exists()) return hit
            val underApp = File(dir, "app/$rel")
            if (underApp.exists()) return underApp
            dir = dir.parentFile ?: return File(rel)
        }
        return File(rel)
    }
}
