package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SttTuningDefaultsTest {

    @Test
    fun default_language_is_en_us() {
        assertThat(SttTuning.DEFAULT_LANGUAGE).isEqualTo("en-US")
    }
}
