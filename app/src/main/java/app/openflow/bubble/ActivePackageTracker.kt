package app.openflow.bubble

/**
 * Keep [lastPackage] fresh on every a11y event.
 * Window-only updates leave a stale bank package after
 * `am start` / task restore without WINDOW_STATE_CHANGED.
 */
object ActivePackageTracker {
    fun remember(last: String?, eventPackage: String?): String? {
        val p = eventPackage?.trim().orEmpty()
        return if (p.isNotEmpty()) p else last
    }

    fun shouldIgnoreFocus(packageName: String?): Boolean =
        PackagePolicy.shouldHideBubble(packageName)
}
