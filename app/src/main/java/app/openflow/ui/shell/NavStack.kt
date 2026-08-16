package app.openflow.ui.shell

/**
 * Pure single-activity back stack for Compose routes.
 * Bottom-bar taps reset to a single root tab (Wispr-style clear nav).
 */
object NavStack {

    fun current(stack: List<AppRoute>): AppRoute =
        stack.lastOrNull() ?: AppRoute.Home

    /** First-run: Setup until a11y+mic (+battery seen). Not a bottom tab. */
    fun initial(ready: Boolean): List<AppRoute> =
        if (ready) listOf(AppRoute.Home) else listOf(AppRoute.Setup)

    /** Navigate to [dest]. Bottom tabs replace stack; sub-screens push. */
    fun navigate(stack: List<AppRoute>, dest: AppRoute): List<AppRoute> {
        if (dest.isBottomBar()) return listOf(dest)
        if (dest == AppRoute.Settings) return listOf(AppRoute.Settings)
        val cur = current(stack)
        if (cur == dest) return stack.ifEmpty { listOf(AppRoute.Home) }
        var base = stack.ifEmpty { listOf(AppRoute.Home) }
        // Avoid duplicate consecutive destinations
        if (base.last() == dest) return base
        // Settings children sit on Settings so Back always lands there.
        if (dest.isSettingsSubtree() && AppRoute.Settings !in base) {
            base = listOf(AppRoute.Settings)
        }
        return base + dest
    }

    fun canGoBack(stack: List<AppRoute>): Boolean {
        if (stack.size > 1) return true
        val cur = stack.singleOrNull() ?: return false
        if (cur == AppRoute.Setup) return false
        if (cur == AppRoute.Home) return false
        if (cur.isSettingsSubtree()) return true
        if (cur.isBottomBar()) return true
        return true
    }

    fun goBack(stack: List<AppRoute>): List<AppRoute> {
        if (stack.size > 1) return stack.dropLast(1)
        val cur = stack.singleOrNull()
        if (cur != null && cur.isSettingsSubtree() && cur != AppRoute.Settings) {
            return listOf(AppRoute.Settings)
        }
        if (cur != null && cur != AppRoute.Home && cur != AppRoute.Setup) {
            return listOf(AppRoute.Home)
        }
        return stack.ifEmpty { listOf(AppRoute.Home) }
    }

    fun openDeepLink(dest: AppRoute): List<AppRoute> = when {
        dest.isBottomBar() || dest == AppRoute.Settings -> listOf(dest)
        dest.isSettingsSubtree() -> listOf(AppRoute.Settings, dest)
        else -> listOf(AppRoute.Home, dest)
    }

    val Saver: androidx.compose.runtime.saveable.Saver<List<AppRoute>, ArrayList<String>> =
        androidx.compose.runtime.saveable.Saver(
            save = { stack -> ArrayList(stack.map { it.name }) },
            restore = { list ->
                list.mapNotNull { name ->
                    runCatching { AppRoute.valueOf(name) }.getOrNull()
                }.ifEmpty { listOf(AppRoute.Home) }
            }
        )
}
