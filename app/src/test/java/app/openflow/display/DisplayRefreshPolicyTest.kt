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

    @Test
    fun needs_apply_false_when_already_on_target() {
        assertThat(DisplayRefreshPolicy.needsApply(currentHz = 120f, preferredHz = 120)).isFalse()
        assertThat(DisplayRefreshPolicy.needsApply(currentHz = 119.5f, preferredHz = 120)).isFalse()
    }

    @Test
    fun needs_apply_true_when_far() {
        assertThat(DisplayRefreshPolicy.needsApply(currentHz = 60f, preferredHz = 120)).isTrue()
        assertThat(DisplayRefreshPolicy.needsApply(currentHz = null, preferredHz = 120)).isTrue()
    }

    @Test
    fun needs_apply_false_when_no_pick() {
        assertThat(DisplayRefreshPolicy.needsApply(currentModeId = 1, pick = null)).isFalse()
    }

    @Test
    fun needs_apply_false_when_already_that_mode() {
        val pick = DisplayRefreshPolicy.ModeInfo(3, 120f)
        assertThat(DisplayRefreshPolicy.needsApply(currentModeId = 3, pick = pick)).isFalse()
        assertThat(DisplayRefreshPolicy.needsApply(currentModeId = 1, pick = pick)).isTrue()
    }

    @Test
    fun pick_empty_modes_null() {
        assertThat(DisplayRefreshPolicy.pickMode(emptyList(), 120)).isNull()
    }
}
