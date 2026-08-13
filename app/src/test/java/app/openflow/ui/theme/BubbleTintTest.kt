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
}
