package app.openflow.stt.providers.ondevice

data class DeviceSpec(
    val ramMb: Int,
    val freeMb: Int,
    val lowRam: Boolean,
    val abi: String,
)

data class CatalogModel(
    val id: String,
    val minRamMb: Int,
    val minFreeMb: Int,
    val quality: Int,
    val url: String,
)

/** Low-ram devices never get models above 3 GB minRam. */
fun suggest(spec: DeviceSpec, catalog: List<CatalogModel>): CatalogModel? =
    catalog
        .filter { !spec.lowRam || it.minRamMb <= 3072 }
        .filter { spec.ramMb >= it.minRamMb && spec.freeMb >= it.minFreeMb }
        .maxByOrNull { it.quality }
