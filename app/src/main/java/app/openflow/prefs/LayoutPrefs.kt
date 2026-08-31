package app.openflow.prefs

/**
 * Home modules + drawer extras only (no duplicates with bottom bar).
 *
 * Bottom bar (fixed): Home · Dictionary · Snippets · Style · Insights
 * Drawer extras: History · Customize (Settings always in shell gear)
 *
 * Encoding: comma-separated ids; leading '!' means hidden.
 *
 * Home blocks match [app.openflow.ui.home.HomeModulePolicy] / HomeFeed.
 */
object LayoutPrefs {

    val HOME_MODULES = listOf(
        "banner",
        "search",
        "recent",
        "howto",
        "stats",
        "note",
        "honesty",
    )

    /** Drawer-only destinations the user may hide (Settings always shown in shell). */
    val DRAWER_EXTRAS = listOf("history", "customize")

    @Deprecated("Use DRAWER_EXTRAS — bottom tabs are not drawer items")
    val NAV_ITEMS = DRAWER_EXTRAS

    /** Declutter default: search + recent first; stats/note off; howto until dismissed. */
    const val DEFAULT_HOME = "banner,search,recent,howto,!stats,!note,honesty"
    const val DEFAULT_NAV = "history,customize"

    data class Module(val id: String, val visible: Boolean)

    private val LEGACY_HOME_ID = mapOf(
        "setup" to "howto",
        "keys" to "search",
        "test" to null,
        "stats" to "stats",
        "recent" to "recent",
    )

    /** Map legacy Home ids → current catalog. Drops unknown / obsolete. */
    fun normalizeHomeRaw(raw: String): String {
        val parts = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return DEFAULT_HOME
        val out = mutableListOf<String>()
        val seen = linkedSetOf<String>()
        for (p in parts) {
            val hidden = p.startsWith('!')
            val oldId = if (hidden) p.drop(1) else p
            val id = when {
                oldId in HOME_MODULES -> oldId
                oldId in LEGACY_HOME_ID -> LEGACY_HOME_ID[oldId]
                else -> null
            } ?: continue
            if (id in seen) continue
            seen += id
            out += if (hidden) "!$id" else id
        }
        return if (out.isEmpty()) DEFAULT_HOME else out.joinToString(",")
    }

    private fun homeDefaultVisible(id: String): Boolean {
        val token = DEFAULT_HOME.split(',').firstOrNull {
            it == id || it == "!$id"
        } ?: return true
        return !token.startsWith('!')
    }

    fun parseModules(raw: String, catalog: List<String>): List<Module> {
        val effective = if (catalog === HOME_MODULES || catalog == HOME_MODULES) {
            normalizeHomeRaw(raw)
        } else {
            raw
        }
        val parts = effective.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val seen = linkedSetOf<String>()
        val out = mutableListOf<Module>()
        for (p in parts) {
            val hidden = p.startsWith('!')
            val id = if (hidden) p.drop(1) else p
            if (id !in catalog || id in seen) continue
            // Migrate old catalog ids that moved off drawer
            if (id in listOf("dictionary", "snippets", "style", "settings")) continue
            seen += id
            out += Module(id, visible = !hidden)
        }
        for (id in catalog) {
            if (id !in seen) {
                val visible = if (catalog == HOME_MODULES) homeDefaultVisible(id) else true
                out += Module(id, visible = visible)
            }
        }
        return out
    }

    fun encodeModules(modules: List<Module>): String =
        modules.joinToString(",") { m -> if (m.visible) m.id else "!${m.id}" }

    fun toggleVisible(modules: List<Module>, id: String): List<Module> =
        modules.map { if (it.id == id) it.copy(visible = !it.visible) else it }

    fun move(modules: List<Module>, id: String, delta: Int): List<Module> {
        val i = modules.indexOfFirst { it.id == id }
        if (i < 0) return modules
        val j = (i + delta).coerceIn(0, modules.lastIndex)
        if (i == j) return modules
        val mut = modules.toMutableList()
        val item = mut.removeAt(i)
        mut.add(j, item)
        return mut
    }

    fun visibleIds(modules: List<Module>): List<String> =
        modules.filter { it.visible }.map { it.id }

    /** Drawer extras visibility. Settings + home always true. Bottom tabs not drawer. */
    fun isDrawerVisible(raw: String, id: String): Boolean {
        if (id == "home" || id == "settings") return true
        if (id in listOf("dictionary", "snippets", "style")) return false
        val mods = parseModules(raw, DRAWER_EXTRAS)
        return mods.find { it.id == id }?.visible == true
    }

    /** @deprecated use isDrawerVisible */
    fun isNavVisible(raw: String, id: String): Boolean = isDrawerVisible(raw, id)
}
