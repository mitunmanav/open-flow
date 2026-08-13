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
        assertThat(text.lowercase()).doesNotContain("audio stored")
        assertThat(text).contains("Audio uploaded by OpenFlow: never")
    }

    @Test
    fun keep_says_on_device() {
        val text = PrivacyDefaults.reportText("keep")
        assertThat(text).contains("Local history: on device SQLite (not encrypted) (keep)")
    }

    @Test
    fun wipe_24h_says_purged() {
        val text = PrivacyDefaults.reportText("wipe_24h")
        assertThat(text).contains("Local history: on device, purged after 24h (wipe_24h)")
    }

    @Test
    fun never_store_says_not_stored() {
        val text = PrivacyDefaults.reportText("never_store")
        assertThat(text).contains("Local history: not stored in Room history (never_store)")
    }

    @Test
    fun never_claims_we_kept_audio() {
        for (policy in listOf("keep", "wipe_24h", "never_store")) {
            val text = PrivacyDefaults.reportText(policy).lowercase()
            assertThat(text).doesNotContain("audio stored")
            assertThat(text).doesNotContain("oops we kept audio")
            assertThat(text).contains("audio uploaded by openflow: never")
        }
    }
}
