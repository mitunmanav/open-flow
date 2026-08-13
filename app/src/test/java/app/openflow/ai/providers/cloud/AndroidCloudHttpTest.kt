package app.openflow.ai.providers.cloud

import app.openflow.engine.RateLimit
import app.openflow.engine.RateLimitResult
import app.openflow.engine.SendPolicy
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

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

    @Test
    fun host_of_is_url_host() {
        assertThat(CloudHttpSafe.hostOf("https://api.x.ai/v1/chat")).isEqualTo("api.x.ai")
        assertThat(CloudHttpSafe.hostOf("https://api.openai.com/v1")).isEqualTo("api.openai.com")
    }

    @Test
    fun rate_gate_denied_throws_rate_limited() {
        val now = mutableLong(0L)
        val limit = RateLimit(perMinute = 1, nowMs = { now.value })
        CloudHttpSafe.rateGate("https://api.x.ai/v1", limit)
        val err = runCatching {
            CloudHttpSafe.rateGate("https://api.x.ai/v1/chat", limit)
        }.exceptionOrNull()
        assertThat(err).isInstanceOf(IOException::class.java)
        assertThat(err!!.message).isEqualTo("rate limited")
    }

    @Test
    fun rate_gate_buckets_by_host() {
        val now = mutableLong(0L)
        val limit = RateLimit(perMinute = 1, nowMs = { now.value })
        CloudHttpSafe.rateGate("https://api.x.ai/v1/a", limit)
        CloudHttpSafe.rateGate("https://api.openai.com/v1", limit)
        val sameHost = runCatching {
            CloudHttpSafe.rateGate("https://api.x.ai/v1/b", limit)
        }.exceptionOrNull()
        assertThat(sameHost).isInstanceOf(IOException::class.java)
        assertThat(sameHost!!.message).isEqualTo("rate limited")
    }

    @Test
    fun post_denied_is_rate_limited_no_auth_leak() {
        val now = mutableLong(0L)
        val limit = RateLimit(perMinute = 1, nowMs = { now.value })
        assertThat(limit.tryAcquire("api.x.ai")).isEqualTo(RateLimitResult.Allowed)
        val http = AndroidCloudHttp(limit)
        val err = runCatching {
            http.post(
                "https://api.x.ai/v1/chat",
                mapOf("Authorization" to "Bearer sk-live-secret"),
                "{}",
            )
        }.exceptionOrNull()
        assertThat(err).isInstanceOf(IOException::class.java)
        assertThat(err!!.message).isEqualTo("rate limited")
        assertThat(err.message.orEmpty().lowercase()).doesNotContain("bearer")
        assertThat(err.message.orEmpty()).doesNotContain("sk-live-secret")
    }

    @Test
    fun minimize_is_send_policy() {
        val text = "mail me at a@b.com or 9876543210 thanks"
        assertThat(CloudMinimize.forBrain(text)).isEqualTo(SendPolicy.forBrain(text))
        assertThat(CloudMinimize.forBrain(text)).doesNotContain("a@b.com")
        assertThat(CloudMinimize.forBrain(text)).doesNotContain("9876543210")
    }

    private fun mutableLong(start: Long) = object {
        var value: Long = start
    }
}
