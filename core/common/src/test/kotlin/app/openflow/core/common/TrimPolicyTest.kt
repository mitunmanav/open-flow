package app.openflow.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrimPolicyTest {
    @Test fun `below 20 keeps`() {
        assertThat(TrimPolicy.action(0)).isEqualTo(TrimPolicy.Action.KEEP)
        assertThat(TrimPolicy.shouldDropIdleStt(0)).isFalse()
        assertThat(TrimPolicy.shouldReleaseUiCaches(0)).isFalse()
    }

    @Test fun `20 releases ui but keeps idle stt`() {
        assertThat(TrimPolicy.action(20)).isEqualTo(TrimPolicy.Action.RELEASE_UI)
        assertThat(TrimPolicy.shouldDropIdleStt(20)).isFalse()
        assertThat(TrimPolicy.shouldReleaseUiCaches(20)).isTrue()
    }

    @Test fun `40 drops idle stt`() {
        assertThat(TrimPolicy.action(40)).isEqualTo(TrimPolicy.Action.DROP_IDLE_STT)
        assertThat(TrimPolicy.shouldDropIdleStt(40)).isTrue()
    }

    @Test fun `never drops while listening or flushing`() {
        assertThat(TrimPolicy.dropIdleEngine(40, listening = true)).isFalse()
        assertThat(TrimPolicy.dropIdleEngine(40, listening = false, stopInProgress = true)).isFalse()
        assertThat(TrimPolicy.dropIdleEngine(40, listening = false)).isTrue()
    }
}
