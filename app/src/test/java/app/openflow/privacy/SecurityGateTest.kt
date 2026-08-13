package app.openflow.privacy

import app.openflow.ai.providers.cloud.CloudHttpSafe
import app.openflow.ai.providers.host.HostUrl
import app.openflow.prefs.FlowPrefs
import app.openflow.secrets.AndroidSecretStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Launch-ready security gate. Pins SECURITY.md defaults in XML + honesty helpers.
 */
class SecurityGateTest {

    @Test
    fun manifest_declares_internet_backup_off_nsc_extraction() {
        val xml = locate("src/main/AndroidManifest.xml").readText()
        assertThat(xml).contains("android.permission.INTERNET")
        assertThat(xml).contains("android:allowBackup=\"false\"")
        assertThat(xml).contains("android:networkSecurityConfig=\"@xml/network_security_config\"")
        assertThat(xml).contains("android:dataExtractionRules=\"@xml/data_extraction_rules\"")
        assertThat(xml).contains("android:fullBackupContent=\"@xml/backup_rules\"")
        assertThat(xml).contains("android:usesCleartextTraffic=\"false\"")
    }

    @Test
    fun backup_rules_exclude_all_domains() {
        val xml = locate("src/main/res/xml/backup_rules.xml").readText()
        assertThat(xml).contains("<full-backup-content>")
        for (domain in listOf("root", "file", "database", "sharedpref", "external")) {
            assertThat(xml).contains("<exclude domain=\"$domain\"")
        }
    }

    @Test
    fun data_extraction_excludes_sharedpref_and_db() {
        val xml = locate("src/main/res/xml/data_extraction_rules.xml").readText()
        assertThat(xml).contains("<exclude domain=\"sharedpref\"")
        assertThat(xml).contains("<exclude domain=\"database\"")
        assertThat(xml).contains("<device-transfer>")
    }

    @Test
    fun secrets_prefs_not_flow_prefs() {
        assertThat(AndroidSecretStore.PREFS_NAME).isEqualTo("openflow_secrets")
        assertThat(AndroidSecretStore.PREFS_NAME).isNotEqualTo(FlowPrefs.PREFS_NAME)
    }

    @Test
    fun vendor_honesty_grok_is_xai_not_groq() {
        val text = PrivacyDefaults.reportText()
        assertThat(text).contains("Grok: xAI (not Groq)")
        assertThat(text.lowercase()).doesNotContain("api.groq.com")
        assertThat(text).contains("account required: no")
        assertThat(text).contains("Analytics: disabled")
    }

    @Test
    fun report_never_embeds_sample_api_key() {
        val sample = "sk-live-secret-SHOULD-NOT-APPEAR"
        val text = PrivacyDefaults.reportText()
        assertThat(text).doesNotContain(sample)
        assertThat(text.lowercase()).doesNotContain("sk-")
    }

    @Test
    fun hosturl_http_matches_nsc_domain_literals() {
        val xml = locate("src/main/res/xml/network_security_config.xml").readText()
        val domains = Regex("""<domain(?:\s[^>]*)?>([^<]+)</domain>""")
            .findAll(xml)
            .map { it.groupValues[1].trim() }
            .toList()
        assertThat(domains).isNotEmpty()
        for (d in domains) {
            val host = if (":" in d) "[$d]" else d
            assertThat(HostUrl.allow("http://$host")).isTrue()
        }
        for (h in HostUrl.NSC_CLEARTEXT_HOSTS) {
            assertThat(xml).contains(">$h<")
        }
        assertThat(HostUrl.allow("http://192.168.1.5")).isFalse()
        assertThat(HostUrl.allow("http://172.31.255.255")).isFalse()
        assertThat(xml).contains("cleartextTrafficPermitted=\"false\"")
        assertThat(xml).doesNotContain("cleartextTrafficPermitted=\"true\" />")
    }

    @Test
    fun report_honest_about_brain_post() {
        val text = PrivacyDefaults.reportText()
        assertThat(text).contains("Transcript uploaded by OpenFlow: only if cloud brain")
        assertThat(text.lowercase()).doesNotContain("transcript uploaded by openflow: never")
        assertThat(text).contains("Audio uploaded by OpenFlow: only if cloud ear")
    }

    @Test
    fun cloud_headers_redact_api_keys() {
        val redacted = CloudHttpSafe.redactHeaders(
            mapOf(
                "Authorization" to "Bearer sk-live-secret",
                "x-api-key" to "ant-key",
                "Content-Type" to "application/json",
            ),
        )
        assertThat(redacted["Authorization"]).isEqualTo("REDACTED")
        assertThat(redacted["x-api-key"]).isEqualTo("REDACTED")
        assertThat(redacted["Content-Type"]).isEqualTo("application/json")
        assertThat(CloudHttpSafe.isSecretHeader("api-key")).isTrue()
        assertThat(CloudHttpSafe.isSecretHeader("Content-Type")).isFalse()
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
