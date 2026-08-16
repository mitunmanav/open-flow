package app.openflow.ui.theme

/**
 * Product skins.
 * **BRUTAL** = cream/charcoal/ink, hard borders (only shipped look).
 * **M3** kept for shape math tests; prefs/UI always coerce to BRUTAL.
 */
enum class VisualSkin(val storage: String) {
    BRUTAL("brutal"),
    M3("m3");

    companion object {
        /** Ship look = modern brutal only. */
        val DEFAULT: VisualSkin = BRUTAL

        /** Soft/M3 storage aliases heal to BRUTAL (Soft option removed). */
        fun fromStorage(value: String): VisualSkin =
            when (value.lowercase()) {
                "brutal", "subtle_brutal", "subtle-brutal", "modern_brutal", "neo_brutal",
                "m3", "material", "material3", "soft" -> BRUTAL
                else -> DEFAULT
            }
    }
}
