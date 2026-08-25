package app.openflow.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CaptureCapTest {

    @Test
    fun admits_under_cap() {
        assertThat(CaptureCap.admit(0L, 1000, CaptureCap.DEFAULT_MAX_BYTES)).isTrue()
    }

    @Test
    fun admits_exactly_to_cap() {
        assertThat(CaptureCap.admit(CaptureCap.DEFAULT_MAX_BYTES - 100, 100, CaptureCap.DEFAULT_MAX_BYTES)).isTrue()
    }

    @Test
    fun rejects_past_cap() {
        assertThat(CaptureCap.admit(CaptureCap.DEFAULT_MAX_BYTES, 1, CaptureCap.DEFAULT_MAX_BYTES)).isFalse()
    }
}
