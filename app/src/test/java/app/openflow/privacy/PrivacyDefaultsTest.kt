package app.openflow.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrivacyDefaultsTest {

    @Test
    fun defaults_are_local_only() {
        val d = PrivacyDefaults()
        assertThat(d.preferOnDeviceStt).isTrue()
        assertThat(d.allowCloudStt).isFalse()
        assertThat(d.allowSync).isFalse()
        assertThat(d.allowCrashReports).isFalse()
        assertThat(d.analyticsEnabled).isFalse()
    }

    @Test
    fun report_lists_all_toggles() {
        val text = PrivacyDefaults().reportText()
        assertThat(text).contains("on-device STT")
        assertThat(text).contains("cloud STT: OFF")
        assertThat(text).contains("sync: OFF")
        assertThat(text).contains("INTERNET permission: not declared by default")
    }
}
