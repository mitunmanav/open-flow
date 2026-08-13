package app.openflow.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrivacyDefaultsTest {

    @Test
    fun report_lists_honest_local_defaults() {
        val text = PrivacyDefaults.reportText()
        assertThat(text).contains("Speech recognition: Android system STT (may leave device)")
        assertThat(text).contains("OpenFlow server: none")
        assertThat(text).contains("Analytics: disabled")
        assertThat(text).contains("Audio uploaded by OpenFlow: never")
        assertThat(text).contains("Transcript uploaded by OpenFlow: never")
        assertThat(text).contains("Local history: on device")
        assertThat(text).contains("sync: OFF")
        assertThat(text).contains("INTERNET permission: declared; unused until user pick")
        // Must not overclaim STT stays on device
        assertThat(text.lowercase()).doesNotContain("voice stays on device")
    }
}
