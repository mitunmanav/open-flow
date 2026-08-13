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
}
