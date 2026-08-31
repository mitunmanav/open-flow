package app.openflow.ui.home

import app.openflow.prefs.LayoutPrefs

/**
 * Home feed blocks driven by [LayoutPrefs.HOME_MODULES].
 * Banner still needs runtime need; howto needs !seenHowTo.
 */
object HomeModulePolicy {
    const val HOWTO = "howto"
    const val BANNER = "banner"
    const val STATS = "stats"
    const val NOTE = "note"
    const val SEARCH = "search"
    const val RECENT = "recent"
    const val HONESTY = "honesty"

    fun isVisible(modules: List<LayoutPrefs.Module>, id: String): Boolean =
        modules.find { it.id == id }?.visible == true

    fun orderedVisible(modules: List<LayoutPrefs.Module>): List<String> =
        LayoutPrefs.visibleIds(modules)
}
