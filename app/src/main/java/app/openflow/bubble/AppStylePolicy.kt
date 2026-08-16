package app.openflow.bubble

import app.openflow.text.WritingStyle

/**
 * Local per-app category style backed by [AppContextEngine].
 */
object AppStylePolicy {

    fun category(packageName: String?): String {
        val ctx = AppContextEngine.detect(packageName, null)
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

    fun styleFor(packageName: String?, fallback: WritingStyle): WritingStyle {
        val ctx = AppContextEngine.detect(packageName, null)
        return when (ctx.category) {
            AppCategory.MESSAGING -> WritingStyle.CASUAL
            AppCategory.EMAIL, AppCategory.WORK_COLLAB -> WritingStyle.FORMAL
            AppCategory.DEV_TERMINAL -> WritingStyle.CASUAL
            else -> fallback
        }
    }
}
