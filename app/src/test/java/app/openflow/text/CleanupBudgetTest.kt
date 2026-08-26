package app.openflow.text

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CleanupBudgetTest {

    @Test
    fun budget_is_positive_and_bounded() {
        assertThat(CleanupBudget.POLISH_MS).isAtLeast(1_000L)
        assertThat(CleanupBudget.POLISH_MS).isAtMost(10_000L)
    }

    @Test
    fun fallback_keeps_raw_verbatim_trimmed() {
        val fb = CleanupBudget.fallback("  um raw text here  ")
        assertThat(fb!!.raw).isEqualTo("um raw text here")
        assertThat(fb.clean).isEqualTo("um raw text here")
    }

    @Test
    fun fallback_null_when_nothing_to_insert() {
        assertThat(CleanupBudget.fallback("")).isNull()
        assertThat(CleanupBudget.fallback("   \n\t ")).isNull()
    }

    @Test
    fun fallback_never_throws_on_adversarial_input() {
        val nasty = listOf("\u0000", "\uD800", "a".repeat(100_000), "﻿﻿")
        for (s in nasty) {
            val fb = CleanupBudget.fallback(s)
            if (s.isBlank()) assertThat(fb).isNull() else assertThat(fb).isNotNull()
        }
    }

    @Test
    fun within_returns_value_when_fast() = runBlocking {
        val r = CleanupBudget.within(500) {
            delay(10)
            "ok"
        }
        assertThat(r).isEqualTo("ok")
    }

    @Test
    fun within_returns_null_on_timeout() = runBlocking {
        val r = CleanupBudget.within(50) {
            delay(500)
            "late"
        }
        assertThat(r).isNull()
    }

    @Test
    fun local_500_words_under_50ms() {
        val text = (1..500).joinToString(" ") { "word$it" }
        repeat(2) { CleanupPipeline.run(text) }
        val t0 = System.nanoTime()
        CleanupPipeline.run(text)
        val ms = (System.nanoTime() - t0) / 1_000_000.0
        assertThat(ms).isLessThan(50.0)
    }
}
