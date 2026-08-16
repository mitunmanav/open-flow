package app.openflow.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleTintTest {

    @Test
    fun charcoal_argb() {
        assertThat(BubbleTint.argb("charcoal")).isEqualTo(0xFF1A1A18.toInt())
    }

    @Test
    fun cream_argb() {
        assertThat(BubbleTint.argb("cream")).isEqualTo(0xFFF4EFE6.toInt())
    }

    @Test
    fun ink_argb() {
        assertThat(BubbleTint.argb("ink")).isEqualTo(0xFF3D5A80.toInt())
    }

    @Test
    fun stone_argb() {
        assertThat(BubbleTint.argb("stone")).isEqualTo(0xFFE8E4DC.toInt())
    }

    @Test
    fun unknown_falls_to_charcoal() {
        assertThat(BubbleTint.argb("neon")).isEqualTo(BubbleTint.argb(BubbleTint.CHARCOAL))
        assertThat(BubbleTint.normalize("")).isEqualTo(BubbleTint.CHARCOAL)
    }

    @Test
    fun light_tints_use_dark_on_color() {
        assertThat(BubbleTint.onArgb("cream")).isEqualTo(0xFF1A1A18.toInt())
        assertThat(BubbleTint.onArgb("stone")).isEqualTo(0xFF1A1A18.toInt())
        assertThat(BubbleTint.onArgb("charcoal")).isEqualTo(0xFFF4EFE6.toInt())
        assertThat(BubbleTint.onArgb("ink")).isEqualTo(0xFFF4EFE6.toInt())
    }

    @Test
    fun extra_tints_normalize_and_contrast() {
        for (id in listOf(BubbleTint.SKY, BubbleTint.FOREST, BubbleTint.CORAL, BubbleTint.GRAPE)) {
            assertThat(BubbleTint.normalize(id)).isEqualTo(id)
            assertThat(BubbleTint.argb(id)).isNotEqualTo(0)
        }
        assertThat(BubbleTint.onArgb(BubbleTint.SKY)).isEqualTo(0xFF1A1A18.toInt())
        assertThat(BubbleTint.onArgb(BubbleTint.FOREST)).isEqualTo(0xFFF4EFE6.toInt())
        assertThat(BubbleTint.onArgb(BubbleTint.CORAL)).isEqualTo(0xFF1A1A18.toInt())
        assertThat(BubbleTint.onArgb(BubbleTint.GRAPE)).isEqualTo(0xFFF4EFE6.toInt())
    }

    @Test
    fun preview_stage_contrasts_cream_and_charcoal() {
        // Cream-on-cream was invisible in Bubble settings live preview.
        assertThat(BubbleTint.previewStageArgb(BubbleTint.CREAM))
            .isEqualTo(BubbleTint.argb(BubbleTint.CHARCOAL))
        assertThat(BubbleTint.previewStageArgb(BubbleTint.STONE))
            .isEqualTo(BubbleTint.argb(BubbleTint.CHARCOAL))
        assertThat(BubbleTint.previewStageArgb(BubbleTint.CHARCOAL))
            .isEqualTo(BubbleTint.argb(BubbleTint.CREAM))
        assertThat(BubbleTint.previewStageArgb(BubbleTint.FOREST))
            .isEqualTo(BubbleTint.argb(BubbleTint.CREAM))
    }
}
