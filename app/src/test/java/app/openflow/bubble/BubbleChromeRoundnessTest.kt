package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleChromeRoundnessTest {
    @Test
    fun pct_from_legacy() {
        assertThat(BubbleChrome.pctFromLegacy("hard")).isEqualTo(0)
        assertThat(BubbleChrome.pctFromLegacy("soft")).isEqualTo(50)
        assertThat(BubbleChrome.pctFromLegacy("round")).isEqualTo(100)
        assertThat(BubbleChrome.pctFromLegacy("75")).isEqualTo(75)
        assertThat(BubbleChrome.pctFromLegacy("nope")).isEqualTo(50)
    }

    @Test
    fun circle_stays_round() {
        assertThat(BubbleChrome.cornerPx("circle", 1f, 0)).isEqualTo(999f)
    }

    @Test
    fun pill_lerps() {
        assertThat(BubbleChrome.cornerPx("pill", 1f, 0)).isEqualTo(12f)
        assertThat(BubbleChrome.cornerPx("pill", 1f, 100)).isEqualTo(24f)
    }
}
