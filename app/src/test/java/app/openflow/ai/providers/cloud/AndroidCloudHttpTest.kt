package app.openflow.ai.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AndroidCloudHttpTest {

    @Test
    fun redact_strips_authorization_and_keys() {
        val out = CloudHttpSafe.redactHeaders(
            mapOf(
                "Authorization" to "Bearer sk-secret",
                "x-api-key" to "anth-secret",
                "api-key" to "other-secret",
                "Content-Type" to "application/json",
            ),
        )
        assertThat(out["Authorization"]).isEqualTo("REDACTED")
        assertThat(out["x-api-key"]).isEqualTo("REDACTED")
        assertThat(out["api-key"]).isEqualTo("REDACTED")
        assertThat(out["Content-Type"]).isEqualTo("application/json")
        assertThat(out.toString()).doesNotContain("sk-secret")
        assertThat(out.toString()).doesNotContain("anth-secret")
        assertThat(out.toString()).doesNotContain("other-secret")
    }

    @Test
    fun allow_https_and_lan_http_not_public_http() {
        assertThat(CloudHttpSafe.allowUrl("https://api.x.ai/v1/chat")).isTrue()
        assertThat(CloudHttpSafe.allowUrl("http://192.168.1.10:11434/v1")).isTrue()
        assertThat(CloudHttpSafe.allowUrl("http://127.0.0.1:8080/v1")).isTrue()
        assertThat(CloudHttpSafe.allowUrl("http://example.com/v1")).isFalse()
        assertThat(CloudHttpSafe.allowUrl("http://api.openai.com/v1")).isFalse()
        assertThat(CloudHttpSafe.allowUrl("")).isFalse()
    }

    @Test
    fun post_rejects_public_http_and_does_not_leak_key() {
        val http = AndroidCloudHttp()
        val err = runCatching {
            http.post(
                "http://example.com/v1",
                mapOf("Authorization" to "Bearer sk-live-secret"),
                "{}",
            )
        }.exceptionOrNull()
        assertThat(err).isNotNull()
        assertThat(err!!.message.orEmpty().lowercase()).doesNotContain("bearer")
        assertThat(err.message.orEmpty()).doesNotContain("sk-live-secret")
    }

    @Test
    fun timeouts_are_set() {
        assertThat(CloudHttpSafe.CONNECT_MS).isGreaterThan(0)
        assertThat(CloudHttpSafe.READ_MS).isGreaterThan(0)
    }
}
