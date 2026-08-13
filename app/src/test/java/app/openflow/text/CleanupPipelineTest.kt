package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CleanupPipelineTest {

    private val sample =
        "I, uh, basically think that we should meet at 4:30 actually 5:30 and stuff"

    @Test
    fun amount_actually_replaces_number() {
        val r = CleanupPipeline.run("The amount is 430, actually 530.")
        assertThat(r.clean.lowercase()).contains("530")
        assertThat(r.clean.lowercase()).doesNotContain("430")
        assertThat(r.clean.lowercase()).doesNotContain("actually")
        assertThat(r.raw).contains("430")
        assertThat(r.corrections).isNotEmpty()
    }

    @Test
    fun time_actually_replaces() {
        val r = CleanupPipeline.run("The meeting is at 4:30, actually 5:30.")
        assertThat(r.clean.lowercase()).contains("5:30")
        assertThat(r.clean.lowercase()).doesNotContain("4:30")
    }

    @Test
    fun date_no_replaces() {
        val r = CleanupPipeline.run("The deadline is August 12th, no, August 15th.")
        assertThat(r.clean.lowercase()).contains("15")
        assertThat(r.clean.lowercase()).doesNotContain("12")
    }

    @Test
    fun send_to_john_no_james() {
        val r = CleanupPipeline.run("Send it to John. No, send it to James.")
        assertThat(r.clean.lowercase()).contains("james")
        assertThat(r.clean.lowercase()).doesNotContain("john")
    }

    @Test
    fun fillers_gone_like_kept() {
        val r = CleanupPipeline.run("I, uh, like, um, pizza.")
        assertThat(r.clean.lowercase()).contains("like")
        assertThat(r.clean.lowercase()).contains("pizza")
        assertThat(r.clean.lowercase()).doesNotContain("uh")
        assertThat(r.clean.lowercase()).doesNotContain("um")
    }

    @Test
    fun real_like_kept() {
        val r = CleanupPipeline.run("I like pizza.")
        assertThat(r.clean.lowercase()).contains("i like pizza")
    }

    @Test
    fun repeated_words_collapsed() {
        val r = CleanupPipeline.run("I I want to go")
        assertThat(r.clean.lowercase()).contains("i want")
        assertThat(r.clean.lowercase()).doesNotContain("i i")
    }

    @Test
    fun raw_level_skips_cleanup() {
        val r = CleanupPipeline.run("I, uh, actually want pizza", CleanupLevel.RAW)
        assertThat(r.clean).contains("uh")
        assertThat(r.clean).contains("actually")
        assertThat(r.clean).isEqualTo(r.raw)
    }

    @Test
    fun light_skips_course_correct() {
        val r = CleanupPipeline.run("amount is 430 actually 530", CleanupLevel.LIGHT)
        assertThat(r.clean.lowercase()).contains("430")
    }

    @Test
    fun light_strips_uh_keeps_actually_number() {
        val r = CleanupPipeline.run(
            "I, uh, the amount is 430 actually 530",
            CleanupLevel.LIGHT
        )
        val clean = r.clean.lowercase()
        assertThat(clean).doesNotContain("uh")
        assertThat(clean).contains("430")
        assertThat(clean).contains("actually")
        assertThat(clean).contains("530")
    }

    @Test
    fun medium_is_light_plus_course_correct() {
        val r = CleanupPipeline.run(
            "I, uh, the amount is 430 actually 530",
            CleanupLevel.NORMAL
        )
        val clean = r.clean.lowercase()
        assertThat(clean).doesNotContain("uh")
        assertThat(clean).contains("530")
        assertThat(clean).doesNotContain("430")
        assertThat(clean).doesNotContain("actually")
    }

    @Test
    fun blank_safe() {
        val r = CleanupPipeline.run("   ")
        assertThat(r.clean).isEmpty()
        assertThat(r.raw).isEmpty()
    }

    /** Four levels must produce four different outcomes on the same speech. */
    @Test
    fun four_levels_properly_differ() {
        val none = CleanupPipeline.run(sample, CleanupLevel.RAW).clean
        val light = CleanupPipeline.run(sample, CleanupLevel.LIGHT).clean
        val medium = CleanupPipeline.run(sample, CleanupLevel.NORMAL).clean
        val high = CleanupPipeline.run(sample, CleanupLevel.HIGH).clean

        assertThat(none).isEqualTo(sample)

        // Light: fillers gone, no course-correct
        assertThat(light.lowercase()).doesNotContain("uh")
        assertThat(light.lowercase()).contains("4:30")
        assertThat(light.lowercase()).contains("basically")

        // Medium: course-correct time
        assertThat(medium.lowercase()).contains("5:30")
        assertThat(medium.lowercase()).doesNotContain("4:30")
        assertThat(medium.lowercase()).doesNotContain("actually")
        assertThat(medium).isNotEqualTo(light)

        // High: + brevity (hedges / wordiness)
        assertThat(high).isNotEqualTo(medium)
        assertThat(high.lowercase()).doesNotContain("basically")
        assertThat(high.lowercase()).doesNotContain("and stuff")
        assertThat(high.lowercase()).doesNotContain("i think that")
        assertThat(high.lowercase()).contains("5:30")
    }

    @Test
    fun medium_keeps_hedges_high_does_not() {
        val raw = "Basically we need clarity"
        val med = CleanupPipeline.run(raw, CleanupLevel.NORMAL).clean.lowercase()
        val high = CleanupPipeline.run(raw, CleanupLevel.HIGH).clean.lowercase()
        assertThat(med).contains("basically")
        assertThat(high).doesNotContain("basically")
        assertThat(high).isNotEqualTo(med)
    }

    @Test
    fun high_does_not_force_formal_over_style() {
        // Excited style must survive High (level must not steal style)
        val r = CleanupPipeline.run(
            "this is a short excited line",
            CleanupLevel.HIGH,
            WritingStyle.EXCITED
        )
        assertThat(r.clean.trimEnd().last()).isEqualTo('!')
    }

    @Test
    fun high_formal_still_applies_style_after_hedges() {
        val raw = "hey um i think we should ship it"
        val r = CleanupPipeline.run(raw, CleanupLevel.HIGH, WritingStyle.FORMAL)
        assertThat(r.clean.lowercase()).doesNotContain("um")
        assertThat(r.clean.trimEnd().last()).isEqualTo('.')
        assertThat(r.clean.first().isUpperCase()).isTrue()
    }

    @Test
    fun course_correct_leading_time_any_tail() {
        val a = CleanupPipeline.run(
            "meet at 4:30 actually 5:30",
            CleanupLevel.NORMAL
        ).clean.lowercase()
        assertThat(a).contains("5:30")
        assertThat(a).doesNotContain("4:30")
        assertThat(a).doesNotContain("4:5:30")

        val b = CleanupPipeline.run(
            "meet at 4:30 actually 5:30 tomorrow with Sam",
            CleanupLevel.NORMAL
        ).clean.lowercase()
        assertThat(b).contains("5:30")
        assertThat(b).contains("tomorrow")
        assertThat(b).contains("sam")
        assertThat(b).doesNotContain("4:30")
    }

    @Test
    fun medium_lists_light_does_not() {
        val raw = "1. Apples 2. Bananas 3. Oranges"
        val light = CleanupPipeline.run(raw, CleanupLevel.LIGHT).clean
        val medium = CleanupPipeline.run(raw, CleanupLevel.NORMAL).clean
        assertThat(light).doesNotContain("\n")
        assertThat(medium).contains("\n")
        assertThat(medium).contains("1. Apples")
    }

    @Test
    fun light_grammar_lone_i() {
        val r = CleanupPipeline.run("i want pizza", CleanupLevel.LIGHT)
        assertThat(r.clean).contains("I want")
    }

    @Test
    fun light_skips_clarity_opener_strip() {
        // lightClarity is Medium+ only
        val light = CleanupPipeline.run("Well, we should go", CleanupLevel.LIGHT).clean.lowercase()
        val medium = CleanupPipeline.run("Well, we should go", CleanupLevel.NORMAL).clean.lowercase()
        assertThat(light).contains("well")
        assertThat(medium).doesNotContain("well")
        assertThat(medium).contains("we should go")
    }
}
