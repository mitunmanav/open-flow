package app.openflow.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HexColorTest {
    private val cream = 0xFFF4F1EA.toInt()

    @Test
    fun rrggbb_with_hash() {
        assertThat(HexColor.parse("#F4F1EA", 0)).isEqualTo(cream)
    }

    @Test
    fun aarrggbb() {
        assertThat(HexColor.parse("#80F4F1EA", 0)).isEqualTo(0x80F4F1EA.toInt())
    }

    @Test
    fun bad_falls_back() {
        assertThat(HexColor.parse("nope", cream)).isEqualTo(cream)
        assertThat(HexColor.parse("", cream)).isEqualTo(cream)
        assertThat(HexColor.parse("#GGG", cream)).isEqualTo(cream)
    }

    @Test
    fun format_roundtrip() {
        assertThat(HexColor.format(cream)).isEqualTo("#FFF4F1EA")
        assertThat(HexColor.parse(HexColor.format(cream), 0)).isEqualTo(cream)
    }
}
