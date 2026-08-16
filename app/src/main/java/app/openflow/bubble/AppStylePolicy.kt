package app.openflow.bubble

import app.openflow.prefs.FlowPrefs
import app.openflow.text.WritingStyle

/**
 * Local per-app category style backed by [AppContextEngine].
 */
object AppStylePolicy {

    fun category(packageName: String?, prefs: FlowPrefs? = null): String {
        val ctx = AppContextEngine.resolveContext(packageName, null, prefs)
        return when (ctx.category) {
            AppCategory.MESSAGING -> "personal"
            AppCategory.EMAIL -> "email"
            AppCategory.WORK_COLLAB -> "work"
            AppCategory.DOCS_NOTES -> "notes"
            AppCategory.DEV_TERMINAL -> "dev"
            AppCategory.AI_SEARCH -> "search"
            AppCategory.GENERAL -> "other"
        }
    }

    fun styleFor(packageName: String?, fallback: WritingStyle, prefs: FlowPrefs? = null): WritingStyle {
        val ctx = AppContextEngine.resolveContext(packageName, null, prefs)
        return if (ctx.category == AppCategory.GENERAL && !ctx.isCustomOverride && (prefs == null || !prefs.appContextEnabled)) {
            fallback
        } else {
            ctx.defaultStyle
        }
    }
}
