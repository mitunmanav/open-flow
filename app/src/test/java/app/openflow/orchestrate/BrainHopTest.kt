package app.openflow.orchestrate

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BrainHopTest {

    @Test
    fun local_only_ignores_key() {
        val ask = BrainHopAsk(
            mode = RouteMode.LOCAL_ONLY,
            aiWhen = AiWhen.EVERY,
            brainId = "openai",
            signals = RouteSignals(true, emptySet(), setOf("openai")),
            looksLikeCommand = false,
            textLen = 80,
            cleaned = "x".repeat(80),
            levelRaw = false,
        )
        assertThat(BrainHop.pick(ask).providerId).isEqualTo("none")
    }

    @Test
    fun local_then_uses_user_brain_not_other_keyed() {
        val ask = BrainHopAsk(
            mode = RouteMode.LOCAL_THEN_AI,
            aiWhen = AiWhen.EVERY,
            brainId = "anthropic",
            signals = RouteSignals(true, emptySet(), setOf("openai", "anthropic")),
            looksLikeCommand = false,
            textLen = 80,
            cleaned = "x".repeat(80),
            levelRaw = false,
        )
        assertThat(BrainHop.pick(ask)).isEqualTo(RouteExplain("anthropic", "user-brain"))
    }

    @Test
    fun miss_only_skips_when_clean() {
        val ask = BrainHopAsk(
            mode = RouteMode.LOCAL_THEN_AI,
            aiWhen = AiWhen.MISS_ONLY,
            brainId = "openai",
            signals = RouteSignals(true, emptySet(), setOf("openai")),
            looksLikeCommand = false,
            textLen = 80,
            cleaned = "Short clean sentence.",
            levelRaw = false,
        )
        assertThat(BrainHop.pick(ask).reason).isEqualTo("miss-skip")
    }

    @Test
    fun miss_true_on_two_signals() {
        val t = "um " + "word ".repeat(40) + "and then so and then so and then so"
        assertThat(BrainHop.miss(t)).isTrue()
    }

    @Test
    fun ai_first_offline_none() {
        val ask = BrainHopAsk(
            mode = RouteMode.AI_FIRST,
            aiWhen = AiWhen.EVERY,
            brainId = "openai",
            signals = RouteSignals(false, emptySet(), setOf("openai")),
            looksLikeCommand = false,
            textLen = 80,
            cleaned = "hello",
            levelRaw = false,
        )
        assertThat(BrainHop.pick(ask).providerId).isEqualTo("none")
    }

    @Test
    fun from_legacy_off_none_is_local_only() {
        assertThat(RouteMode.fromLegacy(false, "none")).isEqualTo(RouteMode.LOCAL_ONLY)
    }
}
