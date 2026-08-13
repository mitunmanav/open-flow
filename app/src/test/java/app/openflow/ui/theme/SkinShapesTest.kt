package app.openflow.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SkinShapesTest {

    @Test
    fun default_skin_is_brutal() {
        assertThat(VisualSkin.DEFAULT).isEqualTo(VisualSkin.BRUTAL)
    }

    @Test
    fun brutal_medium_is_hard_zero_dp() {
        // Cards use MaterialTheme.shapes.medium. Soft leftover 2dp still clips.
        assertThat(SkinShapes.cornerDp(VisualSkin.BRUTAL, SkinShapes.Slot.MEDIUM))
            .isEqualTo(0)
        assertThat(SkinShapes.isHard(VisualSkin.BRUTAL)).isTrue()
        assertThat(SkinShapes.cornerDp(VisualSkin.DEFAULT, SkinShapes.Slot.MEDIUM))
            .isEqualTo(0)
    }

    @Test
    fun brutal_all_slots_hard() {
        SkinShapes.Slot.entries.forEach { slot ->
            assertThat(SkinShapes.cornerDp(VisualSkin.BRUTAL, slot)).isEqualTo(0)
        }
    }

    @Test
    fun m3_medium_stays_rounded_opt_in() {
        assertThat(SkinShapes.cornerDp(VisualSkin.M3, SkinShapes.Slot.MEDIUM))
            .isEqualTo(16)
        assertThat(SkinShapes.isHard(VisualSkin.M3)).isFalse()
        assertThat(SkinShapes.cornerDp(VisualSkin.M3, SkinShapes.Slot.EXTRA_SMALL))
            .isEqualTo(8)
        assertThat(SkinShapes.cornerDp(VisualSkin.M3, SkinShapes.Slot.SMALL))
            .isEqualTo(12)
        assertThat(SkinShapes.cornerDp(VisualSkin.M3, SkinShapes.Slot.LARGE))
            .isEqualTo(20)
        assertThat(SkinShapes.cornerDp(VisualSkin.M3, SkinShapes.Slot.EXTRA_LARGE))
            .isEqualTo(28)
    }

    @Test
    fun from_storage_soft_aliases_are_m3_only() {
        assertThat(VisualSkin.fromStorage("m3")).isEqualTo(VisualSkin.M3)
        assertThat(VisualSkin.fromStorage("soft")).isEqualTo(VisualSkin.M3)
        assertThat(VisualSkin.fromStorage("material3")).isEqualTo(VisualSkin.M3)
        assertThat(VisualSkin.fromStorage("brutal")).isEqualTo(VisualSkin.BRUTAL)
        assertThat(VisualSkin.fromStorage("weird")).isEqualTo(VisualSkin.DEFAULT)
    }
}
