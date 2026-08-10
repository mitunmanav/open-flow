package app.openflow.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrivacyDefaultsTest {

    @Test
    fun report_lists_local_defaults() {
        val text = PrivacyDefaults.reportText()
        assertThat(text).contains("on-device STT")
        assertThat(text).contains("cloud STT: OFF")
        assertThat(text).contains("sync: OFF")
        assertThat(text).contains("INTERNET permission: not declared by default")
    }
}
