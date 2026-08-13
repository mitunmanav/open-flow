package app.openflow.ai.providers.host

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HostUrlTest {

    @Test
    fun https_any_host_allowed() {
        assertThat(HostUrl.allow("https://example.com/v1")).isTrue()
        assertThat(HostUrl.allow("https://api.x.ai/v1")).isTrue()
        assertThat(HostUrl.allow("HTTPS://192.168.1.5:11434/v1")).isTrue()
    }

    @Test
    fun http_nsc_literals_allowed() {
        assertThat(HostUrl.allow("http://10.0.0.1/v1")).isTrue()
        assertThat(HostUrl.allow("http://10.0.0.2/v1")).isTrue()
        assertThat(HostUrl.allow("http://10.0.2.2:11434/v1")).isTrue()
        assertThat(HostUrl.allow("http://172.16.0.1:8080")).isTrue()
        assertThat(HostUrl.allow("http://172.17.0.1")).isTrue()
        assertThat(HostUrl.allow("http://192.168.0.1")).isTrue()
        assertThat(HostUrl.allow("http://192.168.1.1")).isTrue()
        assertThat(HostUrl.allow("http://192.168.1.10")).isTrue()
        assertThat(HostUrl.allow("http://192.168.1.100")).isTrue()
        assertThat(HostUrl.allow("http://127.0.0.1:11434/v1")).isTrue()
        assertThat(HostUrl.allow("http://localhost:11434/v1")).isTrue()
        assertThat(HostUrl.allow("http://ip6-localhost:11434/v1")).isTrue()
        assertThat(HostUrl.allow("http://[::1]:11434/v1")).isTrue()
        assertThat(HostUrl.allow("http://foo.localhost:11434/v1")).isTrue()
    }

    @Test
    fun http_rfc1918_not_on_nsc_rejected() {
        assertThat(HostUrl.allow("http://192.168.1.5:11434/v1")).isFalse()
        assertThat(HostUrl.allow("http://172.31.255.255")).isFalse()
        assertThat(HostUrl.allow("http://10.0.0.3/v1")).isFalse()
        assertThat(HostUrl.allow("http://127.0.0.2")).isFalse()
        assertThat(HostUrl.allow("http://169.254.1.1:9000")).isFalse()
        assertThat(HostUrl.allow("http://[fe80::1]:9000")).isFalse()
    }

    @Test
    fun empty_and_file_rejected() {
        assertThat(HostUrl.allow(null)).isFalse()
        assertThat(HostUrl.allow("")).isFalse()
        assertThat(HostUrl.allow("   ")).isFalse()
        assertThat(HostUrl.allow("file:///tmp/model")).isFalse()
        assertThat(HostUrl.allow("file://192.168.1.1/x")).isFalse()
    }

    @Test
    fun public_http_rejected() {
        assertThat(HostUrl.allow("http://example.com/v1")).isFalse()
        assertThat(HostUrl.allow("http://8.8.8.8/v1")).isFalse()
        assertThat(HostUrl.allow("http://1.1.1.1")).isFalse()
        assertThat(HostUrl.allow("http://172.15.0.1")).isFalse()
        assertThat(HostUrl.allow("http://172.32.0.1")).isFalse()
        assertThat(HostUrl.allow("http://100.64.1.1")).isFalse()
    }
}
