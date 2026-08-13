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
        val cur = current(stack)
        if (cur == dest) return stack.ifEmpty { listOf(AppRoute.Home) }
        val base = stack.ifEmpty { listOf(AppRoute.Home) }
        // Avoid duplicate consecutive destinations
        if (base.last() == dest) return base
        return base + dest
    }

    fun canGoBack(stack: List<AppRoute>): Boolean = stack.size > 1

    fun goBack(stack: List<AppRoute>): List<AppRoute> {
        if (stack.size <= 1) return stack.ifEmpty { listOf(AppRoute.Home) }
        return stack.dropLast(1)
    }

    fun openDeepLink(dest: AppRoute): List<AppRoute> =
        if (dest.isBottomBar()) listOf(dest)
        else listOf(AppRoute.Home, dest)
}
