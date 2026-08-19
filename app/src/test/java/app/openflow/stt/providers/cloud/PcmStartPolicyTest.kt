package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PcmStartPolicyTest {

    @Test
    fun buffer_must_be_positive() {
        assertThat(PcmStartPolicy.bufferOk(0)).isFalse()
        assertThat(PcmStartPolicy.bufferOk(-2)).isFalse()
        assertThat(PcmStartPolicy.bufferOk(1280)).isTrue()
    }

    @Test
    fun record_must_be_initialized() {
        assertThat(PcmStartPolicy.recordOk(initialized = false)).isFalse()
        assertThat(PcmStartPolicy.recordOk(initialized = true)).isTrue()
    }
}
