package app.openflow.stt.providers.ondevice

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnPhoneModelUiTest {
    @Test
    fun line_ready_busy_missing() {
        assertThat(OnPhoneModelUi.line(ready = true, busy = false)).isEqualTo("tiny.en · ready")
        assertThat(OnPhoneModelUi.line(ready = false, busy = true)).isEqualTo("tiny.en · downloading")
        assertThat(OnPhoneModelUi.line(ready = false, busy = false)).isEqualTo("tiny.en · not on this phone")
    }

    @Test
    fun done_not_live_is_honest() {
        assertThat(OnPhoneModelUi.DONE_NOT_LIVE)
            .isEqualTo("Text shows after Done. Not live like Gboard.")
    }
}
