package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrailingPeriodPolicyTest {

    @Test
    fun formal_keeps_period() {
        assertThat(TrailingPeriodPolicy.apply("Sounds good.", WritingStyle.FORMAL, messaging = true))
            .isEqualTo("Sounds good.")
    }

    @Test
    fun very_casual_strips_period_any_app() {
        assertThat(
            TrailingPeriodPolicy.apply("Sounds good.", WritingStyle.VERY_CASUAL, messaging = false)
        ).isEqualTo("Sounds good")
    }

    @Test
    fun casual_strips_short_in_messaging() {
        assertThat(
            TrailingPeriodPolicy.apply("Sounds good.", WritingStyle.CASUAL, messaging = true)
        ).isEqualTo("Sounds good")
        assertThat(
            TrailingPeriodPolicy.apply("Sounds good.", WritingStyle.CASUAL, messaging = false)
        ).isEqualTo("Sounds good.")
    }

    @Test
    fun casual_keeps_long() {
        val long = "This is a longer note that should keep its period because it is not a short chat ping."
        assertThat(TrailingPeriodPolicy.apply(long, WritingStyle.CASUAL, messaging = true))
            .isEqualTo(long)
    }

    @Test
    fun keeps_question_and_bang() {
        assertThat(TrailingPeriodPolicy.apply("Ready?", WritingStyle.CASUAL, messaging = true))
            .isEqualTo("Ready?")
        assertThat(TrailingPeriodPolicy.apply("Go!", WritingStyle.VERY_CASUAL, messaging = true))
            .isEqualTo("Go!")
    }

    @Test
    fun keeps_ellipsis() {
        assertThat(TrailingPeriodPolicy.apply("Wait...", WritingStyle.CASUAL, messaging = true))
            .isEqualTo("Wait...")
    }
}
