package app.openflow.orchestrate

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BrainRouterTest {

    private val offline =
        RouteSignals(online = false, keyedEars = emptySet(), keyedBrains = emptySet())
    private val onlineOpenAiBrain =
        RouteSignals(
            online = true,
            keyedEars = setOf("openai"),
            keyedBrains = setOf("openai"),
        )
    private val onlineEarOnlyOpenAi =
        RouteSignals(
            online = true,
            keyedEars = setOf("openai"),
            keyedBrains = emptySet(),
        )

    @Test
    fun command_returns_none_even_in_auto() {
        val result =
            BrainRouter.pick(
                auto = true,
                manualBrainId = "openai",
                signals = onlineOpenAiBrain,
                health = ProviderHealth(),
                looksLikeCommand = true,
                textLen = 100,
            )
        assertThat(result).isEqualTo(RouteExplain("none", "command-local"))
    }

    @Test
    fun command_returns_none_even_in_manual() {
        val result =
            BrainRouter.pick(
                auto = false,
                manualBrainId = "openai",
                signals = onlineOpenAiBrain,
                health = ProviderHealth(),
                looksLikeCommand = true,
                textLen = 100,
            )
        assertThat(result).isEqualTo(RouteExplain("none", "command-local"))
    }

    @Test
    fun manual_ignores_signals() {
        val result =
            BrainRouter.pick(
                auto = false,
                manualBrainId = "openai",
                signals = offline,
                health = ProviderHealth(),
                looksLikeCommand = false,
                textLen = 100,
            )
        assertThat(result).isEqualTo(RouteExplain("none", "local-only"))
    }

    @Test
    fun auto_short_text_skips_brain() {
        val result =
            BrainRouter.pick(
                auto = true,
                manualBrainId = "none",
                signals = onlineOpenAiBrain,
                health = ProviderHealth(),
                looksLikeCommand = false,
                textLen = 39,
            )
        assertThat(result).isEqualTo(RouteExplain("none", "short-skip-brain"))
    }

    @Test
    fun auto_long_text_picks_first_keyed_cloud() {
        val result =
            BrainRouter.pick(
                auto = true,
                manualBrainId = "openai",
                signals = onlineOpenAiBrain,
                health = ProviderHealth(),
                looksLikeCommand = false,
                textLen = 40,
            )
        assertThat(result).isEqualTo(RouteExplain("openai", "user-brain"))
    }

    @Test
    fun auto_long_offline_falls_back_to_none() {
        val result =
            BrainRouter.pick(
                auto = true,
                manualBrainId = "none",
                signals = onlineOpenAiBrain.copy(online = false),
                health = ProviderHealth(),
                looksLikeCommand = false,
                textLen = 50,
            )
        assertThat(result).isEqualTo(RouteExplain("none", "fallback-none"))
    }

    @Test
    fun auto_long_unhealthy_cloud_skips_to_next() {
        val health = ProviderHealth(failThreshold = 3)
        repeat(3) { health.recordFailure("openai") }
        val signals =
            RouteSignals(
                online = true,
                keyedEars = emptySet(),
                keyedBrains = setOf("openai", "anthropic"),
            )
        val result =
            BrainRouter.pick(
                auto = true,
                manualBrainId = "openai",
                signals = signals,
                health = health,
                looksLikeCommand = false,
                textLen = 50,
            )
        assertThat(result).isEqualTo(RouteExplain("openai", "user-brain"))
    }

    @Test
    fun auto_long_no_keyed_brain_falls_back_to_none() {
        val result =
            BrainRouter.pick(
                auto = true,
                manualBrainId = "none",
                signals = offline,
                health = ProviderHealth(),
                looksLikeCommand = false,
                textLen = 50,
            )
        assertThat(result).isEqualTo(RouteExplain("none", "fallback-none"))
    }

    @Test
    fun keyed_ears_cannot_force_brain() {
        val result =
            BrainRouter.pick(
                auto = true,
                manualBrainId = "none",
                signals = onlineEarOnlyOpenAi,
                health = ProviderHealth(),
                looksLikeCommand = false,
                textLen = 50,
            )
        assertThat(result).isEqualTo(RouteExplain("none", "fallback-none"))
    }

    @Test
    fun never_selects_cloud_without_brain_key() {
        val signals =
            RouteSignals(
                online = true,
                keyedEars = setOf("openai", "anthropic"),
                keyedBrains = setOf("anthropic"),
            )
        val result =
            BrainRouter.pick(
                auto = true,
                manualBrainId = "anthropic",
                signals = signals,
                health = ProviderHealth(),
                looksLikeCommand = false,
                textLen = 50,
            )
        assertThat(result).isEqualTo(RouteExplain("anthropic", "user-brain"))
        assertThat(result.providerId).isNotEqualTo("openai")
    }
}
