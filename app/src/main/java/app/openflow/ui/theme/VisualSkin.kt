package app.openflow.ui.theme

/**
 * Two product skins — same IA, different paint.
 * m3 = Material soft · brutal = subtle cream/charcoal hard borders.
 */
enum class VisualSkin(val storage: String) {
    M3("m3"),
    BRUTAL("brutal");

    companion object {
        fun fromStorage(value: String): VisualSkin =
            when (value.lowercase()) {
                "brutal", "subtle_brutal", "subtle-brutal" -> BRUTAL
                else -> M3
            }
    }
}
