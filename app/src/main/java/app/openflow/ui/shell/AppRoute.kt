package app.openflow.ui.shell

enum class AppRoute(val title: String, val navId: String? = null) {
    Home("Open Flow", "home"),
    History("History", "history"),
    Dictionary("Dictionary", "dictionary"),
    Snippets("Snippets", "snippets"),
    Style("Style", "style"),
    Settings("Settings", "settings"),
    SpeechAi("Speech + AI"),
    Appearance("Appearance"),
    BubbleSettings("Bubble"),
    HomeModules("Home layout"),
    Cleanup("Cleanup"),
    Privacy("Privacy"),
    Sounds("Sounds"),
    Setup("Set up", "setup"),
}

/** Bottom bar primary destinations. */
val BottomBarRoutes = listOf(
    AppRoute.Home,
    AppRoute.History,
    AppRoute.Dictionary,
    AppRoute.Settings,
)

/**
 * Settings hub + every settings child.
 * Bottom nav keeps Settings selected; Back returns to Settings.
 */
val SettingsSubtreeRoutes = setOf(
    AppRoute.Settings,
    AppRoute.SpeechAi,
    AppRoute.Appearance,
    AppRoute.BubbleSettings,
    AppRoute.Cleanup,
    AppRoute.Privacy,
    AppRoute.Sounds,
    AppRoute.Snippets,
    AppRoute.Style,
    AppRoute.HomeModules,
)

fun AppRoute.isBottomBar(): Boolean = this in BottomBarRoutes

fun AppRoute.isSettingsSubtree(): Boolean = this in SettingsSubtreeRoutes

/** Toolbar / system Back target. Root tabs return self (Back disabled there). */
fun AppRoute.backTarget(): AppRoute = when {
    isBottomBar() -> this
    isSettingsSubtree() -> AppRoute.Settings
    else -> AppRoute.Home
}
