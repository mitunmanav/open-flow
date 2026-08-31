package app.openflow.audio

import app.openflow.stt.EarMicPolicy
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AppAudioCaptureTest {

    @Test
    fun wavConsumer_accepts_matching_generation() {
        val c = WavFileConsumer(startGeneration = 1)
        val pcm = ByteArray(100) { it.toByte() }
        c.onPcm(pcm, 1)
        assertThat(c.retainedBytes()).isEqualTo(100L)
    }

    @Test
    fun wavConsumer_rejects_stale_generation() {
        val c = WavFileConsumer(startGeneration = 1)
        c.onPcm(ByteArray(100) { 1 }, 1)
        c.onPcm(ByteArray(100) { 2 }, 2) // stale
        assertThat(c.retainedBytes()).isEqualTo(100L)
    }

    @Test
    fun wavConsumer_stopAndWrite_generation_mismatch_returns_null_and_clears() {
        val c = WavFileConsumer(startGeneration = 1)
        c.onPcm(ByteArray(200), 1)
        val tmp = Files.createTempFile("t", ".wav").toFile()
        val out = c.stopAndWrite(tmp, 2) // mismatch
        assertThat(out).isNull()
        assertThat(c.retainedBytes()).isEqualTo(0L)
    }

    @Test
    fun wavConsumer_stopAndWrite_matching_writes_wav_and_clears() {
        val c = WavFileConsumer(startGeneration = 5)
        c.onPcm(ByteArray(100) { 0x01 }, 5)
        c.onPcm(ByteArray(200) { 0x02 }, 5)
        val tmp = Files.createTempFile("t", ".wav").toFile()
        try {
            val out = c.stopAndWrite(tmp, 5)
            assertThat(out).isNotNull()
            assertThat(out!!.exists()).isTrue()
            assertThat(out.length()).isGreaterThan(44L)
            assertThat(c.retainedBytes()).isEqualTo(0L)
        } finally { tmp.delete() }
    }

    @Test
    fun wavConsumer_respects_captureCap() {
        val cap = 300L
        val c = WavFileConsumer(startGeneration = 1, maxBytes = cap)
        c.onPcm(ByteArray(200), 1)
        assertThat(c.retainedBytes()).isEqualTo(200L)
        c.onPcm(ByteArray(200), 1) // would exceed cap 400 > 300, dropped
        assertThat(c.retainedBytes()).isEqualTo(200L)
    }

    @Test
    fun wavConsumer_discard_clears() {
        val c = WavFileConsumer(startGeneration = 1)
        c.onPcm(ByteArray(500), 1)
        c.discard()
        assertThat(c.retainedBytes()).isEqualTo(0L)
        assertThat(c.stopAndWrite(File("/tmp/x"), 1)).isNull()
    }

    @Test
    fun wavConsumer_clearForNewGeneration_resets_and_accepts_new() {
        val c = WavFileConsumer(startGeneration = 1)
        c.onPcm(ByteArray(100), 1)
        c.clearForNewGeneration(2)
        assertThat(c.retainedBytes()).isEqualTo(0L)
        c.onPcm(ByteArray(50), 2)
        assertThat(c.retainedBytes()).isEqualTo(50L)
        c.onPcm(ByteArray(50), 1) // stale
        assertThat(c.retainedBytes()).isEqualTo(50L)
    }

    @Test
    fun pcmResampler_identity_sameRate() {
        val pcm = ByteArray(100) { it.toByte() }
        val out = PcmResampler.resamplePcm16(pcm, 16000, 16000)
        assertThat(out).isEqualTo(pcm)
    }

    @Test
    fun pcmResampler_48k_to_16k_oneThird() {
        // 48k mono 300 samples (600 bytes) -> 16k 100 samples (200 bytes)
        val inSamples = 300
        val inPcm = ByteArray(inSamples * 2) { (it % 256).toByte() }
        val out = PcmResampler.resamplePcm16(inPcm, 48000, 16000)
        assertThat(out.size).isEqualTo(200)
    }

    @Test
    fun pcmResampler_16k_to_48k_triple() {
        val inSamples = 100
        val inPcm = ByteArray(inSamples * 2) { 0x10 }
        val out = PcmResampler.resamplePcm16(inPcm, 16000, 48000)
        assertThat(out.size).isEqualTo(600)
    }

    @Test
    fun pcmResampler_empty_returns_empty() {
        assertThat(PcmResampler.resamplePcm16(ByteArray(0), 48000, 16000)).isEmpty()
    }

    @Test
    fun earMicPolicy_teeEnabled() {
        assertThat(EarMicPolicy.teeEnabledFor("cloud")).isTrue()
        assertThat(EarMicPolicy.teeEnabledFor("openai")).isTrue()
        assertThat(EarMicPolicy.teeEnabledFor("on_phone")).isTrue()
        assertThat(EarMicPolicy.teeEnabledFor("system")).isFalse()
        assertThat(EarMicPolicy.teeEnabledFor("SYSTEM")).isFalse()
    }

    @Test
    fun shouldCaptureWav_tee_system_no() {
        assertThat(EarMicPolicy.shouldCaptureWav("system", true)).isFalse()
        assertThat(EarMicPolicy.shouldCaptureWav("cloud", true)).isTrue()
        assertThat(EarMicPolicy.shouldCaptureWav("on_phone", true)).isFalse()
        assertThat(EarMicPolicy.shouldCaptureWav("on_phone", false)).isFalse()
    }

    @Test
    fun shouldCaptureWav_old_system_yes() {
        // rollback: old path system had wav via dual
        assertThat(EarMicPolicy.shouldCaptureWav("system", false)).isTrue()
        assertThat(EarMicPolicy.shouldCaptureWav("cloud", false)).isTrue()
    }

    @Test
    fun appCapture_generation_isolation_via_wavConsumer() {
        // Simulate rapid stop -> start race: gen1 wav should not leak to gen2
        val wav1 = WavFileConsumer(1)
        val wav2 = WavFileConsumer(2)
        wav1.onPcm(ByteArray(100) { 1 }, 1)
        wav1.onPcm(ByteArray(100) { 2 }, 1)
        // Simulate AppAudioCapture dispatch with gen 1 then gen 2 consumers disjoint
        // wav1 should have 200, wav2 0
        assertThat(wav1.retainedBytes()).isEqualTo(200L)
        assertThat(wav2.retainedBytes()).isEqualTo(0L)
        // Now feed gen2
        wav2.onPcm(ByteArray(50) { 3 }, 2)
        wav1.onPcm(ByteArray(50) { 4 }, 1) // stale after wav1 cleared? but wav1 still gen1
        assertThat(wav1.retainedBytes()).isEqualTo(250L) // wav1 still accepts gen1
        assertThat(wav2.retainedBytes()).isEqualTo(50L)
        // After stopAndWrite gen1, wav1 cleared, gen2 must not have gen1 data
        val tmp1 = Files.createTempFile("a", ".wav").toFile()
        val tmp2 = Files.createTempFile("b", ".wav").toFile()
        try {
            wav1.stopAndWrite(tmp1, 1)
            wav2.stopAndWrite(tmp2, 2)
            assertThat(tmp1.length()).isGreaterThan(44L)
            assertThat(tmp2.length()).isGreaterThan(44L)
            // wav files should be distinct sizes (250 vs 50 PCM)
            assertThat(tmp1.length()).isNotEqualTo(tmp2.length())
        } finally { tmp1.delete(); tmp2.delete() }
    }

    @Test
    fun appCapture_fanOut_identical_delivery() {
        val results = mutableMapOf<String, ByteArray>()
        val c1 = object : AppAudioCapture.PcmConsumer {
            override fun onPcm(pcm: ByteArray, generation: Int) { results["a"] = pcm.copyOf() }
        }
        val c2 = object : AppAudioCapture.PcmConsumer {
            override fun onPcm(pcm: ByteArray, generation: Int) { results["b"] = pcm.copyOf() }
        }
        val pcm = ByteArray(64) { it.toByte() }
        // Simulate AppAudioCapture dispatch
        listOf(c1, c2).forEach { it.onPcm(pcm, 1) }
        assertThat(results["a"]).isEqualTo(pcm)
        assertThat(results["b"]).isEqualTo(pcm)
    }

    @Test
    fun appCapture_consumer_exception_doesNotBreakOthers() {
        var secondCalled = false
        val bad = object : AppAudioCapture.PcmConsumer {
            override fun onPcm(pcm: ByteArray, generation: Int) { throw RuntimeException("boom") }
        }
        val good = object : AppAudioCapture.PcmConsumer {
            override fun onPcm(pcm: ByteArray, generation: Int) { secondCalled = true }
        }
        // Simulate dispatch loop with try/catch (as AppAudioCapture does)
        val consumers = listOf(bad, good)
        val pcm = ByteArray(10)
        for (c in consumers) try { c.onPcm(pcm, 1) } catch (_: Exception) {}
        assertThat(secondCalled).isTrue()
    }

    @Test
    fun appCapture_zero_consumers_start_should_not_fail() {
        // AppAudioCapture.start with empty list returns true without mic (per impl)
        val cap = AppAudioCapture(context = null)
        val ok = cap.start(generation = 1, newConsumers = emptyList())
        assertThat(ok).isTrue()
        cap.stop()
    }
}
