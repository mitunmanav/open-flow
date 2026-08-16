package app.openflow.insights

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InsightSharePayloadTest {
    @Test
    fun text_includes_stats_not_transcript() {
        val t = InsightSharePayload.text(1200, 40, 5, 132.0)
        assertThat(t).contains("1200")
        assertThat(t).contains("40")
        assertThat(t).contains("5")
        assertThat(t).contains("132")
        assertThat(t.lowercase()).doesNotContain("transcript")
    }
}
