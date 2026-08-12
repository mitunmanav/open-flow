package app.openflow.ui.theme

/**
 * Product skins.
 * **BRUTAL** = modern brutal already in repo (cream/charcoal/ink, hard borders).
 * **M3** = soft Material alternate only.
 */
enum class VisualSkin(val storage: String) {
    BRUTAL("brutal"),
    M3("m3");

    companion object {
        /** Ship look = existing modern brutal system. */
        val DEFAULT: VisualSkin = BRUTAL

        fun fromStorage(value: String): VisualSkin =
            when (value.lowercase()) {
                "m3", "material", "material3", "soft" -> M3
                "brutal", "subtle_brutal", "subtle-brutal", "modern_brutal", "neo_brutal" -> BRUTAL
                else -> DEFAULT
            }
    }
}
