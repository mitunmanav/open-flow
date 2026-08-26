package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HallucinationGuardTest {

    @Test
    fun fourgram_loop_collapses_to_one() {
        val t = "thank you for watching thank you for watching thank you for watching"
        val r = HallucinationGuard.apply(t)
        assertThat(r.text).isEqualTo("thank you for watching")
        assertThat(r.loopCollapsed).isTrue()
    }

    @Test
    fun unique_content_stays() {
        val t = "please send the report tomorrow"
        val r = HallucinationGuard.apply(t)
        assertThat(r.text).isEqualTo(t)
        assertThat(r.loopCollapsed).isFalse()
        assertThat(r.signatures).isEmpty()
    }

    @Test
    fun signature_flagged_not_deleted() {
        val t = "the meeting is done thanks for watching"
        val r = HallucinationGuard.apply(t)
        assertThat(r.text).contains("thanks for watching")
        assertThat(r.text).contains("meeting")
        assertThat(r.signatures).contains("thanks for watching")
    }

    @Test
    fun short_text_untouched() {
        val r = HallucinationGuard.apply("hello there")
        assertThat(r.text).isEqualTo("hello there")
        assertThat(r.loopCollapsed).isFalse()
    }

    @Test
    fun blank_passthrough() {
        assertThat(HallucinationGuard.apply("").text).isEmpty()
        assertThat(HallucinationGuard.apply("   ").loopCollapsed).isFalse()
    }

    @Test
    fun collapse_is_idempotent() {
        val t = "one two three four one two three four one two three four extra"
        val once = HallucinationGuard.collapseLoops(t)
        assertThat(HallucinationGuard.collapseLoops(once)).isEqualTo(once)
        assertThat(once).isEqualTo("one two three four extra")
    }
}
