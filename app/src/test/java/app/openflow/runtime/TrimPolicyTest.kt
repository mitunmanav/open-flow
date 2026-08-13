package app.openflow.runtime

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrimPolicyTest {

    @Test
    fun drop_idle_stt_at_background() {
        assertThat(TrimPolicy.shouldDropIdleStt(40)).isTrue()
    }

    @Test
    fun drop_idle_stt_above_background() {
        assertThat(TrimPolicy.shouldDropIdleStt(80)).isTrue()
    }

    @Test
    fun keep_idle_stt_below_background() {
        assertThat(TrimPolicy.shouldDropIdleStt(39)).isFalse()
        assertThat(TrimPolicy.shouldDropIdleStt(20)).isFalse()
        assertThat(TrimPolicy.shouldDropIdleStt(0)).isFalse()
    }
}
