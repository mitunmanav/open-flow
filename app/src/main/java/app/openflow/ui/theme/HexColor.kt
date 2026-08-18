package app.openflow.ui.theme

object HexColor {
    fun parse(raw: String, fallback: Int): Int {
        val s = raw.trim().removePrefix("#")
        val hex = when (s.length) {
            6 -> "FF$s"
            8 -> s
            else -> return fallback
        }
        return hex.toLongOrNull(16)?.toInt() ?: fallback
    }

    fun format(argb: Int): String {
        val u = argb.toLong() and 0xFFFFFFFFL
        return "#%08X".format(u)
    }
}
