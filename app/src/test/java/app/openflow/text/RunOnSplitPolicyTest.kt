package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RunOnSplitPolicyTest {

    @Test
    fun then_i_splits() {
        val out = RunOnSplitPolicy.apply(
            "I went to the store then I bought milk today too"
        )
        assertThat(out).contains("store.")
        assertThat(out).contains("I bought")
    }

    @Test
    fun bread_and_butter_no_split() {
        val s = "I like bread and butter on toast"
        assertThat(RunOnSplitPolicy.apply(s)).isEqualTo(s)
    }

    @Test
    fun skip_inside_numbered_list() {
        val s = "1. first item and then I skip 2. second"
        assertThat(RunOnSplitPolicy.apply(s)).isEqualTo(s)
    }
}
