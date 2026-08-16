package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.CloudProviders
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AllEarsTest {

    @Test
    fun test_all_ears_instantiated_via_cloud_providers() {
        val sock = FakeSocket()
        val allEarIds = listOf("openai", "deepgram", "assemblyai", "sarvam")

        for (id in allEarIds) {
            val ear = CloudProviders.ear(id, { "test-ear-key-$id" }, sock)
            assertThat(ear).isNotNull()
        }
    }

    @Test
    fun test_openai_realtime_ear_connect_and_auth() {
        val sock = FakeSocket()
        val ear = OpenAiRealtimeEar(apiKey = { "sk-openai-key" }, socket = sock)
        ear.startOnce("en")
        assertThat(sock.headers["Authorization"]).isEqualTo("Bearer sk-openai-key")
        assertThat(sock.headers["OpenAI-Beta"]).isEqualTo("realtime=v1")
        assertThat(sock.url).contains("wss://api.openai.com/v1/realtime")
    }

    @Test
    fun test_deepgram_ear_connect_and_auth() {
        val sock = FakeSocket()
        val ear = DeepgramEar(apiKey = { "dg-key" }, socket = sock)
        ear.startOnce("en-US")
        assertThat(sock.headers["Authorization"]).isEqualTo("Token dg-key")
        assertThat(sock.url).contains("wss://api.deepgram.com/v1/listen")
        assertThat(sock.url).contains("model=nova-2")
    }

    @Test
    fun test_assembly_ear_connect_and_auth() {
        val sock = FakeSocket()
        val ear = AssemblyEar(apiKey = { "assembly-key" }, socket = sock)
        ear.startOnce("en")
        assertThat(sock.headers["Authorization"]).isEqualTo("assembly-key")
        assertThat(sock.url).contains("wss://streaming.assemblyai.com/v3/ws")
    }

    @Test
    fun test_sarvam_ear_connect_and_auth() {
        val sock = FakeSocket()
        val ear = SarvamEar(apiKey = { "sarvam-key" }, socket = sock, mode = "transcribe")
        ear.startOnce("en-IN")
        assertThat(sock.headers["api-subscription-key"]).isEqualTo("sarvam-key")
        assertThat(sock.url).contains("wss://api.sarvam.ai/speech-to-text/ws")
        assertThat(sock.url).contains("model=saaras:v3")
    }
}
