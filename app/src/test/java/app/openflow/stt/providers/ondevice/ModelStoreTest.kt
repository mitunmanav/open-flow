package app.openflow.stt.providers.ondevice

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.file.Files

class ModelStoreTest {

    @Test
    fun missing_file_is_not_ready() {
        val dir = Files.createTempDirectory("of-models").toFile()
        val store = ModelStore(dir, downloader = { _, _ -> error("no net") }, minReadyBytes = 1L)
        assertThat(store.isReady("tiny.en")).isFalse()
        assertThat(store.file("tiny.en").exists()).isFalse()
    }

    @Test
    fun ensure_writes_bytes_to_ggml_tiny_en_bin() {
        val dir = Files.createTempDirectory("of-models").toFile()
        val store = ModelStore(dir, downloader = { url, dest ->
            assertThat(url).contains("ggml-tiny.en-q5_1.bin")
            dest.writeBytes(byteArrayOf(0x67, 0x67, 0x6d, 0x6c))
        }, minReadyBytes = 1L)
        val f = store.ensure(
            "tiny.en",
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en-q5_1.bin",
        )
        assertThat(f.name).isEqualTo("ggml-tiny.en-q5_1.bin")
        assertThat(f.length()).isGreaterThan(0)
        assertThat(
            store.isReady(
                "tiny.en",
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en-q5_1.bin",
            )
        ).isTrue()
    }

    @Test
    fun ensure_skips_download_when_file_present() {
        val dir = Files.createTempDirectory("of-models").toFile()
        var hits = 0
        val store = ModelStore(dir, downloader = { _, dest ->
            hits++
            dest.writeBytes(byteArrayOf(1))
        }, minReadyBytes = 1L)
        val url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en-q5_1.bin"
        store.ensure("tiny.en", url)
        store.ensure("tiny.en", url)
        assertThat(hits).isEqualTo(1)
    }
}
