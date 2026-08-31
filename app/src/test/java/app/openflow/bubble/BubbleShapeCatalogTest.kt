package app.openflow.bubble

import app.openflow.ui.theme.BubbleTint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleShapeCatalogTest {

    @Test
    fun normalize_unknown_is_pill() {
        assertThat(BubbleShapeCatalog.normalize("neon")).isEqualTo("pill")
        assertThat(BubbleShapeCatalog.normalize("")).isEqualTo("pill")
    }

    @Test
    fun all_new_pill_variants_exist() {
        assertThat(BubbleShapeCatalog.ids())
            .containsAtLeast("pill", "slim", "chunk", "stadium", "circle", "square", "dot")
    }

    @Test
    fun slim_is_wider_and_shorter_than_pill() {
        val pill = BubbleShapeSpec.PILL
        val slim = BubbleShapeSpec.SLIM
        assertThat(slim.idleWidthDp).isGreaterThan(pill.idleWidthDp)
        assertThat(slim.idleHeightDp).isLessThan(pill.idleHeightDp)
    }

    @Test
    fun stadium_corners_near_full_round() {
        val d = 2f
        assertThat(BubbleShapeSpec.STADIUM.cornerPx(d, 100))
            .isEqualTo(48f * d)
    }

    @Test
    fun overlaySize_uses_catalog() {
        val (w, h) = BubbleGeometry.overlaySizePx(false, 2f, "slim")
        assertThat(w).isEqualTo((112f * 2f).toInt())
        assertThat(h).isEqualTo((36f * 2f).toInt())
    }

    @Test
    fun tint_pulse_keeps_on_rgb() {
        val on = BubbleTint.onArgb(BubbleTint.CREAM)
        val pulse = BubbleChrome.pulseArgb(on)
        assertThat(pulse and 0x00FFFFFF).isEqualTo(on and 0x00FFFFFF)
        assertThat((pulse ushr 24) and 0xFF).isEqualTo(0x33)
    }

    @Test
    fun every_tint_has_contrasting_on() {
        BubbleTint.ALL.forEach { s ->
            assertThat(s.fillArgb).isNotEqualTo(s.onArgb)
        }
    }
}
