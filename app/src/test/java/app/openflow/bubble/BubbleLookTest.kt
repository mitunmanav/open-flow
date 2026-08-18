package app.openflow.bubble

import app.openflow.ui.theme.BubbleTint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleLookTest {

    @Test
    fun empty_hex_uses_tint_preset() {
        assertThat(BubbleLook.fillArgb("", BubbleTint.SKY))
            .isEqualTo(BubbleTint.argb(BubbleTint.SKY))
        assertThat(BubbleLook.onArgb("", BubbleTint.CREAM))
            .isEqualTo(BubbleTint.onArgb(BubbleTint.CREAM))
    }

    @Test
    fun hex_wins_over_tint() {
        assertThat(BubbleLook.fillArgb("#FF0000", BubbleTint.SKY))
            .isEqualTo(0xFFFF0000.toInt())
        assertThat(BubbleLook.onArgb("#00FF00", BubbleTint.CHARCOAL))
            .isEqualTo(0xFF00FF00.toInt())
    }

    @Test
    fun bad_hex_falls_back_to_tint() {
        assertThat(BubbleLook.fillArgb("zzz", BubbleTint.CORAL))
            .isEqualTo(BubbleTint.argb(BubbleTint.CORAL))
    }
}
