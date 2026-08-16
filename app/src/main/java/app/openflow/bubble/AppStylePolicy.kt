package app.openflow.bubble

import app.openflow.prefs.FlowPrefs
import app.openflow.text.StyleCategory
import app.openflow.text.StyleResolvePolicy
import app.openflow.text.WritingStyle

/**
 * Per-app style facade — Wispr 4-cat hub via [StyleResolvePolicy].
 */
object AppStylePolicy {

    fun category(packageName: String?, prefs: FlowPrefs? = null): String {
        val cat = if (prefs != null) {
            StyleResolvePolicy.category(packageName, prefs.getStyleAppAssignments())
        } else {
            StyleResolvePolicy.detect(packageName)
        }
        return when (cat) {
            StyleCategory.PERSONAL -> "personal"
            StyleCategory.WORK -> "work"
            StyleCategory.EMAIL -> "email"
            StyleCategory.OTHER -> "other"
        }
    }

    fun styleFor(packageName: String?, fallback: WritingStyle, prefs: FlowPrefs? = null): WritingStyle {
        if (prefs == null) {
            val cat = StyleResolvePolicy.detect(packageName)
            return if (cat == StyleCategory.OTHER) fallback else cat.defaultStyle
        }
        return StyleResolvePolicy.resolve(
            packageName,
            prefs.getStyleAppAssignments(),
            prefs.hubStylesMap(),
        )
    }
}
