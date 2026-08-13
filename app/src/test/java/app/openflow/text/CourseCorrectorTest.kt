package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CourseCorrectorTest {

    @Test
    fun time_actually_replaces_last_time() {
        val out = CourseCorrector.apply("set a reminder for 4:30 actually 5:30")
        assertThat(out.lowercase()).contains("5:30")
        assertThat(out.lowercase()).doesNotContain("4:30")
    }

    @Test
    fun meet_at_five_actually_six() {
        val out = CourseCorrector.apply("let's meet at 5 actually 6pm")
        assertThat(out.lowercase()).contains("6")
        assertThat(out.lowercase()).doesNotContain("at 5 ")
    }

    @Test
    fun wait_no_day_replace() {
        val out = CourseCorrector.apply("let's meet Tuesday wait no Friday")
        assertThat(out.lowercase()).contains("friday")
        assertThat(out.lowercase()).doesNotContain("tuesday")
        // structural chunk replace — keep prefix (not full drop)
        assertThat(out.lowercase()).contains("meet")
    }

    @Test
    fun scratch_that_restarts() {
        val out = CourseCorrector.apply("I want to go shopping scratch that start with the budget")
        assertThat(out.lowercase()).contains("budget")
        assertThat(out.lowercase()).doesNotContain("shopping")
    }

    @Test
    fun make_that_number() {
        val out = CourseCorrector.apply("budget 50k actually make that 75k")
        assertThat(out.lowercase()).contains("75k")
        assertThat(out.lowercase()).doesNotContain("50k")
    }

    @Test
    fun no_marker_unchanged_core() {
        val out = CourseCorrector.apply("hello world from open flow")
        assertThat(out.lowercase()).contains("hello world")
    }

    @Test
    fun blank_safe() {
        assertThat(CourseCorrector.apply("")).isEmpty()
        assertThat(CourseCorrector.apply("   ")).isEmpty()
    }

    @Test
    fun amount_comma_actually_number() {
        val out = CourseCorrector.apply("The amount is 430, actually 530.")
        assertThat(out.lowercase()).contains("530")
        assertThat(out.lowercase()).doesNotContain("430")
        assertThat(out.lowercase()).doesNotContain("actually")
    }

    @Test
    fun meeting_comma_actually_time() {
        val out = CourseCorrector.apply("The meeting is at 4:30, actually 5:30.")
        assertThat(out.lowercase()).contains("5:30")
        assertThat(out.lowercase()).doesNotContain("4:30")
        assertThat(out.lowercase()).doesNotContain("actually")
    }

    @Test
    fun deadline_no_date_ordinal() {
        val out = CourseCorrector.apply("The deadline is August 12th, no, August 15th.")
        assertThat(out.lowercase()).contains("15")
        assertThat(out.lowercase()).doesNotContain("12")
        assertThat(out.lowercase()).contains("deadline")
        assertThat(out.lowercase()).contains("august")
    }

    @Test
    fun send_to_john_no_james() {
        val out = CourseCorrector.apply("Send it to John. No, send it to James.")
        assertThat(out.lowercase()).contains("james")
        assertThat(out.lowercase()).doesNotContain("john")
    }

    @Test
    fun analyze_records_corrections() {
        val r = CourseCorrector.analyze("The amount is 430, actually 530.")
        assertThat(r.text.lowercase()).contains("530")
        assertThat(r.text.lowercase()).doesNotContain("430")
        assertThat(r.corrections).isNotEmpty()
        assertThat(r.corrections.first().originalText).contains("430")
        assertThat(r.corrections.first().replacementText).contains("530")
        assertThat(r.corrections.first().marker.lowercase()).contains("actually")
    }

    @Test
    fun apply_matches_analyze_text() {
        val raw = "let's meet Tuesday wait no Friday"
        assertThat(CourseCorrector.apply(raw)).isEqualTo(CourseCorrector.analyze(raw).text)
    }

    @Test
    fun i_mean_and_instead_markers() {
        val a = CourseCorrector.apply("call Bob i mean Rob")
        assertThat(a.lowercase()).contains("rob")
        assertThat(a.lowercase()).doesNotContain("bob")

        val b = CourseCorrector.apply("use red instead blue")
        assertThat(b.lowercase()).contains("blue")
        assertThat(b.lowercase()).doesNotContain("red")
    }

    @Test
    fun rather_and_sorry_markers() {
        val a = CourseCorrector.apply("pick option A rather option B")
        assertThat(a.lowercase()).contains("b")
        assertThat(a.lowercase()).doesNotContain("option a")
        assertThat(a.lowercase()).contains("pick")

        val b = CourseCorrector.apply("meet at 3 sorry 4")
        assertThat(b.lowercase()).contains("4")
        assertThat(b.lowercase()).doesNotContain("at 3")
        assertThat(b.lowercase()).contains("meet")
    }

    @Test
    fun no_fluff_phrase_hardcode_generic_tail() {
        // Any tail after entity replace must survive (not one test phrase)
        val out = CourseCorrector.apply("call at 9 actually 10 with the team later")
        assertThat(out.lowercase()).contains("10")
        assertThat(out.lowercase()).doesNotContain(" 9 ")
        assertThat(out.lowercase()).contains("team")
        assertThat(out.lowercase()).contains("later")
    }

    @Test
    fun bare_no_in_normal_speech_is_not_a_correction() {
        val out = CourseCorrector.apply("I have no time today")
        assertThat(out.lowercase()).contains("no")
        assertThat(out.lowercase()).contains("time")
        assertThat(out.lowercase()).contains("have")
    }

    @Test
    fun bare_wait_in_normal_speech_is_not_a_correction() {
        val out = CourseCorrector.apply("please wait here")
        assertThat(out.lowercase()).contains("wait")
        assertThat(out.lowercase()).contains("please")
        assertThat(out.lowercase()).contains("here")
    }

    @Test
    fun or_wait_replaces_day_keeps_prefix() {
        val out = CourseCorrector.apply("let's do Monday or wait Tuesday")
        assertThat(out.lowercase()).contains("tuesday")
        assertThat(out.lowercase()).doesNotContain("monday")
        assertThat(out.lowercase()).contains("let's do")
    }

    @Test
    fun no_wait_keeps_prefix() {
        val out = CourseCorrector.apply("email Sarah no wait email Tom")
        assertThat(out.lowercase()).contains("tom")
        assertThat(out.lowercase()).doesNotContain("sarah")
        assertThat(out.lowercase()).contains("email")
    }

    @Test
    fun i_meant_replaces_name() {
        val out = CourseCorrector.apply("call mom I meant dad")
        assertThat(out.lowercase()).contains("dad")
        assertThat(out.lowercase()).doesNotContain("mom")
        assertThat(out.lowercase()).contains("call")
    }

    @Test
    fun hang_on_swaps_time() {
        val out = CourseCorrector.apply("meet at noon hang on 3pm")
        assertThat(out.lowercase()).contains("3")
        assertThat(out.lowercase()).doesNotContain("noon")
        assertThat(out.lowercase()).contains("meet")
    }

    @Test
    fun on_second_thought_restarts_short_intent() {
        val out = CourseCorrector.apply("buy milk on second thought buy eggs")
        assertThat(out.lowercase()).contains("eggs")
        assertThat(out.lowercase()).doesNotContain("milk")
    }

    @Test
    fun never_mind_restarts() {
        val out = CourseCorrector.apply("I was going to call never mind start the budget")
        assertThat(out.lowercase()).contains("budget")
        assertThat(out.lowercase()).doesNotContain("call")
    }

    @Test
    fun sorry_in_apology_is_not_a_correction() {
        val out = CourseCorrector.apply("I am sorry about that")
        assertThat(out.lowercase()).contains("sorry")
        assertThat(out.lowercase()).contains("about")
        assertThat(out.lowercase()).contains("that")
    }

    @Test
    fun non_empty_without_structured_fix_keeps_all_words() {
        val raw = "hello actually world"
        val out = CourseCorrector.apply(raw)
        assertThat(out.lowercase()).contains("hello")
        assertThat(out.lowercase()).contains("actually")
        assertThat(out.lowercase()).contains("world")
    }
}
