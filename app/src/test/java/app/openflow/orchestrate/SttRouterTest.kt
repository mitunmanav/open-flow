package app.openflow.orchestrate

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SttRouterTest {

    private val offline = RouteSignals(online = false, keyedEars = emptySet(), keyedBrains = emptySet())
    private val onlineOpenAi =
        RouteSignals(online = true, keyedEars = setOf("openai"), keyedBrains = emptySet())

    @Test
    fun manual_ignores_signals() {
        val result =
            SttRouter.pick(
                auto = false,
                manualEarId = "openai",
                signals = offline,
                health = ProviderHealth(),
            )
        assertThat(result).isEqualTo(RouteExplain("openai", "manual"))
    }

    @Test
    fun auto_prefer_on_device_still_local_first() {
        val signals =
            RouteSignals(
                online = true,
                keyedEars = setOf("openai"),
                keyedBrains = emptySet(),
                preferOnDevice = true,
            )
        val result =
            SttRouter.pick(
                auto = true,
                manualEarId = "system",
                signals = signals,
                health = ProviderHealth(),
            )
        assertThat(result).isEqualTo(RouteExplain("system", "local-first"))
    }

    @Test
    fun auto_offline_picks_system() {
        val result =
            SttRouter.pick(
                auto = true,
                manualEarId = "system",
                signals = offline,
                health = ProviderHealth(),
            )
        assertThat(result.providerId).isEqualTo("system")
    }

    @Test
    fun auto_online_with_openai_key_still_prefers_system() {
        val result =
            SttRouter.pick(
                auto = true,
                manualEarId = "system",
                signals = onlineOpenAi,
                health = ProviderHealth(),
            )
        assertThat(result).isEqualTo(RouteExplain("system", "local-first"))
    }

    @Test
    fun auto_prefer_on_device_never_picks_whisper_stub() {
        val health = ProviderHealth(failThreshold = 3)
        repeat(3) { health.recordFailure("system") }
        val signals =
            RouteSignals(
                online = true,
                keyedEars = setOf("openai"),
                keyedBrains = emptySet(),
                preferOnDevice = true,
            )
        val result =
            SttRouter.pick(
                auto = true,
                manualEarId = "system",
                signals = signals,
                health = health,
            )
        assertThat(result.providerId).isNotEqualTo("on_phone")
        assertThat(result).isEqualTo(RouteExplain("openai", "cloud-keyed"))
    }

    @Test
    fun auto_system_unavailable_openai_keyed_picks_openai() {
        val health = ProviderHealth(failThreshold = 3)
        repeat(3) { health.recordFailure("system") }
        val result =
            SttRouter.pick(
                auto = true,
                manualEarId = "system",
                signals = onlineOpenAi,
                health = health,
            )
        assertThat(result).isEqualTo(RouteExplain("openai", "cloud-keyed"))
    }

    @Test
    fun pick_is_single_call_no_retry() {
        val health = ProviderHealth()
        val first =
            SttRouter.pick(
                auto = true,
                manualEarId = "system",
                signals = onlineOpenAi,
                health = health,
            )
        val second =
            SttRouter.pick(
                auto = true,
                manualEarId = "system",
                signals = onlineOpenAi,
                health = health,
            )
        assertThat(first).isEqualTo(second)
    }
}
