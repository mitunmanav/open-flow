package app.openflow.bubble

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Fresh focus lookup before insert — avoids stale nodes after window changes.
 */
object FocusResolver {

    fun resolveEditable(
        root: AccessibilityNodeInfo?,
        cached: AccessibilityNodeInfo?,
        isUsable: (AccessibilityNodeInfo) -> Boolean,
        findInSubtree: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {
        // Prefer live input focus
        if (root != null) {
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused != null) {
                if (isUsable(focused)) return focused
                val nested = findInSubtree(focused)
                @Suppress("DEPRECATION")
                focused.recycle()
                if (nested != null) return nested
            }
        }
        // Fallback: cached if still usable
        if (cached != null && isUsable(cached)) {
            @Suppress("DEPRECATION")
            return AccessibilityNodeInfo.obtain(cached)
        }
        return null
    }
}
