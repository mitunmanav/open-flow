package app.openflow.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class PrivacyDefaultsTest {

    @Test
    fun report_lists_honest_local_defaults() {
        val text = PrivacyDefaults.reportText()
        assertThat(text).contains("Speech recognition: Android system STT (may leave device)")
        assertThat(text).contains("OpenFlow server: none")
        assertThat(text).contains("Analytics: disabled")
        assertThat(text).contains("Audio uploaded by OpenFlow: only if cloud ear")
        assertThat(text).contains("Transcript uploaded by OpenFlow: never")
        assertThat(text).contains("Local history: on device")
        assertThat(text).contains("sync: OFF")
        assertThat(text).contains("INTERNET permission: declared; unused until user pick")
        assertThat(text).contains("Grok: xAI (not Groq)")
        // Must not overclaim STT stays on device
        assertThat(text.lowercase()).doesNotContain("voice stays on device")
        assertThat(text.lowercase()).doesNotContain("audio stored")
        assertThat(text.lowercase()).doesNotContain("api.groq.com")
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
            assertThat(text).contains("audio uploaded by openflow: only if cloud ear")
        }
    }

    @Test
    fun audio_leaves_only_if_cloud_ear() {
        assertThat(PrivacyDefaults.audioLeaves("openai")).isTrue()
        assertThat(PrivacyDefaults.audioLeaves("deepgram")).isTrue()
        assertThat(PrivacyDefaults.audioLeaves("assemblyai")).isTrue()
        assertThat(PrivacyDefaults.audioLeaves("sarvam")).isTrue()
        assertThat(PrivacyDefaults.audioLeaves("laptop")).isTrue()
        assertThat(PrivacyDefaults.audioLeaves("custom_stt")).isTrue()
        assertThat(PrivacyDefaults.audioLeaves("on_phone")).isFalse()
        assertThat(PrivacyDefaults.audioLeaves("system")).isFalse()
        assertThat(PrivacyDefaults.audioLeaves("none")).isFalse()
    }

    @Test
    fun grok_is_xai_not_groq() {
        val text = PrivacyDefaults.reportText()
        assertThat(text).contains("Grok: xAI (not Groq)")
        assertThat(text.lowercase()).doesNotContain("grok = groq")
        assertThat(text.lowercase()).doesNotContain("api.groq.com")
    }

    @Test
    fun nsc_public_https_only_cleartext_is_listed_lan() {
        val xml = locate("src/main/res/xml/network_security_config.xml").readText()
        assertThat(xml).contains("cleartextTrafficPermitted=\"false\"")
        assertThat(xml).contains("<domain includeSubdomains=\"true\">localhost</domain>")
        assertThat(xml).contains("<domain>127.0.0.1</domain>")
        assertThat(xml).contains("<domain>10.0.2.2</domain>")
        assertThat(xml).contains("<domain>192.168.1.1</domain>")
        assertThat(xml).doesNotContain("cleartextTrafficPermitted=\"true\" />")
        assertThat(xml.lowercase()).doesNotContain("api.openai.com")
        assertThat(xml.lowercase()).doesNotContain("api.groq.com")
    }

    private fun locate(rel: String): File {
        var dir = File(".").canonicalFile
        repeat(6) {
            val hit = File(dir, rel)
            if (hit.isFile) return hit
            val underApp = File(dir, "app/$rel")
            if (underApp.isFile) return underApp
            dir = dir.parentFile ?: return File(rel)
        }
        return File(rel)
    }
}
