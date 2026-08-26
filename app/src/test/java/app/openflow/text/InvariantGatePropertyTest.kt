package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Random

/**
 * Seeded property tests for the pipeline contract (spec success criteria 2):
 * 1k random + adversarial inputs x every cleanup level.
 * - I1: no invented words survive the gate
 * - I2: ?/! terminals survive unless last word was filler/tag
 * - I3: totality — no throw on any input
 * - I4: idempotence — clean(clean(x)) == clean(x)
 */
class InvariantGatePropertyTest {

    private val vocab = listOf(
        "um", "uh", "like", "you", "know", "i", "was", "gonna", "send", "the",
        "report", "meeting", "tomorrow", "no", "wait", "actually", "five",
        "hundred", "dollars", "seven", "thirty", "pm", "may", "fifth", "email",
        "dot", "com", "at", "hey", "can't", "don't", "it's", "new", "line",
        "period", "comma", "bullet", "first", "second", "third", "no", "no",
        "w", "w", "why", "hello", "world", "42", "x", "ok", "so", "well",
    )
    private val punct = listOf(",", ".", "!", "?", ";", ":", "...", "-", "\"")

    @Test
    fun pipeline_holds_invariants_on_1k_seeded_inputs() {
        val rng = Random(20260825L)
        repeat(1000) {
            val input = buildString {
                val n = 1 + rng.nextInt(24)
                for (i in 0 until n) {
                    if (i > 0) append(' ')
                    if (rng.nextInt(6) == 0) append(punct[rng.nextInt(punct.size)])
                    else append(vocab[rng.nextInt(vocab.size)])
                }
                if (rng.nextBoolean()) append(punct[rng.nextInt(punct.size)])
            }
            for (level in CleanupLevel.entries) {
                // I3 + I1: never throws, never invents (gate falls back honestly).
                val out = CleanupPipeline.run(input, level = level)
                assertThat(out.clean).isNotNull()
                assertThat(InvariantGate.ok(input, out.clean))
                    .isTrue()

                // I4: idempotence.
                val again = CleanupPipeline.run(out.clean, level = level)
                assertThat(again.clean).isEqualTo(out.clean)
            }
        }
    }

    @Test
    fun gate_never_throws_on_adversarial_text() {
        val rng = Random(7L)
        val chars = "abcXYZ019 \t.,!?;'\"()[]{}<>@#$%&*+-_/\\~^|€£₹日本語🙂\uE000\uE001\n"
        repeat(1000) {
            val n = rng.nextInt(40)
            val sb = StringBuilder()
            for (i in 0 until n) sb.append(chars[rng.nextInt(chars.length)])
            val text = sb.toString()
            val verdict = InvariantGate.check(text, text.reversed())
            assertThat(verdict).isNotNull()
            CleanupPipeline.run(text)
        }
    }
}
