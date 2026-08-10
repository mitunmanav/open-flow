package app.openflow.prefs

/**
 * Pure helpers for Drop 2 customisability: home modules + drawer nav visibility.
 * Encoding: comma-separated ids; leading '!' means hidden.
 * Example: "setup,stats,!test,recent"
 */
object LayoutPrefs {

    val HOME_MODULES = listOf("setup", "stats", "test", "recent")
    val NAV_ITEMS = listOf("history", "dictionary", "snippets", "style", "settings")

    const val DEFAULT_HOME = "setup,stats,test,recent"
    const val DEFAULT_NAV = "history,dictionary,snippets,style,settings"

    data class Module(val id: String, val visible: Boolean)

    fun parseModules(raw: String, catalog: List<String>): List<Module> {
        val parts = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val seen = linkedSetOf<String>()
        val out = mutableListOf<Module>()
        for (p in parts) {
            val hidden = p.startsWith('!')
            val id = if (hidden) p.drop(1) else p
            if (id !in catalog || id in seen) continue
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

    fun isNavVisible(raw: String, id: String): Boolean {
        if (id == "home") return true
        val mods = parseModules(raw, NAV_ITEMS)
        return mods.find { it.id == id }?.visible != false
    }
}
