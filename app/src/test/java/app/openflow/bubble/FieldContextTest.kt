package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FieldContextTest {

    @Test
    fun on_only_when_brain_rewrites() {
        assertThat(FieldContext.on(brainRewrite = false)).isFalse()
        assertThat(FieldContext.on(brainRewrite = true)).isTrue()
    }

    @Test
    fun surrounding_empty_when_off_or_blank() {
        assertThat(FieldContext.surrounding(on = false, fieldText = "hello")).isEmpty()
        assertThat(FieldContext.surrounding(on = true, fieldText = "  ")).isEmpty()
        assertThat(FieldContext.surrounding(on = true, fieldText = "")).isEmpty()
    }

    @Test
    fun surrounding_keeps_field_text_when_on() {
        assertThat(FieldContext.surrounding(on = true, fieldText = "  hello there  "))
            .isEqualTo("hello there")
    }

    @Test
    fun enhance_input_skips_blank_field() {
        assertThat(FieldContext.enhanceInput("said this", "")).isEqualTo("said this")
        assertThat(FieldContext.enhanceInput("said this", "   ")).isEqualTo("said this")
    }

    @Test
    fun enhance_input_passes_field_then_said() {
        val out = FieldContext.enhanceInput("meet at six", "Hey, can we")
        assertThat(out).contains("Hey, can we")
        assertThat(out).contains("meet at six")
        assertThat(out).isNotEqualTo("meet at six")
    }
}
