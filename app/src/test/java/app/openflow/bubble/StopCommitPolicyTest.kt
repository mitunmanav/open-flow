package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StopCommitPolicyTest {

    @Test
    fun table() {
        assertThat(StopCommitPolicy.decide(save = true, raw = "hi"))
            .isEqualTo(StopCommitPolicy.Action.POLISH_INSERT)
        assertThat(StopCommitPolicy.decide(save = true, raw = "  "))
            .isEqualTo(StopCommitPolicy.Action.PERSIST_FAIL)
        assertThat(StopCommitPolicy.decide(save = true, raw = ""))
            .isEqualTo(StopCommitPolicy.Action.PERSIST_FAIL)
        assertThat(StopCommitPolicy.decide(save = false, raw = "hi"))
            .isEqualTo(StopCommitPolicy.Action.DISCARD)
        assertThat(StopCommitPolicy.decide(save = false, raw = ""))
            .isEqualTo(StopCommitPolicy.Action.DISCARD)
    }
}
