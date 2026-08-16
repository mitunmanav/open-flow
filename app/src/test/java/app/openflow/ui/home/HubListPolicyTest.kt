package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HubListPolicyTest {

    @Test
    fun blank_query_keeps_all() {
        assertThat(HubListPolicy.matches("", "wispr", "flow")).isTrue()
        assertThat(HubListPolicy.matches("   ", "x")).isTrue()
    }

    @Test
    fun query_matches_any_field_ignore_case() {
        assertThat(HubListPolicy.matches("Wisp", "wispr", "flow")).isTrue()
        assertThat(HubListPolicy.matches("FLOW", "wispr", "flow")).isTrue()
        assertThat(HubListPolicy.matches("zzz", "wispr", "flow")).isFalse()
    }

    @Test
    fun filter_keeps_matching_rows() {
        val rows = listOf("alpha" to "A", "beta" to "B", "alpine" to "C")
        val out = HubListPolicy.filterPairs(rows, "alp")
        assertThat(out.map { it.first }).containsExactly("alpha", "alpine").inOrder()
    }
}
