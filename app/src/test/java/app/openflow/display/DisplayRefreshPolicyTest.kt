package app.openflow.display

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DisplayRefreshPolicyTest {

    @Test
    fun allowed_set_includes_120() {
        assertThat(DisplayRefreshPolicy.TARGETS_HZ).contains(120)
    }

    @Test
    fun normalize_snaps_to_known() {
        assertThat(DisplayRefreshPolicy.normalizePreference(60)).isEqualTo(60)
        assertThat(DisplayRefreshPolicy.normalizePreference(90)).isEqualTo(90)
        assertThat(DisplayRefreshPolicy.normalizePreference(120)).isEqualTo(120)
        assertThat(DisplayRefreshPolicy.normalizePreference(144)).isEqualTo(144)
        assertThat(DisplayRefreshPolicy.normalizePreference(100)).isEqualTo(90)
        assertThat(DisplayRefreshPolicy.normalizePreference(130)).isEqualTo(120)
    }

    @Test
    fun pick_exact_120() {
        val modes = listOf(
            DisplayRefreshPolicy.ModeInfo(1, 60f),
            DisplayRefreshPolicy.ModeInfo(2, 90f),
            DisplayRefreshPolicy.ModeInfo(3, 120f)
        )
        assertThat(DisplayRefreshPolicy.pickMode(modes, 120)?.modeId).isEqualTo(3)
    }

    @Test
    fun pick_closest_when_missing() {
        val modes = listOf(
            DisplayRefreshPolicy.ModeInfo(1, 60f),
            DisplayRefreshPolicy.ModeInfo(2, 90f)
        )
        assertThat(DisplayRefreshPolicy.pickMode(modes, 144)?.modeId).isEqualTo(2)
    }

    @Test
    fun available_targets_filter() {
        val modes = listOf(
            DisplayRefreshPolicy.ModeInfo(1, 60.0f),
            DisplayRefreshPolicy.ModeInfo(2, 120.0f)
        )
        assertThat(DisplayRefreshPolicy.availableTargets(modes))
            .containsExactly(60, 120).inOrder()
    }
}
