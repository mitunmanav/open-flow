package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FieldContextTest {

    @Test
    fun on_always_for_local_context() {
        assertThat(FieldContext.on(brainRewrite = false)).isTrue()
        assertThat(FieldContext.on(brainRewrite = true)).isTrue()
    }

    @Test
    fun continue_lowers_first_when_field_open() {
        assertThat(FieldContext.continueSpoken("Hey can we", "Meet at six"))
            .isEqualTo("meet at six")
    }

    @Test
    fun continue_keeps_cap_after_sentence_end() {
        assertThat(FieldContext.continueSpoken("Hello.", "Meet at six"))
            .isEqualTo("Meet at six")
    }

    @Test
    fun after_polish_course_corrects_across_field() {
        val out = FieldContext.afterPolish("meet at 5 pm", "actually 6 pm")
        assertThat(out.lowercase()).contains("6")
        assertThat(out.lowercase()).doesNotContain("5")
    }

    @Test
    fun after_polish_empty_prefix_is_spoken() {
        assertThat(FieldContext.afterPolish("", "hello there")).isEqualTo("hello there")
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
