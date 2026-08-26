package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContinuationPolicyTest {

    @Test
    fun open_prefix_lowercases_join() {
        assertThat(ContinuationPolicy.join("I think we should", "Wait for the report."))
            .isEqualTo("I think we should wait for the report.")
    }

    @Test
    fun closed_prefix_keeps_caps() {
        assertThat(ContinuationPolicy.join("Done.", "Wait for the report."))
            .isEqualTo("Done. Wait for the report.")
        assertThat(ContinuationPolicy.join("Ready?", "Yes."))
            .isEqualTo("Ready? Yes.")
    }

    @Test
    fun newline_is_closed() {
        assertThat(ContinuationPolicy.join("Hello\n", "Wait"))
            .isEqualTo("Hello\nWait")
    }

    @Test
    fun empty_sides() {
        assertThat(ContinuationPolicy.join("", "Only")).isEqualTo("Only")
        assertThat(ContinuationPolicy.join("Hi", "")).isEqualTo("Hi")
    }

    @Test
    fun overlap_skips_join() {
        assertThat(ContinuationPolicy.join("Does", "Does naren know"))
            .isEqualTo("Does naren know")
    }

    @Test
    fun acronym_stays_upper() {
        assertThat(ContinuationPolicy.join("Call the", "API later"))
            .isEqualTo("Call the API later")
    }

    @Test
    fun punct_piece_no_extra_space() {
        assertThat(ContinuationPolicy.join("Hi", ".")).isEqualTo("Hi.")
    }
}
