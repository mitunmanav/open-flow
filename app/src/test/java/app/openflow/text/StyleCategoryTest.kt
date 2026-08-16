package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StyleCategoryTest {
    @Test
    fun defaults_match_wispr_android() {
        assertThat(StyleCategory.PERSONAL.defaultStyle).isEqualTo(WritingStyle.CASUAL)
        assertThat(StyleCategory.WORK.defaultStyle).isEqualTo(WritingStyle.FORMAL)
        assertThat(StyleCategory.EMAIL.defaultStyle).isEqualTo(WritingStyle.FORMAL)
        assertThat(StyleCategory.OTHER.defaultStyle).isEqualTo(WritingStyle.FORMAL)
    }

    @Test
    fun allow_list_very_casual_personal_only() {
        assertThat(StyleCategory.PERSONAL.allows(WritingStyle.VERY_CASUAL)).isTrue()
        assertThat(StyleCategory.WORK.allows(WritingStyle.VERY_CASUAL)).isFalse()
        assertThat(StyleCategory.EMAIL.allows(WritingStyle.VERY_CASUAL)).isFalse()
        assertThat(StyleCategory.OTHER.allows(WritingStyle.VERY_CASUAL)).isFalse()
    }

    @Test
    fun allow_list_excited_not_personal() {
        assertThat(StyleCategory.PERSONAL.allows(WritingStyle.EXCITED)).isFalse()
        assertThat(StyleCategory.WORK.allows(WritingStyle.EXCITED)).isTrue()
        assertThat(StyleCategory.EMAIL.allows(WritingStyle.EXCITED)).isTrue()
        assertThat(StyleCategory.OTHER.allows(WritingStyle.EXCITED)).isTrue()
    }

    @Test
    fun coerce_falls_back_to_default_when_disallowed() {
        assertThat(StyleCategory.PERSONAL.coerce(WritingStyle.EXCITED))
            .isEqualTo(WritingStyle.CASUAL)
        assertThat(StyleCategory.WORK.coerce(WritingStyle.VERY_CASUAL))
            .isEqualTo(WritingStyle.FORMAL)
        assertThat(StyleCategory.EMAIL.coerce(WritingStyle.FORMAL))
            .isEqualTo(WritingStyle.FORMAL)
    }
}
