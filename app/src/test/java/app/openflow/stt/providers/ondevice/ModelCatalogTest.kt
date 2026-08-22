package app.openflow.stt.providers.ondevice

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun parse_fixture_entries() {
        val json = """
            [
              {"id":"tiny","minRamMb":2048,"minFreeMb":200,"quality":1,"url":"https://example.invalid/tiny"},
              {"id":"small","minRamMb":6144,"minFreeMb":800,"quality":3,"url":"https://example.invalid/small"}
            ]
        """.trimIndent()
        val models = ModelCatalog.parse(json)
        assertThat(models).hasSize(2)
        assertThat(models[0].id).isEqualTo("tiny")
        assertThat(models[0].minRamMb).isEqualTo(2048)
        assertThat(models[0].minFreeMb).isEqualTo(200)
        assertThat(models[0].quality).isEqualTo(1)
        assertThat(models[0].url).isEqualTo("https://example.invalid/tiny")
        assertThat(models[1].id).isEqualTo("small")
        assertThat(models[1].quality).isEqualTo(3)
    }

    @Test
    fun parse_empty_array() {
        assertThat(ModelCatalog.parse("[]")).isEmpty()
    }

    @Test
    fun catalog_includes_tiny_en_https() {
        val json = java.io.File("src/main/assets/ondevice_catalog.json").readText()
        val models = ModelCatalog.parse(json)
        val tinyEn = models.first { it.id == "tiny.en" }
        assertThat(tinyEn.url)
            .isEqualTo("https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en-q5_1.bin")
        assertThat(tinyEn.url).contains("tiny.en-q5_1")
        assertThat(tinyEn.minRamMb).isAtMost(2048)
    }
}
