package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionStopPolicyTest {
    @Test
    fun on_phone_limit_commits_not_discard() {
        val a = SessionStopPolicy.onLimit("on_phone")
        assertThat(a).isEqualTo(SessionStopPolicy.Action.COMMIT)
        assertThat(SessionStopPolicy.save(a)).isTrue()
    }

    @Test
    fun system_limit_still_commits() {
        assertThat(SessionStopPolicy.onLimit("system"))
            .isEqualTo(SessionStopPolicy.Action.COMMIT)
    }
}
