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
}
