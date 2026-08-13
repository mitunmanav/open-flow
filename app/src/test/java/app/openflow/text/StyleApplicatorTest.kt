package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Same raw blob, five styles — presentation only (end punct / informal).
 * Not cleanup. Fillers stay.
 */
class StyleApplicatorTest {

    private val raw = "hey um i think we should ship it"

    @Test
    fun same_raw_five_styles_differ_by_end_punct() {
        val formal = StyleApplicator.apply(raw, WritingStyle.FORMAL)
        val casual = StyleApplicator.apply(raw, WritingStyle.CASUAL)
        val very = StyleApplicator.apply(raw, WritingStyle.VERY_CASUAL)
        val excited = StyleApplicator.apply(raw, WritingStyle.EXCITED)
        val custom = StyleApplicator.apply(
            raw,
            WritingStyle.CUSTOM,
            CustomStyleConfig(endPunct = EndPunct.BANG, caps = CapsMode.NONE)
        )

        // Style is presentation — fillers stay (High cleanup is not this type)
        assertThat(formal.lowercase()).contains("um")
        assertThat(casual.lowercase()).contains("um")

        assertThat(formal.trimEnd().last()).isEqualTo('.')
        assertThat(formal.first().isUpperCase()).isTrue()

        assertThat(casual.first().isUpperCase()).isTrue()
        assertThat(casual.lowercase()).doesNotContain("going to")

        assertThat(very.trimEnd().last()).isNotEqualTo('.')
        assertThat(very.trimEnd().last()).isNotEqualTo('!')

        assertThat(excited.trimEnd().last()).isEqualTo('!')

        assertThat(custom.trimEnd().last()).isEqualTo('!')
        assertThat(custom.first()).isEqualTo('h')

        val outs = listOf(formal, casual, very, excited, custom)
        // End punct / caps make these distinct (casual vs very: both unpunctuated short)
        assertThat(formal).isNotEqualTo(casual)
        assertThat(formal).isNotEqualTo(very)
        assertThat(formal).isNotEqualTo(excited)
        assertThat(formal).isNotEqualTo(custom)
        assertThat(casual).isNotEqualTo(excited)
        assertThat(casual).isNotEqualTo(custom)
        assertThat(very).isNotEqualTo(excited)
        assertThat(very).isNotEqualTo(custom)
        assertThat(excited).isNotEqualTo(custom)
        assertThat(outs).hasSize(5)
    }
}
