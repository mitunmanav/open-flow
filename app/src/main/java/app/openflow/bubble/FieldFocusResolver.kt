package app.openflow.bubble

import android.view.accessibility.AccessibilityNodeInfo

/** Focused-editable node walk for the Flow Bubble. Pure lookup — no service state. */
object FieldFocusResolver {

    /** FOCUS_INPUT node if it accepts dictation, else nearest editable in its subtree. */
    fun findFocusedEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        if (isUsableEditable(focused)) return focused
        val nested = findEditableInSubtree(focused)
        @Suppress("DEPRECATION")
        focused.recycle()
        return nested
    }

    /** Depth-first first editable that [isUsableEditable]. Returns a fresh copy. */
    fun findEditableInSubtree(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (isUsableEditable(node)) {
            @Suppress("DEPRECATION")
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableInSubtree(child)
            @Suppress("DEPRECATION")
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    fun isUsableEditable(node: AccessibilityNodeInfo): Boolean =
        FieldPolicy.acceptsDictation(
            enabled = node.isEnabled,
            isEditable = node.isEditable,
            isPassword = node.isPassword,
            inputType = node.inputType,
            className = node.className?.toString(),
            hintText = node.hintText?.toString(),
            contentDescription = node.contentDescription?.toString(),
            bodyText = node.text?.toString()
        )
}
