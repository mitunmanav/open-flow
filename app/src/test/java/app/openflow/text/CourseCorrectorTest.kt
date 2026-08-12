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

        val b = CourseCorrector.apply("meet at 3 sorry 4")
        assertThat(b.lowercase()).contains("4")
        assertThat(b.lowercase()).doesNotContain("at 3")
    }
}
