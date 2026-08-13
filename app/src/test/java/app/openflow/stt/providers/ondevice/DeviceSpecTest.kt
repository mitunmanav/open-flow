package app.openflow.stt.providers.ondevice

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeviceSpecTest {

    private val catalog = listOf(
        CatalogModel(
            id = "tiny",
            minRamMb = 2048,
            minFreeMb = 200,
            quality = 1,
            url = "https://example.invalid/tiny",
        ),
        CatalogModel(
            id = "small",
            minRamMb = 6144,
            minFreeMb = 800,
            quality = 3,
            url = "https://example.invalid/small",
        ),
        CatalogModel(
            id = "huge",
            minRamMb = 12288,
            minFreeMb = 4000,
            quality = 9,
            url = "https://example.invalid/huge",
        ),
    )

    @Test
    fun three_gb_gets_tiny() {
        val spec = DeviceSpec(ramMb = 3072, freeMb = 10_000, lowRam = false, abi = "arm64-v8a")
        assertThat(suggest(spec, catalog)?.id).isEqualTo("tiny")
    }

    @Test
    fun eight_gb_gets_small() {
        val spec = DeviceSpec(ramMb = 8192, freeMb = 10_000, lowRam = false, abi = "arm64-v8a")
        assertThat(suggest(spec, catalog)?.id).isEqualTo("small")
    }

    @Test
    fun low_ram_never_gets_huge() {
        val spec = DeviceSpec(ramMb = 16384, freeMb = 20_000, lowRam = true, abi = "arm64-v8a")
        val pick = suggest(spec, catalog)
        assertThat(pick?.id).isNotEqualTo("huge")
        assertThat(pick?.id).isEqualTo("tiny")
    }

    @Test
    fun empty_catalog_is_null() {
        val spec = DeviceSpec(ramMb = 8192, freeMb = 10_000, lowRam = false, abi = "arm64-v8a")
        assertThat(suggest(spec, emptyList())).isNull()
    }
}
