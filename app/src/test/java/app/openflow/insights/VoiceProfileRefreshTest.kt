package app.openflow.insights

import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.TimeZone

class VoiceProfileRefreshTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun locked_under_2000() = runTest {
        val r = VoiceProfileRefresh.run(
            sessions = emptyList(),
            totalWords = 1999,
            streakDays = 0,
            zone = utc,
            brain = FakeBrain("""{"archetype":"A","catchphrase":"B","headline":"C"}"""),
            providerName = "fake",
        )
        assertThat(r.isFailure).isTrue()
    }

    @Test
    fun no_ai_fails() = runTest {
        val r = VoiceProfileRefresh.run(
            sessions = emptyList(),
            totalWords = 2000,
            streakDays = 1,
            zone = utc,
            brain = NoAI,
            providerName = "none",
        )
        assertThat(r.isFailure).isTrue()
        assertThat(r.exceptionOrNull()?.message).contains("brain key")
    }

    @Test
    fun success_parses_json() = runTest {
        val r = VoiceProfileRefresh.run(
            sessions = listOf(
                InsightSession("flow flow cool", "", 0L, 60_000, 3, "com.app"),
            ),
            totalWords = 2500,
            streakDays = 2,
            zone = utc,
            brain = FakeBrain("""{"archetype":"Coder","catchphrase":"ship it","headline":"Night owl"}"""),
            providerName = "fake",
        )
        assertThat(r.getOrNull()).isEqualTo(VoiceFlavor("Coder", "ship it", "Night owl"))
    }

    @Test
    fun garbage_fails() = runTest {
        val r = VoiceProfileRefresh.run(
            sessions = emptyList(),
            totalWords = 2500,
            streakDays = 1,
            zone = utc,
            brain = FakeBrain("not json"),
            providerName = "fake",
        )
        assertThat(r.isFailure).isTrue()
    }

    private class FakeBrain(private val out: String) : TextAIProvider {
        override val name: String = "fake"
        override suspend fun enhance(text: String, mode: String): String = out
    }
}
