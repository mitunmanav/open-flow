package app.openflow.prefs

/**
 * Home modules + drawer extras only (no duplicates with bottom bar).
 *
 * Bottom bar (fixed): Home · Dictionary · Snippets · Style
 * Drawer extras: Settings (always) · History · Customize
 *
 * Encoding: comma-separated ids; leading '!' means hidden.
 */
object LayoutPrefs {

    val HOME_MODULES = listOf("setup", "test", "keys", "stats", "recent")

    /** Drawer-only destinations the user may hide (Settings always shown in shell). */
    val DRAWER_EXTRAS = listOf("history", "customize")

    @Deprecated("Use DRAWER_EXTRAS — bottom tabs are not drawer items")
    val NAV_ITEMS = DRAWER_EXTRAS

    const val DEFAULT_HOME = "setup,test,keys,stats,recent"
    const val DEFAULT_NAV = "history,customize"

    data class Module(val id: String, val visible: Boolean)

    fun parseModules(raw: String, catalog: List<String>): List<Module> {
        val parts = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
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
            if (id !in seen) out += Module(id, visible = true)
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
