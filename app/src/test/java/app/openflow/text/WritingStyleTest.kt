package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WritingStyleTest {

    private val body = "we are gonna ship this soon"
    /** Long enough for casual AUTO period; multi-clause for soft vs sentence caps. */
    private val rich = "we are gonna leave now. then we ship this soon and i hope it works well today"

    @Test
    fun formal_expands_informal_and_periods() {
        val out = StyleApplicator.apply(body, WritingStyle.FORMAL)
        assertThat(out.lowercase()).contains("going to")
        assertThat(out.lowercase()).doesNotContain("gonna")
        assertThat(out.trimEnd().last()).isEqualTo('.')
        assertThat(out.first().isUpperCase()).isTrue()
    }

    @Test
    fun formal_forces_period_over_bang() {
        val out = StyleApplicator.apply("we shipped it!", WritingStyle.FORMAL)
        assertThat(out.trimEnd().last()).isEqualTo('.')
    }

    @Test
    fun casual_sentence_caps_and_auto_period_when_long() {
        val short = StyleApplicator.apply(body, WritingStyle.CASUAL)
        assertThat(short.first().isUpperCase()).isTrue()
        assertThat(short.lowercase()).contains("gonna")
        assertThat(short.trimEnd().last()).isNotEqualTo('.')

        val long = StyleApplicator.apply(
            "this is a somewhat longer casual sentence that should get a period",
            WritingStyle.CASUAL
        )
        assertThat(long.trimEnd().last()).isEqualTo('.')
        assertThat(long.first().isUpperCase()).isTrue()
    }

    @Test
    fun very_casual_soft_caps_no_forced_period() {
        val short = StyleApplicator.apply("hey there", WritingStyle.VERY_CASUAL)
        assertThat(short).doesNotContain(".")
        assertThat(short.first().isUpperCase()).isTrue()

        val multi = StyleApplicator.apply(
            "we left now. then we came back",
            WritingStyle.VERY_CASUAL
        )
        // Soft caps = first char only; second sentence stays lower
        assertThat(multi.first()).isEqualTo('W')
        assertThat(multi).contains(". then ")
        assertThat(multi.trimEnd().last()).isNotEqualTo('.')
        assertThat(multi.trimEnd().last()).isNotEqualTo('!')
    }

    @Test
    fun excited_prefers_bang() {
        val out = StyleApplicator.apply("we shipped it", WritingStyle.EXCITED)
        assertThat(out.trimEnd().last()).isEqualTo('!')

        val fromPeriod = StyleApplicator.apply("we shipped it.", WritingStyle.EXCITED)
        assertThat(fromPeriod.trimEnd().last()).isEqualTo('!')
    }

    @Test
    fun casual_differs_from_formal() {
        val casual = StyleApplicator.apply(body, WritingStyle.CASUAL)
        val formal = StyleApplicator.apply(body, WritingStyle.FORMAL)
        assertThat(casual).isNotEqualTo(formal)
        assertThat(casual.lowercase()).contains("gonna")
        assertThat(formal.lowercase()).contains("going to")
    }

    @Test
    fun all_built_in_styles_differ_on_same_input() {
        val formal = StyleApplicator.apply(rich, WritingStyle.FORMAL)
        val casual = StyleApplicator.apply(rich, WritingStyle.CASUAL)
        val very = StyleApplicator.apply(rich, WritingStyle.VERY_CASUAL)
        val excited = StyleApplicator.apply(rich, WritingStyle.EXCITED)

        // Pairwise distinct — no style is a cheat alias of another
        val outs = listOf(formal, casual, very, excited)
        assertThat(outs.toSet()).hasSize(4)

        assertThat(formal.lowercase()).contains("going to")
        assertThat(formal.trimEnd().last()).isEqualTo('.')
        assertThat(formal).contains(". Then ") // sentence caps

        assertThat(casual.lowercase()).contains("gonna")
        assertThat(casual).contains(". Then ")
        assertThat(casual.trimEnd().last()).isEqualTo('.') // long → auto period

        assertThat(very.lowercase()).contains("gonna")
        assertThat(very).contains(". then ") // soft caps, no second-sentence cap
        assertThat(very.trimEnd().last()).isNotEqualTo('.')
        assertThat(very.trimEnd().last()).isNotEqualTo('!')

        assertThat(excited.trimEnd().last()).isEqualTo('!')
        assertThat(excited.lowercase()).contains("gonna")
    }

    @Test
    fun style_does_not_do_high_cleanup() {
        // Fillers / hedges stay — that's CleanupPipeline HIGH, not style
        val raw = "um we are gonna ship this basically soon"
        val styled = StyleApplicator.apply(raw, WritingStyle.FORMAL)
        assertThat(styled.lowercase()).contains("um")
        assertThat(styled.lowercase()).contains("basically")
        assertThat(styled.lowercase()).contains("going to")
    }

    @Test
    fun custom_user_replacements() {
        val custom = CustomStyleConfig(
            endPunct = EndPunct.PERIOD,
            caps = CapsMode.SENTENCE,
            expandInformal = false,
            replacements = listOf("cheers" to "Thanks", "yeah" to "yes")
        )
        val out = StyleApplicator.apply("yeah cheers everyone", WritingStyle.CUSTOM, custom)
        assertThat(out.lowercase()).contains("yes")
        assertThat(out.lowercase()).contains("thanks")
        assertThat(out.lowercase()).doesNotContain("yeah")
        assertThat(out.lowercase()).doesNotContain("cheers")
        assertThat(out.trimEnd().last()).isEqualTo('.')
    }

    @Test
    fun custom_expand_informal_optional() {
        val off = CustomStyleConfig(expandInformal = false, endPunct = EndPunct.NONE)
        val on = CustomStyleConfig(expandInformal = true, endPunct = EndPunct.NONE)
        assertThat(StyleApplicator.apply(body, WritingStyle.CUSTOM, off).lowercase())
            .contains("gonna")
        assertThat(StyleApplicator.apply(body, WritingStyle.CUSTOM, on).lowercase())
            .contains("going to")
    }

    @Test
    fun custom_none_end_punct() {
        val custom = CustomStyleConfig(endPunct = EndPunct.NONE, caps = CapsMode.FIRST)
        val out = StyleApplicator.apply("hello world", WritingStyle.CUSTOM, custom)
        assertThat(out).isEqualTo("Hello world")
    }

    @Test
    fun custom_bang_and_caps_none() {
        val custom = CustomStyleConfig(
            endPunct = EndPunct.BANG,
            caps = CapsMode.NONE,
            expandInformal = false
        )
        val out = StyleApplicator.apply("ship it now", WritingStyle.CUSTOM, custom)
        assertThat(out).isEqualTo("ship it now!")
    }

    @Test
    fun parse_replacements_lines() {
        val blob = """
            # comment
            gonna=>going to
            wanna=want to
        """.trimIndent()
        val pairs = CustomStyleConfig.parseReplacements(blob)
        assertThat(pairs).containsExactly(
            "gonna" to "going to",
            "wanna" to "want to"
        )
    }

    @Test
    fun styles_change_same_cleanup_differently() {
        val input = "um we are gonna meet at 4:30 actually 5:30"
        val cleaned = CleanupPipeline.run(
            input,
            CleanupLevel.NORMAL,
            WritingStyle.CASUAL
        ).clean
        val fullFormal = CleanupPipeline.run(
            input,
            CleanupLevel.NORMAL,
            WritingStyle.FORMAL
        ).clean
        assertThat(fullFormal.lowercase()).contains("5:30")
        assertThat(fullFormal.lowercase()).contains("going to")
        assertThat(cleaned.lowercase()).contains("gonna")
        assertThat(fullFormal).isNotEqualTo(cleaned)
    }
}
