package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CleanupPipelineTest {

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
        assertThat(r.clean.lowercase()).isEqualTo("i like pizza.")
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
    }

    @Test
    fun light_skips_course_correct() {
        val r = CleanupPipeline.run("amount is 430 actually 530", CleanupLevel.LIGHT)
        // fillers only; correction optional off
        assertThat(r.clean.lowercase()).contains("430")
    }

    @Test
    fun blank_safe() {
        val r = CleanupPipeline.run("   ")
        assertThat(r.clean).isEmpty()
        assertThat(r.raw).isEmpty()
    }
}
